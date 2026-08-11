package pitheguy.mcsrcdesktop.cef;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.cef.browser.CefBrowser;
import org.cef.browser.CefFrame;
import org.cef.callback.CefQueryCallback;
import org.cef.handler.CefMessageRouterHandlerAdapter;
import pitheguy.mcsrcdesktop.decompile.DecompileResult;
import pitheguy.mcsrcdesktop.decompile.MinecraftDecompiler;
import pitheguy.mcsrcdesktop.download.MinecraftDownloader;
import pitheguy.mcsrcdesktop.download.VersionInfo;
import pitheguy.mcsrcdesktop.download.VersionManifest;
import pitheguy.mcsrcdesktop.index.IndexEventHandler;
import pitheguy.mcsrcdesktop.util.ExtraTypeAdapters;

import java.nio.file.Files;
import java.time.Instant;
import java.util.Base64;
import java.util.concurrent.CompletableFuture;

public class EventHandler extends CefMessageRouterHandlerAdapter {
    public static final Gson GSON = new GsonBuilder()
            .registerTypeAdapter(Instant.class, new ExtraTypeAdapters.InstantAdapter())
            .create();

    private final MinecraftDownloader downloader;
    private final IndexEventHandler indexHandler;


    public EventHandler(MinecraftDownloader downloader) {
        this.downloader = downloader;
        this.indexHandler = new IndexEventHandler(this.downloader);
    }

    @Override
    public boolean onQuery(CefBrowser browser, CefFrame frame, long queryId, String request, boolean persistent, CefQueryCallback callback) {
        System.out.println("Received query: " + request);
        JsonObject requestJson = JsonParser.parseString(request).getAsJsonObject();
        switch (requestJson.get("action").getAsString()) {
            case "download" -> handleDownload(requestJson, callback);
            case "decompile" -> handleDecompile(requestJson, callback);
            case "index" -> handleIndex(requestJson, callback);
            default -> {
                return false;
            }
        }
        return true;
    }

    private void handleDownload(JsonObject request, CefQueryCallback callback) {
        try {
            String version = request.get("version").getAsString();
            VersionManifest manifest = downloader.fetchVersionManifest();
            VersionInfo versionInfo = downloader.fetchVersionInfo(manifest, version);
            ProgressUpdater progressUpdater = new ProgressUpdater(callback);
            var jarFuture = downloader.downloadJar(versionInfo, progressUpdater);
            var librariesFuture = downloader.downloadLibraries(versionInfo);
            CompletableFuture.allOf(jarFuture, librariesFuture).thenRun(() -> {
                try {
                    String result = "data:application/java-archive;base64," + Base64.getEncoder().encodeToString(Files.readAllBytes(jarFuture.get().toPath()));
                    progressUpdater.finish(result);
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            });
        } catch (Exception e) {
            e.printStackTrace();
            callback.failure(-2, "Fetch failed: " + e.getMessage());
        }
    }

    private void handleDecompile(JsonObject request, CefQueryCallback callback) {
        String className = request.get("className").getAsString();
        String version = request.get("version").getAsString();
        try {
            MinecraftDecompiler decompiler = MinecraftDecompiler.create(downloader, version);
            DecompileResult result = decompiler.decompile(className);
            callback.success(GSON.toJson(result));
        } catch (Exception e) {
            e.printStackTrace();
            callback.failure(-2, "Decompile failed: " + e.getMessage());
        }
    }

    private void handleIndex(JsonObject request, CefQueryCallback callback) {
        try {
            indexHandler.handleEvent(request, callback);
        } catch (Exception e) {
            e.printStackTrace();
            callback.failure(-2, "Index failed: " + e.getMessage());
        }
    }
}
