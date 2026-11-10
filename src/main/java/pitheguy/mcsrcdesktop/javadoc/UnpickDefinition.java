package pitheguy.mcsrcdesktop.javadoc;

import com.google.gson.annotations.SerializedName;

public record UnpickDefinition(
        @SerializedName("path")
        String path,
        @SerializedName("content")
        String content) {
}
