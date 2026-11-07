package pitheguy.mcsrcdesktop.javadoc;

import com.google.gson.annotations.SerializedName;

import java.util.Map;

public record JavadocDesktopDirectorySource(
        @SerializedName("kind")
        String kind,
        @SerializedName("path")
        String path,
        @SerializedName("files")
        Map<String, String[]> files
) {
}
