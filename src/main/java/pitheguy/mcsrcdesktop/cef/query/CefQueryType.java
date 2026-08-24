package pitheguy.mcsrcdesktop.cef.query;

import com.google.gson.JsonObject;

import java.util.function.Function;

import static pitheguy.mcsrcdesktop.util.SharedConstants.GSON;

public class CefQueryType<T extends CefQuery> {
    private final Function<JsonObject, T> deserializer;

    private CefQueryType(Function<JsonObject, T> deserializer) {
        this.deserializer = deserializer;
    }

    public static <T extends CefQuery> CefQueryType<T> of(Class<T> clazz) {
        return new CefQueryType<>(json -> GSON.fromJson(json, clazz));
    }

    public static <T extends CefQuery> CefQueryType<T> of(Function<JsonObject, T> deserializer) {
        return new CefQueryType<>(deserializer);
    }

    public T deserialize(JsonObject json) {
        return deserializer.apply(json);
    }
}
