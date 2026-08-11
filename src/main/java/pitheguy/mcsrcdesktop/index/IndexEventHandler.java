package pitheguy.mcsrcdesktop.index;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import org.cef.callback.CefQueryCallback;
import pitheguy.mcsrcdesktop.cef.ProgressUpdater;
import pitheguy.mcsrcdesktop.download.MinecraftDownloader;
import pitheguy.mcsrcdesktop.util.ExtraTypeAdapters;

import java.io.File;
import java.io.IOException;
import java.time.Instant;

public class IndexEventHandler {
    public static final Gson GSON = new GsonBuilder()
            .registerTypeAdapter(Instant.class, new ExtraTypeAdapters.InstantAdapter())
            .create();

    private final MinecraftDownloader downloader;
    private MinecraftIndexer indexer;

    public IndexEventHandler(MinecraftDownloader downloader) {
        this.downloader = downloader;
    }

    public void handleEvent(JsonObject request, CefQueryCallback callback) throws IOException {
        String type = request.get("type").getAsString();
        switch (type) {
            case "start" -> handleStart(request, callback);
            case "getReference" -> handleGetReference(request, callback);
            case "getReferenceSize" -> handleGetReferenceSize(request, callback);
            case "getBytecode" -> handleGetBytecode(request, callback);
            case "getClassData" -> handleGetClassData(request, callback);
            case "getMemberData" -> handleGetMemberData(request, callback);
            default -> callback.failure(-1, "Unknown type: " + type);
        }
    }

    private void handleStart(JsonObject request, CefQueryCallback callback) throws IOException {
        String version = request.get("version").getAsString();
        File jar = downloader.fetchJar(version);
        indexer = new MinecraftIndexer(jar);
        ProgressUpdater progressUpdater = new ProgressUpdater(callback);
        indexer.index(progressUpdater)
                .thenRun(() -> progressUpdater.finish(null))
                .exceptionally(e -> {
                    e.printStackTrace();
                    callback.failure(-2, "Indexing failed: " + e.getMessage());
                    return null;
                });
    }

    private void handleGetReference(JsonObject request, CefQueryCallback callback) {
        String key = request.get("key").getAsString();
        String[] reference = indexer.getReference(key);
        callback.success(GSON.toJson(reference));
    }

    private void handleGetReferenceSize(JsonObject request, CefQueryCallback callback) {
        int size = indexer.getReferenceSize();
        callback.success(GSON.toJson(size));
    }

    private void handleGetBytecode(JsonObject request, CefQueryCallback callback) throws IOException {
        String className = request.get("className").getAsString();
        String bytecode = indexer.getBytecode(className);
        callback.success(GSON.toJson(bytecode));
    }

    private void handleGetClassData(JsonObject request, CefQueryCallback callback) {
        String[] classData = indexer.getClassData();
        callback.success(GSON.toJson(classData));
    }

    private void handleGetMemberData(JsonObject request, CefQueryCallback callback) {
        String[] memberData = indexer.getMemberData();
        callback.success(GSON.toJson(memberData));
    }
}
