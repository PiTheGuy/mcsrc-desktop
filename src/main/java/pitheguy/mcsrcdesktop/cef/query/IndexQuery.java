package pitheguy.mcsrcdesktop.cef.query;

import com.google.gson.JsonObject;
import com.google.gson.annotations.SerializedName;
import mcsrc.ClassData;
import mcsrc.MemberData;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.cef.callback.CefQueryCallback;
import pitheguy.mcsrcdesktop.cef.ProgressUpdater;
import pitheguy.mcsrcdesktop.download.VersionInfo;
import pitheguy.mcsrcdesktop.index.MinecraftIndexer;

import java.io.File;

import static pitheguy.mcsrcdesktop.util.SharedConstants.GSON;

public interface IndexQuery extends CefQuery {
    CefQueryType<IndexQuery> TYPE = CefQueryType.of(IndexQuery::deserialize);
    Logger LOGGER = LogManager.getLogger();

    @Override
    default CefQueryType<? extends CefQuery> type() {
        return TYPE;
    }

    static IndexQuery deserialize(JsonObject json) {
        Action action = GSON.fromJson(json.get("type"), Action.class); // TODO change to action for protocol version 2
        return GSON.fromJson(json, action.queryClass());
    }

    enum Action {
        @SerializedName("start")
        START(Start.class),
        @SerializedName("getReference")
        GET_REFERENCE(GetReference.class),
        @SerializedName("getReferenceSize")
        GET_REFERENCE_SIZE(GetReferenceSize.class),
        @SerializedName("getClassData")
        GET_CLASS_DATA(GetClassData.class),
        @SerializedName("getMemberData")
        GET_MEMBER_DATA(GetMemberData.class);

        private final Class<? extends IndexQuery> queryClass;

        Action(Class<? extends IndexQuery> queryClass) {
            this.queryClass = queryClass;
        }

        public Class<? extends IndexQuery> queryClass() {
            return queryClass;
        }
    }

    record Start(
            @SerializedName("version")
            String version
    ) implements IndexQuery {
        @Override
        public void handle(CefQueryCallback callback, CefQueryContext context) throws Exception {
            VersionInfo versionInfo = context.downloader().fetchVersionInfo(version);
            File jar = context.downloader().fetchRemappedJar(versionInfo);
            context.setIndexer(new MinecraftIndexer(jar));
            ProgressUpdater progressUpdater = new ProgressUpdater(callback);
            context.indexer().index(progressUpdater)
                    .thenRun(() -> progressUpdater.finish(null))
                    .exceptionally(e -> {
                        LOGGER.error("Failed to index version {}", version, e);
                        callback.failure(-2, "Indexing failed: " + e.getMessage());
                        return null;
                    });
        }
    }

    record GetReference(
            @SerializedName("key")
            String key
    ) implements IndexQuery {
        @Override
        public void handle(CefQueryCallback callback, CefQueryContext context) {
            String[] references = context.indexer().references(key).toArray(String[]::new);
            callback.success(GSON.toJson(references));
        }
    }

    record GetReferenceSize() implements IndexQuery {
        @Override
        public void handle(CefQueryCallback callback, CefQueryContext context) {
            int size = context.indexer().referenceCount();
            callback.success(GSON.toJson(size));
        }
    }

    record GetClassData() implements IndexQuery {
        @Override
        public void handle(CefQueryCallback callback, CefQueryContext context) {
            ClassData[] classData = context.indexer().data().classes().values().toArray(ClassData[]::new);
            callback.success(GSON.toJson(classData));
        }
    }

    record GetMemberData() implements IndexQuery {
        @Override
        public void handle(CefQueryCallback callback, CefQueryContext context) {
            MemberData[] memberData = context.indexer().data().members().values().toArray(MemberData[]::new);
            callback.success(GSON.toJson(memberData));
        }
    }
}
