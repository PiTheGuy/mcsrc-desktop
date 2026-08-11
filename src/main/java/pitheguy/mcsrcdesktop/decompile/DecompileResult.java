package pitheguy.mcsrcdesktop.decompile;

import com.google.gson.annotations.SerializedName;

public record DecompileResult(
        @SerializedName("className")
        String className,
        @SerializedName("checksum")
        long checksum,
        @SerializedName("source")
        String source,
        @SerializedName("tokens")
        Token[] tokens,
        @SerializedName("language")
        Language language
) {
    public enum Language {
        @SerializedName("java")
        JAVA,
        @SerializedName("bytecode")
        BYTECODE
    }
}
