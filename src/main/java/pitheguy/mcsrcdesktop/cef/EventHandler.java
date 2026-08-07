package pitheguy.mcsrcdesktop.cef;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.cef.browser.CefBrowser;
import org.cef.browser.CefFrame;
import org.cef.callback.CefQueryCallback;
import org.cef.handler.CefMessageRouterHandlerAdapter;
import pitheguy.mcsrcdesktop.decompile.MinecraftDecompiler;
import pitheguy.mcsrcdesktop.download.MinecraftDownloader;
import pitheguy.mcsrcdesktop.download.VersionInfo;
import pitheguy.mcsrcdesktop.download.VersionManifest;
import pitheguy.mcsrcdesktop.util.ExtraTypeAdapters;

import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.time.Instant;
import java.util.Base64;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public class EventHandler extends CefMessageRouterHandlerAdapter {
    public static final Gson GSON = new GsonBuilder()
            .registerTypeAdapter(Instant.class, new ExtraTypeAdapters.InstantAdapter())
            .create();

    private final MinecraftDownloader downloader;
    private File mainJar;
    private List<File> libraries;

    public EventHandler(MinecraftDownloader downloader) {
        this.downloader = downloader;
    }

    @Override
    public boolean onQuery(CefBrowser browser, CefFrame frame, long queryId, String request, boolean persistent, CefQueryCallback callback) {
        System.out.println("Received query: " + request);
        JsonObject requestJson = JsonParser.parseString(request).getAsJsonObject();
        switch (requestJson.get("action").getAsString()) {
            case "fetch" -> handleFetch(requestJson, callback);
            case "decompile" -> handleDecompile(requestJson, callback);
            default -> {
                return false;
            }
        }
        return true;
    }

    private void handleFetch(JsonObject request, CefQueryCallback callback) {
        try {
            System.out.println("Starting download");
            String version = request.get("version").getAsString();
            VersionManifest manifest = downloader.fetchVersionManifest();
            VersionInfo versionInfo = downloader.fetchVersionInfo(manifest, version);
            var jarFuture = downloader.fetchJar(versionInfo, progress -> {
                JsonObject json = new JsonObject();
                json.addProperty("type", "progress");
                json.addProperty("progress", progress);
                callback.success(GSON.toJson(json));
            });
            var librariesFuture = downloader.fetchLibraries(versionInfo);
            jarFuture.thenAccept(jar -> this.mainJar = jar);
            librariesFuture.thenAccept(libs -> this.libraries = libs);
            CompletableFuture.allOf(jarFuture, librariesFuture).thenRun(() -> {
                try {
                    JsonObject json = new JsonObject();
                    json.addProperty("type", "done");
                    json.addProperty("path", "data:application/java-archive;base64," + Base64.getEncoder().encodeToString(Files.readAllBytes(mainJar.toPath())));
                    callback.success(GSON.toJson(json));
                    System.out.println("Finished downloading");
                } catch (IOException e) {
                    throw new UncheckedIOException(e);
                }
            });
        } catch (Exception e) {
            e.printStackTrace();
            callback.failure(-2, "Fetch failed: " + e.getMessage());
        }
    }

    private void handleDecompile(JsonObject request, CefQueryCallback callback) {
        if (mainJar == null) throw new IllegalStateException("No version selected!");
        String className = request.get("className").getAsString();
        CompletableFuture.supplyAsync(() -> MinecraftDecompiler.decompile(mainJar, libraries, className))
                .thenAccept(result -> callback.success(GSON.toJson(result)))
                .exceptionally(e -> {
                    e.printStackTrace();
                    callback.failure(-2, "Decompile failed: " + e.getMessage());
                    return null;
                });
    }
}
