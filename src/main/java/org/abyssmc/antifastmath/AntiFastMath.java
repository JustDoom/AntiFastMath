package org.abyssmc.antifastmath;

import net.minestom.server.MinecraftServer;
import net.minestom.server.coordinate.Vec;
import net.minestom.server.entity.Player;
import net.minestom.server.event.GlobalEventHandler;
import net.minestom.server.event.player.PlayerDisconnectEvent;
import net.minestom.server.event.player.PlayerLoginEvent;
import net.minestom.server.event.player.PlayerMoveEvent;
import net.minestom.server.extensions.Extension;
import org.spongepowered.configurate.CommentedConfigurationNode;
import org.spongepowered.configurate.yaml.YamlConfigurationLoader;

import java.io.IOException;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Vector;

public final class AntiFastMath extends Extension {
    HashMap<Player, Double> vanillaPrecision = new HashMap<>();
    HashMap<Player, Double> fastMathPrecision = new HashMap<>();
    HashMap<Player, Integer> playerSamples = new HashMap<>();

    int multiplier;
    String kickMessage;

    public static Vec getVanillaMathMovement(Vec wantedMovement, float f, float f2) {
        float f3 = VanillaMath.sin(f2 * 0.017453292f);
        float f4 = VanillaMath.cos(f2 * 0.017453292f);

        float bestTheoreticalX = (float) (f3 * wantedMovement.z() + f4 * wantedMovement.x()) / (f3 * f3 + f4 * f4) / f;
        float bestTheoreticalZ = (float) (-f3 * wantedMovement.x() + f4 * wantedMovement.z()) / (f3 * f3 + f4 * f4) / f;

        return new Vec(bestTheoreticalX, 0, bestTheoreticalZ);
    }

    public static Vec getFastMathMovement(Vec wantedMovement, float f, float f2) {
        float f3 = OptifineMath.sin(f2 * 0.017453292f);
        float f4 = OptifineMath.cos(f2 * 0.017453292f);

        float bestTheoreticalX = (float) (f3 * wantedMovement.z() + f4 * wantedMovement.x()) / (f3 * f3 + f4 * f4) / f;
        float bestTheoreticalZ = (float) (-f3 * wantedMovement.x() + f4 * wantedMovement.z()) / (f3 * f3 + f4 * f4) / f;

        return new Vec(bestTheoreticalX, 0, bestTheoreticalZ);
    }

    @Override
    public void initialize() {
        // Plugin startup logic

        CommentedConfigurationNode configFile;

        // Create the config file
        try {
            if(!FileUtil.doesFileExist(getDataDirectory().toString()))
                FileUtil.createDirectory(getDataDirectory().toString());

            if(!FileUtil.doesFileExist(getDataDirectory() + "/config.yml"))
                FileUtil.addConfig(getDataDirectory() + "/config.yml");
        } catch (IOException e) {
            e.printStackTrace();
        }

        // Set where we want to load and save the config from
        final YamlConfigurationLoader loader = YamlConfigurationLoader.builder()
                .path(Paths.get(getDataDirectory() + "/config.yml"))
                .build();

        // Load the config
        try {
            configFile = loader.load();
            getLogger().info("Config has been loaded");
        } catch (IOException e) {
            System.err.println("An error occurred while loading this configuration: " + e.getMessage());
            if (e.getCause() != null) {
                e.getCause().printStackTrace();
            }
            System.exit(1);
            return;
        }

        multiplier = configFile.node("multiplierRequired").getInt();
        kickMessage = configFile.node("kickMessage").getString();

        for (Player player : MinecraftServer.getConnectionManager().getOnlinePlayers()) {
            vanillaPrecision.put(player, 0D);
            fastMathPrecision.put(player, 0D);
            playerSamples.put(player, 0);
        }

        GlobalEventHandler globalEventHandler = MinecraftServer.getGlobalEventHandler();
        globalEventHandler.addListener(PlayerLoginEvent.class, event -> {
            vanillaPrecision.put(event.getPlayer(), 0D);
            fastMathPrecision.put(event.getPlayer(), 0D);
            playerSamples.put(event.getPlayer(), 0);
        });

        globalEventHandler.addListener(PlayerDisconnectEvent.class, event -> {
            vanillaPrecision.remove(event.getPlayer());
            fastMathPrecision.remove(event.getPlayer());
            playerSamples.remove(event.getPlayer());
        });

        globalEventHandler.addListener(PlayerMoveEvent.class, event -> {
            Vec movement = event.getNewPosition().asVec().sub(event.getPlayer().getPosition().asVec());
            Vec vanillaMovement = getVanillaMathMovement(movement, (float) (0.1), event.getPlayer().getPosition().yaw());
            Vec fastMathMovement = getFastMathMovement(movement, (float) (0.1), event.getPlayer().getPosition().yaw());

            double lowVanilla = Math.min(Math.abs(vanillaMovement.x()), Math.abs(vanillaMovement.z()));
            double lowOptifine = Math.min(Math.abs(fastMathMovement.x()), Math.abs(fastMathMovement.z()));

            double vanillaRunning = vanillaPrecision.get(event.getPlayer());
            double optifineRunning = fastMathPrecision.get(event.getPlayer());

            double xDistance = event.getPlayer().getPosition().x() - event.getNewPosition().x();
            double zDistance = event.getPlayer().getPosition().z() - event.getNewPosition().z();

            if ((lowVanilla < 1e-5 || lowOptifine < 1e-5) && ((xDistance * xDistance) + (zDistance * zDistance) > 0.01)) {
                vanillaRunning = vanillaRunning * 15 / 16 + lowVanilla;
                optifineRunning = optifineRunning * 15 / 16 + lowOptifine;

                vanillaPrecision.put(event.getPlayer(), vanillaRunning);
                fastMathPrecision.put(event.getPlayer(), optifineRunning);

                int count = playerSamples.get(event.getPlayer());
                playerSamples.put(event.getPlayer(), count + 1);

                if (count > 16 && optifineRunning * multiplier < vanillaRunning) {
                    event.getPlayer().kick(kickMessage);
                }
            }
        });
    }

    @Override
    public void terminate() {

    }
}
