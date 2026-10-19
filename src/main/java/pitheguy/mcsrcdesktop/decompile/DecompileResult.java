package pitheguy.mcsrcdesktop.decompile;

import com.google.gson.annotations.SerializedName;

public record DecompileResult(
        @SerializedName("className")
        String className,
        @SerializedName("jarName")
        String jarName,
        @SerializedName("checksum")
        long checksum,
        @SerializedName("source")
        String source,
        @SerializedName("tokens")
        Token[] tokens,
        @SerializedName("language")
        Language language,
        @SerializedName("version")
        String version
) {
    public enum Language {
        @SerializedName("java")
        JAVA,
        @SerializedName("bytecode")
        BYTECODE
    }
}
