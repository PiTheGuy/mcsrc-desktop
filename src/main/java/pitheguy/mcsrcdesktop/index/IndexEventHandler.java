package pitheguy.mcsrcdesktop.index;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import mcsrc.ClassData;
import mcsrc.MemberData;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.cef.callback.CefQueryCallback;
import pitheguy.mcsrcdesktop.cef.ProgressUpdater;
import pitheguy.mcsrcdesktop.download.MinecraftDownloader;
import pitheguy.mcsrcdesktop.download.VersionInfo;
import pitheguy.mcsrcdesktop.util.ExtraTypeAdapters;

import java.io.File;
import java.io.IOException;
import java.util.Set;

public class IndexEventHandler {
    private static final Logger LOGGER = LogManager.getLogger();
    public static final Gson GSON = new GsonBuilder()
            .registerTypeAdapter(ClassData.class, ExtraTypeAdapters.CLASS_DATA)
            .registerTypeAdapter(MemberData.class, ExtraTypeAdapters.MEMBER_DATA)
            .create();

    private final MinecraftDownloader downloader;
    private MinecraftIndexer indexer;

    public IndexEventHandler(MinecraftDownloader downloader) {
        this.downloader = downloader;
    }

    public void handleEvent(JsonObject request, CefQueryCallback callback) throws IOException, InterruptedException {
        String type = request.get("type").getAsString();
        switch (type) {
            case "start" -> handleStart(request, callback);
            case "getReference" -> handleGetReference(request, callback);
            case "getReferenceSize" -> handleGetReferenceSize(request, callback);
            case "getClassData" -> handleGetClassData(request, callback);
            case "getMemberData" -> handleGetMemberData(request, callback);
            default -> callback.failure(-1, "Unknown type: " + type);
        }
    }

    private void handleStart(JsonObject request, CefQueryCallback callback) throws IOException, InterruptedException {
        String version = request.get("version").getAsString();
        VersionInfo versionInfo = downloader.fetchVersionInfo(version);
        File jar = downloader.fetchRemappedJar(versionInfo);
        indexer = new MinecraftIndexer(jar);
        ProgressUpdater progressUpdater = new ProgressUpdater(callback);
        indexer.index(progressUpdater)
                .thenRun(() -> progressUpdater.finish(null))
                .exceptionally(e -> {
                    LOGGER.error("Failed to index version {}", version, e);
                    callback.failure(-2, "Indexing failed: " + e.getMessage());
                    return null;
                });
    }

    private void handleGetReference(JsonObject request, CefQueryCallback callback) {
        String key = request.get("key").getAsString();
        Set<String> references = indexer.references(key);
        JsonArray output = new JsonArray();
        references.forEach(output::add);
        callback.success(GSON.toJson(output));
    }

    private void handleGetReferenceSize(JsonObject request, CefQueryCallback callback) {
        int size = indexer.referenceCount();
        callback.success(GSON.toJson(size));
    }

    private void handleGetClassData(JsonObject request, CefQueryCallback callback) {
        ClassData[] classData = indexer.data().classes().values().toArray(ClassData[]::new);
        callback.success(GSON.toJson(classData));
    }

    private void handleGetMemberData(JsonObject request, CefQueryCallback callback) {
        MemberData[] memberData = indexer.data().members().values().toArray(MemberData[]::new);
        callback.success(GSON.toJson(memberData));
    }
}
