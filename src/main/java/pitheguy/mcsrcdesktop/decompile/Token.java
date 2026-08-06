package pitheguy.mcsrcdesktop.decompile;

import com.google.gson.annotations.SerializedName;

public sealed interface Token permits Token.MemberToken, Token.NonMethodToken {
    int start();
    int length();
    String className();
    boolean declaration();

    enum TokenType {
        @SerializedName("class")
        CLASS,
        @SerializedName("field")
        FIELD,
        @SerializedName("method")
        METHOD,
        @SerializedName("parameter")
        PARAMETER,
        @SerializedName("local")
        LOCAL
    }

    record MemberToken(
            @SerializedName("start")
            int start,
            @SerializedName("length")
            int length,
            @SerializedName("className")
            String className,
            @SerializedName("declaration")
            boolean declaration,
            @SerializedName("type")
            TokenType type,
            @SerializedName("name")
            String name,
            @SerializedName("descriptor")
            String descriptor
    ) implements Token {}

    record NonMethodToken(
            @SerializedName("start")
            int start,
            @SerializedName("length")
            int length,
            @SerializedName("className")
            String className,
            @SerializedName("declaration")
            boolean declaration,
            @SerializedName("type")
            TokenType type
    ) implements Token {}
}