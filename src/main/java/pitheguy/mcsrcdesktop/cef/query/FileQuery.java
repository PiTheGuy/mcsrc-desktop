package pitheguy.mcsrcdesktop.cef.query;

import com.google.gson.JsonObject;
import com.google.gson.annotations.SerializedName;
import org.cef.callback.CefQueryCallback;
import pitheguy.mcsrcdesktop.cef.ErrorCodes;
import pitheguy.mcsrcdesktop.util.FileUtil;

import java.io.File;
import java.nio.file.DirectoryNotEmptyException;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.util.List;

import static pitheguy.mcsrcdesktop.util.SharedConstants.GSON;

public interface FileQuery extends CefQuery {
    CefQueryType<FileQuery> TYPE = CefQueryType.of(FileQuery::deserialize);

    @Override
    default CefQueryType<? extends CefQuery> type() {
        return TYPE;
    }

    static FileQuery deserialize(JsonObject json) {
        FileQuery.Action action = GSON.fromJson(json.get("action"), FileQuery.Action.class);
        return GSON.fromJson(json, action.queryClass());
    }

    String path();

    default void handle(CefQueryCallback callback, CefQueryContext context) throws Exception {
        try {
            handleAction(callback, context);
        } catch (NoSuchFileException e) {
            callback.failure(ErrorCodes.FILE_NOT_FOUND, "File not found: " + path());
        } catch (DirectoryNotEmptyException e) {
            callback.failure(ErrorCodes.DIRECTORY_NOT_EMPTY, "Directory not empty: " + path());
        }
    }

    void handleAction(CefQueryCallback callback, CefQueryContext context) throws Exception;

    enum Action {
        @SerializedName("read")
        READ(Read.class),
        @SerializedName("write")
        WRITE(Write.class),
        @SerializedName("create_file")
        CREATE_FILE(CreateFile.class),
        @SerializedName("create_directory")
        CREATE_DIRECTORY(CreateDirectory.class),
        @SerializedName("delete")
        DELETE(Delete.class),
        @SerializedName("directory_contents")
        DIRECTORY_CONTENTS(DirectoryContents.class);

        private final Class<? extends FileQuery> queryClass;

        Action(Class<? extends FileQuery> queryClass) {
            this.queryClass = queryClass;
        }

        public Class<? extends FileQuery> queryClass() {
            return queryClass;
        }
    }

    record Read(
            @SerializedName("path")
            String path
    ) implements FileQuery {
        @Override
        public void handleAction(CefQueryCallback callback, CefQueryContext context) throws Exception {
            String url = FileUtil.toDataUrl(new File(path), "unknown");
            callback.success(GSON.toJson(url));
        }
    }

    record Write(
            @SerializedName("path")
            String path,
            @SerializedName("bytes")
            byte[] bytes
    ) implements FileQuery {
        @Override
        public void handleAction(CefQueryCallback callback, CefQueryContext context) throws Exception {
            Files.write(Path.of(path), bytes);
            callback.success(GSON.toJson(null));
        }
    }

    record CreateFile(
            @SerializedName("path")
            String path
    ) implements FileQuery {
        @Override
        public void handleAction(CefQueryCallback callback, CefQueryContext context) throws Exception {
            Files.createFile(Path.of(path));
            callback.success(GSON.toJson(null));
        }
    }

    record CreateDirectory(
            @SerializedName("path")
            String path
    ) implements FileQuery {
        @Override
        public void handleAction(CefQueryCallback callback, CefQueryContext context) throws Exception {
            Files.createDirectory(Path.of(path));
            callback.success(GSON.toJson(null));
        }
    }

    record Delete(
            @SerializedName("path")
            String path,
            @SerializedName("recursive")
            boolean recursive
    ) implements FileQuery {
        @Override
        public void handleAction(CefQueryCallback callback, CefQueryContext context) throws Exception {
            if (recursive) {
                try (var stream = Files.walk(Path.of(path))) {
                    List<Path> paths = stream.toList();
                    for (Path path : paths) Files.delete(path);
                }
            } else {
                Files.delete(Path.of(path));
            }
        }
    }

    record DirectoryContents(
            @SerializedName("path")
            String path
    ) implements FileQuery {
        @Override
        public void handleAction(CefQueryCallback callback, CefQueryContext context) throws Exception {
            try (var stream = Files.list(Path.of(path))) {
                callback.success(GSON.toJson(stream.map(SerializedFile::new).toList()));
            }
        }

        enum SerializedFileKind {
            @SerializedName("file")
            FILE,
            @SerializedName("directory")
            DIRECTORY
        }

        record SerializedFile(
                @SerializedName("kind")
                SerializedFileKind kind,
                @SerializedName("path")
                String path
        ) {
            public SerializedFile(Path path) {
                this(Files.isDirectory(path) ? SerializedFileKind.DIRECTORY : SerializedFileKind.FILE, path.toString().replace(File.separatorChar, '/'));
            }
        }
    }
}
