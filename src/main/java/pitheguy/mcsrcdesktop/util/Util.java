package pitheguy.mcsrcdesktop.util;

import java.nio.file.Path;

public class Util {
    public static Path getAppDataDir() {
        String userHome = System.getProperty("user.home");
        Path base;

        switch (OS.get()) {
            case WINDOWS -> {
                String appData = System.getenv("APPDATA");
                base = (appData != null) ? Path.of(appData) : Path.of(userHome, "AppData/Roaming");
            }
            case OSX -> base = Path.of(userHome, "Library/Application Support");
            case LINUX -> {
                // Linux/Unix - XDG Base Directory spec
                String xdgData = System.getenv("XDG_DATA_HOME");
                base = (xdgData != null && !xdgData.isEmpty())
                        ? Path.of(xdgData)
                        : Path.of(userHome, ".local/share");
            }
            default -> base = Path.of(userHome, ".mcsrc");
        }

        return base.resolve("mcsrc Desktop");
    }

}
