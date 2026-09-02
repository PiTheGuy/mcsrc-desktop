package pitheguy.mcsrcdesktop.cef;

import com.google.gson.JsonObject;
import org.cef.callback.CefQueryCallback;
import pitheguy.mcsrcdesktop.util.ProgressListener;

import static pitheguy.mcsrcdesktop.util.SharedConstants.GSON;

public class ProgressUpdater implements ProgressListener {
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
