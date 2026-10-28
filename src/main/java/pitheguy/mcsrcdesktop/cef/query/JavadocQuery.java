package pitheguy.mcsrcdesktop.cef.query;

import com.google.gson.JsonObject;
import com.google.gson.annotations.SerializedName;
import mcsrc.JavadocMappings;
import net.fabricmc.mappingio.format.MappingFormat;
import org.cef.callback.CefQueryCallback;

import static pitheguy.mcsrcdesktop.util.SharedConstants.GSON;

public interface JavadocQuery extends CefQuery {
    CefQueryType<JavadocQuery> TYPE = CefQueryType.of(JavadocQuery::deserialize);
    JavadocMappings MAPPINGS = new JavadocMappings();

    default CefQueryType<JavadocQuery> type() {
        return TYPE;
    }

    static JavadocQuery deserialize(JsonObject json) {
        Action action = GSON.fromJson(json.get("action"), Action.class);
        return GSON.fromJson(json, action.queryClass());
    }

    Object handleAction(CefQueryCallback callback, CefQueryContext context) throws Exception;

    default void handle(CefQueryCallback callback, CefQueryContext context) throws Exception {
        callback.success(GSON.toJson(handleAction(callback, context)));
    }

    private static JavadocMappings.ElementKind elementKind(int kind) {
        return switch (kind) {
            case 0 -> JavadocMappings.ElementKind.CLASS;
            case 1 -> JavadocMappings.ElementKind.FIELD;
            case 2 -> JavadocMappings.ElementKind.METHOD;
            default -> throw new IllegalArgumentException("Unknown element kind: " + kind);
        };
    }

    enum Action {
        @SerializedName("readJavadocs")
        READ_JAVADOCS(ReadJavadocs.class),
        @SerializedName("beginJavadocDirectory")
        BEGIN_JAVADOC_DIRECTORY(BeginJavadocDirectory.class),
        @SerializedName("readJavadocDirectoryFile")
        READ_JAVADOC_DIRECTORY_FILE(ReadJavadocDirectoryFile.class),
        @SerializedName("finishJavadocDirectory")
        FINISH_JAVADOC_DIRECTORY(FinishJavadocDirectory.class),
        @SerializedName("abortJavadocDirectory")
        ABORT_JAVADOC_DIRECTORY(AbortJavadocDirectory.class),
        @SerializedName("writeJavadocs")
        WRITE_JAVADOCS(WriteJavadocs.class),
        @SerializedName("writeJavadocClass")
        WRITE_JAVADOC_CLASS(WriteJavadocClass.class),
        @SerializedName("createJavadocs")
        CREATE_JAVADOCS(CreateJavadocs.class),
        @SerializedName("resetJavadocs")
        RESET_JAVADOCS(ResetJavadocs.class),
        @SerializedName("getJavadoc")
        GET_JAVADOC(GetJavadoc.class),
        @SerializedName("setJavadoc")
        SET_JAVADOC(SetJavadoc.class);

        private final Class<? extends JavadocQuery> queryClass;

        Action(Class<? extends JavadocQuery> queryClass) {
            this.queryClass = queryClass;
        }

        public Class<? extends JavadocQuery> queryClass() {
            return queryClass;
        }
    }

    record ReadJavadocs(
            @SerializedName("bytes")
            byte[] bytes
    ) implements JavadocQuery {
        @Override
        public MappingFormat handleAction(CefQueryCallback callback, CefQueryContext context) throws Exception {
            return MAPPINGS.read(bytes);
        }
    }

    record BeginJavadocDirectory() implements JavadocQuery {
        @Override
        public Object handleAction(CefQueryCallback callback, CefQueryContext context) throws Exception {
            MAPPINGS.beginDirectory();
            return null;
        }
    }

    record ReadJavadocDirectoryFile(
            @SerializedName("bytes")
            byte[] bytes,
            @SerializedName("path")
            String path
    ) implements JavadocQuery {
        @Override
        public String handleAction(CefQueryCallback callback, CefQueryContext context) throws Exception {
            return MAPPINGS.readDirectoryFile(bytes, path);
        }
    }

    record FinishJavadocDirectory() implements JavadocQuery {
        @Override
        public Object handleAction(CefQueryCallback callback, CefQueryContext context) throws Exception {
            MAPPINGS.finishDirectory();
            return null;
        }
    }

    record AbortJavadocDirectory() implements JavadocQuery {
        @Override
        public Object handleAction(CefQueryCallback callback, CefQueryContext context) throws Exception {
            MAPPINGS.abortDirectory();
            return null;
        }
    }

    record WriteJavadocs(
            @SerializedName("format")
            MappingFormat format
    ) implements JavadocQuery {
        @Override
        public byte[] handleAction(CefQueryCallback callback, CefQueryContext context) throws Exception {
            return MAPPINGS.write(format);
        }
    }

    record WriteJavadocClass(
            @SerializedName("owner")
            String owner
    ) implements JavadocQuery {
        @Override
        public Object handleAction(CefQueryCallback callback, CefQueryContext context) throws Exception {
            return MAPPINGS.writeClass(owner);
        }
    }

    record CreateJavadocs(
            @SerializedName("format")
            MappingFormat format
    ) implements JavadocQuery {
        @Override
        public Object handleAction(CefQueryCallback callback, CefQueryContext context) throws Exception {
            return new JavadocMappings().write(format);
        }
    }

    record ResetJavadocs() implements JavadocQuery {
        @Override
        public Object handleAction(CefQueryCallback callback, CefQueryContext context) throws Exception {
            MAPPINGS.reset();
            return null;
        }
    }

    record GetJavadoc(
            @SerializedName("kind")
            int kind,
            @SerializedName("owner")
            String owner,
            @SerializedName("name")
            String name,
            @SerializedName("descriptor")
            String descriptor
    ) implements JavadocQuery {
        @Override
        public Object handleAction(CefQueryCallback callback, CefQueryContext context) throws Exception {
            return MAPPINGS.get(elementKind(kind), owner, name, descriptor);
        }
    }

    record SetJavadoc(
            @SerializedName("kind")
            int kind,
            @SerializedName("owner")
            String owner,
            @SerializedName("name")
            String name,
            @SerializedName("descriptor")
            String descriptor,
            @SerializedName("comment")
            String comment
    ) implements JavadocQuery {
        @Override
        public Object handleAction(CefQueryCallback callback, CefQueryContext context) throws Exception {
            MAPPINGS.set(elementKind(kind), owner, name, descriptor, comment);
            return null;
        }
    }
}
