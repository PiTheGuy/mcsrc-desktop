package pitheguy.mcsrcdesktop.cef;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.cef.browser.CefBrowser;
import org.cef.browser.CefFrame;
import org.cef.callback.CefQueryCallback;
import org.cef.handler.CefMessageRouterHandlerAdapter;
import pitheguy.mcsrcdesktop.decompile.DecompileResult;
import pitheguy.mcsrcdesktop.decompile.DecompilerOptions;
import pitheguy.mcsrcdesktop.decompile.MinecraftDecompiler;
import pitheguy.mcsrcdesktop.download.MinecraftDownloader;
import pitheguy.mcsrcdesktop.download.VersionInfo;
import pitheguy.mcsrcdesktop.index.IndexEventHandler;
import pitheguy.mcsrcdesktop.remap.MinecraftRemapper;
import pitheguy.mcsrcdesktop.util.ExtraTypeAdapters;
import pitheguy.mcsrcdesktop.util.SharedConstants;

import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.URI;
import java.nio.file.Files;
import java.time.Instant;
import java.util.Base64;

public class EventHandler extends CefMessageRouterHandlerAdapter {
    private static final Logger LOGGER = LogManager.getLogger(EventHandler.class);
    public static final Gson GSON = new GsonBuilder()
            .registerTypeAdapter(Instant.class, ExtraTypeAdapters.INSTANT)
            .create();

    private final MinecraftDownloader downloader;
    private final IndexEventHandler indexHandler;


    public EventHandler(MinecraftDownloader downloader) {
        this.downloader = downloader;
        this.indexHandler = new IndexEventHandler(this.downloader);
    }

    @Override
    public boolean onQuery(CefBrowser browser, CefFrame frame, long queryId, String request, boolean persistent, CefQueryCallback callback) {
        LOGGER.debug("Received query: {}", request);
        JsonObject requestJson = JsonParser.parseString(request).getAsJsonObject();
        switch (requestJson.get("action").getAsString()) {
            case "version" -> handleVersion(requestJson, callback);
            case "update" -> handleUpdate(requestJson, callback);
            case "download" -> handleDownload(requestJson, callback);
            case "decompile" -> handleDecompile(requestJson, callback);
            case "bytecode" -> handleBytecode(requestJson, callback);
            case "index" -> handleIndex(requestJson, callback);
            case "remap" -> handleRemap(requestJson, callback);
            default -> {
                return false;
            }
        }
        return true;
    }

    private void handleVersion(JsonObject request, CefQueryCallback callback) {
        JsonObject response = new JsonObject();
        response.addProperty("protocol", SharedConstants.PROTOCOL_VERSION);
        response.addProperty("app", SharedConstants.APP_VERSION);
        callback.success(GSON.toJson(response));
    }

    private void handleUpdate(JsonObject request, CefQueryCallback callback) {
        try {
            //TODO update automatically?
            Desktop.getDesktop().browse(URI.create("https://github.com/PiTheGuy/mcsrc-desktop/releases/latest"));
            callback.success("{}");
        } catch (Exception e) {
            callback.failure(-2, "Update failed: " + e.getMessage());
        }
    }

    private void handleDownload(JsonObject request, CefQueryCallback callback) {
        try {
            String version = request.get("version").getAsString();
            VersionInfo versionInfo = downloader.fetchVersionInfo(version);
            ProgressUpdater progressUpdater = new ProgressUpdater(callback);
            var future = downloader.downloadVersion(versionInfo, progressUpdater);
            future.thenAccept(downloadInfo -> {
                try {
                    JsonObject response = new JsonObject();
                    String jarUrl = "data:application/java-archive;base64," + Base64.getEncoder().encodeToString(Files.readAllBytes(downloadInfo.jar().toPath()));
                    response.addProperty("jar", jarUrl);
                    if (downloadInfo.mappings() != null) {
                        String mappingsUrl = "data:text/plain;base64," + Base64.getEncoder().encodeToString(Files.readAllBytes(downloadInfo.mappings().toPath()));
                        response.addProperty("mappings", mappingsUrl);
                    }
                    progressUpdater.finish(response);
                } catch (IOException e) {
                    throw new UncheckedIOException(e);
                }
            });
        } catch (Exception e) {
            LOGGER.error("Failed to download version {}", request.get("version").getAsString(), e);
            callback.failure(-2, "Fetch failed: " + e.getMessage());
        }
    }

    private void handleDecompile(JsonObject request, CefQueryCallback callback) {
        String className = request.get("className").getAsString();
        String version = request.get("version").getAsString();
        DecompilerOptions options = GSON.fromJson(request.get("options"), DecompilerOptions.class);
        try {
            MinecraftDecompiler decompiler = MinecraftDecompiler.create(downloader, version);
            DecompileResult result = decompiler.decompile(className, options);
            callback.success(GSON.toJson(result));
        } catch (Exception e) {
            LOGGER.error("Failed to decompile {}", className, e);
            callback.failure(-2, "Decompile failed: " + e.getMessage());
        }
    }

    private void handleBytecode(JsonObject request, CefQueryCallback callback) {
        String className = request.get("className").getAsString();
        String version = request.get("version").getAsString();
        try {
            MinecraftDecompiler decompiler = MinecraftDecompiler.create(downloader, version);
            DecompileResult result = decompiler.getBytecode(className);
            callback.success(GSON.toJson(result));
        } catch (Exception e) {
            LOGGER.error("Failed to get bytecode for {}", className, e);
            callback.failure(-2, "Bytecode retrieval failed: " + e.getMessage());
        }
    }

    private void handleIndex(JsonObject request, CefQueryCallback callback) {
        try {
            indexHandler.handleEvent(request, callback);
        } catch (Exception e) {
            LOGGER.error("Indexing failed", e);
            callback.failure(-2, "Index failed: " + e.getMessage());
        }
    }

    private void handleRemap(JsonObject request, CefQueryCallback callback) {
        String version = request.get("version").getAsString();
        ProgressUpdater progressUpdater = new ProgressUpdater(callback);
        try {
            VersionInfo versionInfo = downloader.fetchVersionInfo(version);
            File jar = downloader.fetchJar(versionInfo);
            File mappings = downloader.fetchMappings(versionInfo);
            MinecraftRemapper remapper = new MinecraftRemapper(jar, mappings);
            File remappedJar = remapper.remap(progressUpdater);
            String url = "data:application/java-archive;base64," + Base64.getEncoder().encodeToString(Files.readAllBytes(remappedJar.toPath()));
            progressUpdater.finish(url);
        } catch (Exception e) {
            LOGGER.error("Failed to remap version {}", version, e);
            callback.failure(-2, "Remap failed: " + e.getMessage());
        }
    }
}
