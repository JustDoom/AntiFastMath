package org.abyssmc.antifastmath;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class FileUtil {

    // Config file functions
    public static boolean doesFileExist(String filename) {
        Path path = Paths.get(filename);
        if (!Files.exists(path)) {
            return false;
        }
        return true;
    }
    public static void createDirectory(String path) throws IOException {
        Files.createDirectory(Paths.get(path));
    }

    public static void addConfig(String filename) throws IOException {
        Path path = Paths.get(filename);
        InputStream stream = AntiFastMath.class.getResourceAsStream("/config.yml");
        Files.copy(stream, path);
    }
}
