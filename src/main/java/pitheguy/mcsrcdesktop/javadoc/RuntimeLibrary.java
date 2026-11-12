package pitheguy.mcsrcdesktop.javadoc;

import com.google.gson.annotations.SerializedName;
import pitheguy.mcsrcdesktop.util.FileUtil;

import java.io.File;
import java.io.IOException;

public record RuntimeLibrary(
        @SerializedName("name")
        String name,
        @SerializedName("url")
        String url
) {
    public RuntimeLibrary(File file) throws IOException {
        this(file.getName(), FileUtil.toDataUrl(file, "application/java-archive"));
    }
}
