package pitheguy.mcsrcdesktop.cef.query;

import com.google.gson.JsonObject;
import org.cef.callback.CefQueryCallback;

import java.util.Map;

public interface CefQuery {
    Map<String, CefQueryType<?>> TYPES = Map.of(
            "version", VersionQuery.TYPE,
            "update", UpdateQuery.TYPE,
            "download", DownloadQuery.TYPE,
            "decompile", DecompileQuery.TYPE,
            "bytecode", BytecodeQuery.TYPE,
            "index", IndexQuery.TYPE,
            "remap", RemapQuery.TYPE,
            "javadoc", JavadocQuery.TYPE
    );

    CefQueryType<? extends CefQuery> type();

    void handle(CefQueryCallback callback, CefQueryContext context) throws Exception;

    static CefQuery deserialize(JsonObject json) {
        String typeId = json.get("type").getAsString();
        CefQueryType<?> type = TYPES.get(typeId);
        if (type == null) return null;
        return type.deserialize(json);
    }
}
