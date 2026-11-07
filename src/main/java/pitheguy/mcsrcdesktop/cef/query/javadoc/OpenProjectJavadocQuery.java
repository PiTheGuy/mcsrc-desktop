package pitheguy.mcsrcdesktop.cef.query.javadoc;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.annotations.SerializedName;
import org.cef.callback.CefQueryCallback;
import pitheguy.mcsrcdesktop.cef.query.CefQueryContext;
import pitheguy.mcsrcdesktop.javadoc.AnnotatorConfig;
import pitheguy.mcsrcdesktop.util.NativeFileDialogs;

import javax.swing.*;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static pitheguy.mcsrcdesktop.util.SharedConstants.GSON;

public record OpenProjectJavadocQuery(
        @SerializedName("alternateFilePicker")
        boolean alternateFilePicker
) implements JavadocQuery {

    public static final String MISSING_PROPERTIES_ERROR = "config.json must contain a Minecraft \"version\" and a relative \"mappings\" directory path.";

    @Override
    public void handle(CefQueryCallback callback, CefQueryContext context) throws Exception {
        SwingUtilities.invokeLater(() -> {
            try {
                Path selectedPath = openFilePicker();
                if (selectedPath == null) {
                    callback.success("null");
                    return;
                }
                AnnotatorConfig config = parseAnnotatorConfig(selectedPath);
                List<MappingFile> fileContent = readJavadocDirectory(config.mappings());
                callback.success(GSON.toJson(new Response(config.mappings().toString(), config.version(), fileContent)));
            } catch (Exception e) {
                JavadocQuery.LOGGER.error("Error opening project", e);
                callback.failure(-2, "Javadoc query failed \n" + e);
            }
        });
    }

    @Override
    public Object handleAction(CefQueryCallback callback, CefQueryContext context) {
        throw new UnsupportedOperationException("Call handle directly");
    }

    private Path openFilePicker() {
        if (alternateFilePicker) {
            JFileChooser chooser = new JFileChooser();
            chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
            int result = chooser.showOpenDialog(null);
            if (result == JFileChooser.APPROVE_OPTION) {
                return chooser.getSelectedFile().toPath();
            }
            return null;
        } else {
            return NativeFileDialogs.pickFolder(null).orElse(null);
        }
    }

    private AnnotatorConfig parseAnnotatorConfig(Path path) throws IOException {
        Path configFile = path.resolve("config.json");
        JsonObject configJson = JsonParser.parseReader(new FileReader(configFile.toFile())).getAsJsonObject();
        if (!configJson.has("mappings") || !configJson.has("version")) {
            throw new IOException(MISSING_PROPERTIES_ERROR);
        }
        JsonElement mappingsEl = configJson.get("mappings");
        JsonElement versionEl = configJson.get("version");
        if (!mappingsEl.isJsonPrimitive() || !mappingsEl.getAsJsonPrimitive().isString() ||
        !versionEl.isJsonPrimitive() || !versionEl.getAsJsonPrimitive().isString()) {
            throw new IOException(MISSING_PROPERTIES_ERROR);
        }
        String mappings = mappingsEl.getAsString();
        String version = versionEl.getAsString();

        String[] mappingsParts = mappings.split("/");
        for (String part : mappingsParts) {
            if (part.isEmpty() || part.equals("..") || part.equals(".") || part.contains("\\") || part.contains("/")) {
                throw new IOException("The \"mappings\" path must point to a subdirectory, using forward slashes.");
            }
        }

        return new AnnotatorConfig(path.resolve(Path.of(mappings)), version);
    }

    private List<MappingFile> readJavadocDirectory(Path path) throws IOException {
        List<Path> mappingsFiles;
        try (var stream = Files.walk(path)) {
            mappingsFiles = stream
                    .filter(Files::isRegularFile)
                    .filter(p -> p.toString().endsWith(".mapping"))
                    .toList();
        }
        List<MappingFile> fileContent = new ArrayList<>();
        for (Path mappingsFile : mappingsFiles) {
            Path relativePath = path.relativize(mappingsFile);
            byte[] bytes = Files.readAllBytes(mappingsFile);
            fileContent.add(new MappingFile(relativePath.toString(), bytes));
        }
        return fileContent;
    }

    record MappingFile(
            @SerializedName("path")
            String path,
            @SerializedName("content")
            byte[] content
    ) {}

    record Response(
            @SerializedName("path")
            String path,
            @SerializedName("version")
            String version,
            @SerializedName("files")
            List<MappingFile> files
    ) {}
}
