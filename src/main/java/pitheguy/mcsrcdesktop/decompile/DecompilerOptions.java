package pitheguy.mcsrcdesktop.decompile;

import com.google.gson.annotations.SerializedName;

public record DecompilerOptions(
        @SerializedName("displayLambdas")
        boolean displayLambdas
) {
    public Object[] toVineflowerOptions() {
        return displayLambdas ? new Object[]{"mark-corresponding-synthetics", "1"} : new Object[0];
    }
}
