package pitheguy.mcsrcdesktop.cef;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import org.cef.callback.CefQueryCallback;
import pitheguy.mcsrcdesktop.util.ExtraTypeAdapters;
import pitheguy.mcsrcdesktop.util.ProgressListener;

import java.time.Instant;

public class ProgressUpdater implements ProgressListener {
    public static final Gson GSON = new GsonBuilder()
            .registerTypeAdapter(Instant.class, new ExtraTypeAdapters.InstantAdapter())
            .create();

    private final CefQueryCallback callback;

    public ProgressUpdater(CefQueryCallback callback) {
        this.callback = callback;
    }

    @Override
    public void update(double progress) {
        JsonObject json = new JsonObject();
        json.addProperty("type", "progress");
        json.addProperty("progress", progress);
        callback.success(GSON.toJson(json));
    }

    public <R> void finish(R result) {
        JsonObject json = new JsonObject();
        json.addProperty("type", "done");
        json.add("result", GSON.toJsonTree(result));
        callback.success(GSON.toJson(json));
    }
}
