package pitheguy.mcsrcdesktop.javadoc;

import com.google.gson.annotations.SerializedName;

import java.nio.file.Path;

public record AnnotatorConfig(Path mappings, String version) {
}
