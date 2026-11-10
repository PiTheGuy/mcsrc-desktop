package pitheguy.mcsrcdesktop.javadoc;

import com.google.gson.annotations.SerializedName;

import java.util.List;

public record UnpickConfig(
        @SerializedName("id")
        String id,
        @SerializedName("definitions")
        List<UnpickDefinition> definitions
) {
}
