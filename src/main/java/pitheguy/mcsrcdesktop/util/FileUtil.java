package pitheguy.mcsrcdesktop.util;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Base64;

public class FileUtil {
    public static String toDataUrl(File file, String mimeType) throws IOException {
        byte[] bytes = Files.readAllBytes(file.toPath());
        return "data:" + mimeType + ";base64," + Base64.getEncoder().encodeToString(bytes);
    }

    public static String stripExtension(String fileName) {
        if (fileName.lastIndexOf('.') < 0) return fileName;
        return fileName.substring(0, fileName.lastIndexOf('.'));
    }
}
