package pitheguy.mcsrcdesktop.cef.query;

import com.google.gson.JsonElement;

import java.util.function.Function;

import static pitheguy.mcsrcdesktop.util.SharedConstants.GSON;

public class CefQueryType<T extends CefQuery> {
    private final Function<JsonElement, T> deserializer;

    private CefQueryType(Function<JsonElement, T> deserializer) {
        this.deserializer = deserializer;
    }

    public static <T extends CefQuery> CefQueryType<T> of(Class<T> clazz) {
        return new CefQueryType<>(json -> GSON.fromJson(json, clazz));
    }

    public static <T extends CefQuery> CefQueryType<T> of(Function<JsonElement, T> deserializer) {
        return new CefQueryType<>(deserializer);
    }

    public T deserialize(JsonElement json) {
        return deserializer.apply(json);
    }
}
