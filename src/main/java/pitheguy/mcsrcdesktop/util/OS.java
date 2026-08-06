package pitheguy.mcsrcdesktop.util;

public enum OS {
    WINDOWS, OSX, LINUX, OTHER;

    public static OS get() {
        String os = System.getProperty("os.name").toLowerCase();
        if (os.contains("win")) {
            return WINDOWS;
        } else if (os.contains("mac")) {
            return OSX;
        } else if (os.contains("nix") || os.contains("nux") || os.contains("aix")) {
            return LINUX;
        } else {
            return OTHER;
        }
    }
}
