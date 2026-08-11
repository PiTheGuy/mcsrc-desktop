package pitheguy.mcsrcdesktop.util;

import com.google.gson.*;
import mcsrc.ClassData;
import mcsrc.Entry;
import mcsrc.MemberData;

import java.lang.reflect.Type;
import java.time.Instant;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class ExtraTypeAdapters {
    public static Object INSTANT = new InstantAdapter();
    public static Object CLASS_DATA = new ClassDataAdapter();
    public static Object MEMBER_DATA = new MemberDataAdapter();

    private static class InstantAdapter implements JsonSerializer<Instant>, JsonDeserializer<Instant> {
        @Override
        public Instant deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
            return Instant.parse(json.getAsString());
        }

        @Override
        public JsonElement serialize(Instant src, Type typeOfSrc, JsonSerializationContext context) {
            return new JsonPrimitive(src.toString());
        }
    }

    private static class ClassDataAdapter implements JsonSerializer<ClassData>, JsonDeserializer<ClassData> {
        @Override
        public ClassData deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
            String[] parts = json.getAsString().split("\\|", -1);
            return new ClassData(parts[0],
                    parts[1].isEmpty() ? null : parts[1],
                    parts[3].isEmpty() ? List.of() : Arrays.asList(parts[3].split(",")),
                    Integer.parseInt(parts[2]));
        }

        @Override
        public JsonElement serialize(ClassData src, Type typeOfSrc, JsonSerializationContext context) {
            String str = "%s|%s|%d|%s".formatted(
                    src.name(),
                    src.superName() == null ? "" : src.superName(),
                    src.access(),
                    String.join(",", src.interfaces()));
            return new JsonPrimitive(str);
        }
    }

    private static class MemberDataAdapter implements JsonSerializer<MemberData>, JsonDeserializer<MemberData> {

        @Override
        public MemberData deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
            String[] parts = json.getAsString().split("\\|", -1);
            Set<Entry.Method> methods = new HashSet<>();
            Set<Entry.Field> fields = new HashSet<>();

            if (!parts[1].isEmpty()) {
                Arrays.stream(parts[1].split(","))
                        .map(MemberDataAdapter::parseMethod)
                        .forEach(methods::add);
            }

            if (!parts[2].isEmpty()) {
                Arrays.stream(parts[2].split(","))
                        .map(MemberDataAdapter::parseField)
                        .forEach(fields::add);
            }

            return new MemberData(parts[0], methods, fields);
        }

        @Override
        public JsonElement serialize(MemberData src, Type typeOfSrc, JsonSerializationContext context) {
            String methods = String.join(",", src.methods().stream().map(Entry.Method::str).toList());
            String fields = String.join(",", src.fields().stream().map(Entry.Field::str).toList());
            String str = "%s|%s|%s".formatted(src.className(), methods, fields);
            return new JsonPrimitive(str);
        }

        private static Entry.Method parseMethod(String value) {
            String[] parts = value.split(":", 3);
            return new Entry.Method(parts[0], parts[1], parts[2]);
        }

        private static Entry.Field parseField(String value) {
            String[] parts = value.split(":", 3);
            return new Entry.Field(parts[0], parts[1], parts[2]);
        }
    }
}
