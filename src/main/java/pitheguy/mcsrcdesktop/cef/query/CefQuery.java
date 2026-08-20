package pitheguy.mcsrcdesktop.cef.query;

import com.google.gson.JsonObject;
import org.cef.callback.CefQueryCallback;

import java.util.Map;

public interface CefQuery {
    Map<String, CefQueryType<?>> TYPES = Map.of();

    CefQueryType<? extends CefQuery> type();

    void handle(CefQueryCallback callback);

    static CefQuery deserialize(JsonObject json) {
        String typeId = json.get("action").getAsString(); // TODO change to type for protocol version 2
        CefQueryType<?> type = TYPES.get(typeId);
        if (type == null) return null;
        return type.deserialize(json);
    }
}
