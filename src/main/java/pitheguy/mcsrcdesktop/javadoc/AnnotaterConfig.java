package pitheguy.mcsrcdesktop.javadoc;

import com.google.gson.annotations.SerializedName;

public record AnnotaterConfig(
        @SerializedName("mappings")
        String mappings,
        @SerializedName("runtimeLibraries")
        String runtimeLibraries,
        @SerializedName("unpickDefinitions")
        String unpickDefinitions,
        @SerializedName("version")
        String version) {
}
