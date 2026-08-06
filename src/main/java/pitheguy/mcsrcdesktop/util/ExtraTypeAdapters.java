package pitheguy.mcsrcdesktop.util;

import com.google.gson.*;

import java.lang.reflect.Type;
import java.time.Instant;

public class ExtraTypeAdapters {
    public static class InstantAdapter implements JsonSerializer<Instant>, JsonDeserializer<Instant> {
        @Override
        public Instant deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
            return Instant.parse(json.getAsString());
        }

        @Override
        public JsonElement serialize(Instant src, Type typeOfSrc, JsonSerializationContext context) {
            return new JsonPrimitive(src.toString());
        }
    }
}
