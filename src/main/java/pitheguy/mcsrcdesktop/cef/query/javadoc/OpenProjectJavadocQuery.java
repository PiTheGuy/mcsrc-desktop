package pitheguy.mcsrcdesktop.cef.query.javadoc;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.google.gson.annotations.SerializedName;
import org.cef.callback.CefQueryCallback;
import pitheguy.mcsrcdesktop.cef.query.CefQueryContext;
import pitheguy.mcsrcdesktop.download.VersionInfo;
import pitheguy.mcsrcdesktop.javadoc.AnnotaterConfig;
import pitheguy.mcsrcdesktop.javadoc.RuntimeLibrary;
import pitheguy.mcsrcdesktop.javadoc.UnpickConfig;
import pitheguy.mcsrcdesktop.javadoc.UnpickDefinition;
import pitheguy.mcsrcdesktop.util.NativeFileDialogs;

import javax.swing.*;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static pitheguy.mcsrcdesktop.util.SharedConstants.GSON;

public record OpenProjectJavadocQuery(
        @SerializedName("alternateFilePicker")
        boolean alternateFilePicker
) implements JavadocQuery {
    @Override
    public void handle(CefQueryCallback callback, CefQueryContext context) throws Exception {
        SwingUtilities.invokeLater(() -> {
            try {
                Path selectedPath = openFilePicker();
                if (selectedPath == null) {
                    callback.success("null");
                    return;
                }
                AnnotaterConfig config = parseAnnotatorConfig(selectedPath);
                List<MappingFile> fileContent = readJavadocDirectory(selectedPath.resolve(config.mappings()));
                UnpickConfig unpickConfig = config.unpickDefinitions() == null ? null : parseUnpickConfig(selectedPath.resolve(config.unpickDefinitions()));
                List<RuntimeLibrary> runtimeLibraries = extractRuntimeLibraries(context, config.version());
                callback.success(GSON.toJson(new Response(config.mappings(), unpickConfig, runtimeLibraries, config.version(), fileContent)));
            } catch (Exception e) {
                LOGGER.error("Error opening project", e);
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

    private AnnotaterConfig parseAnnotatorConfig(Path path) throws IOException {
        Path configFile = path.resolve("config.json");
        JsonElement configJson = JsonParser.parseReader(new FileReader(configFile.toFile()));
        AnnotaterConfig config = GSON.fromJson(configJson, AnnotaterConfig.class);
        if (!configJson.isJsonObject() || config.mappings() == null || config.version() == null) {
            throw new IOException("config.json must contain a Minecraft \"version\" and a relative \"mappings\" directory path.");
        }

        checkPath(configJson, "mappings", config.mappings());
        checkPath(configJson, "runtimeLibraries", config.runtimeLibraries());
        checkPath(configJson, "unpickDefinitions", config.unpickDefinitions());

        return config;
    }

    private static void checkPath(JsonElement configJson, String key, String value) throws IOException {
        if (configJson.getAsJsonObject().has(key) && value == null) {
            throw new IOException("The \"" + key + "\" path must point to a subdirectory, using forward slashes.");
        }

        if (value == null) return;

        String[] parts = value.split("/");
        for (String part : parts) {
            if (part.isEmpty() || part.equals("..") || part.equals(".") || part.contains("\\") || part.contains(":")) {
                throw new IOException("The \"" + key + "\" path must point to a subdirectory, using forward slashes.");
            }
        }
    }

    private UnpickConfig parseUnpickConfig(Path path) throws IOException {
        List<Path> unpickFiles;
        try (var stream = Files.walk(path)) {
            unpickFiles = stream
                    .filter(Files::isRegularFile)
                    .filter(p -> p.toString().endsWith(".unpick"))
                    .toList();
        }
        List<UnpickDefinition> definitions = new ArrayList<>();
        for (Path unpickFile : unpickFiles) {
            definitions.add(new UnpickDefinition(path.relativize(unpickFile).toString(), Files.readString(unpickFile)));
        }
        return new UnpickConfig(UUID.randomUUID().toString(), definitions);
    }

    private List<RuntimeLibrary> extractRuntimeLibraries(CefQueryContext context, String version) {
        try {
            VersionInfo versionInfo = context.downloader().fetchVersionInfo(version);
            List<File> libraryFiles = context.downloader().fetchLibraries(versionInfo);
            List<RuntimeLibrary> libraries = new ArrayList<>();
            for (File libraryFile : libraryFiles) {
                libraries.add(new RuntimeLibrary(libraryFile));
            }
            return libraries;
        } catch (Exception e) {
            LOGGER.warn("Failed to fetch libraries for Annotater project", e);
            return null;
        }
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
            @SerializedName("unpick")
            UnpickConfig unpick,
            @SerializedName("runtimeLibraries")
            List<RuntimeLibrary> runtimeLibraries,
            @SerializedName("version")
            String version,
            @SerializedName("files")
            List<MappingFile> files
    ) {}
}
