package pitheguy.mcsrcdesktop.cef.query.javadoc;

import com.google.gson.JsonObject;
import com.google.gson.annotations.SerializedName;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.cef.callback.CefQueryCallback;
import pitheguy.mcsrcdesktop.cef.query.CefQuery;
import pitheguy.mcsrcdesktop.cef.query.CefQueryContext;
import pitheguy.mcsrcdesktop.cef.query.CefQueryType;

import java.util.concurrent.CompletableFuture;

import static pitheguy.mcsrcdesktop.util.SharedConstants.GSON;

public interface JavadocQuery extends CefQuery {
    CefQueryType<JavadocQuery> TYPE = CefQueryType.of(JavadocQuery::deserialize);
    Logger LOGGER = LogManager.getLogger(JavadocQuery.class);

    default CefQueryType<JavadocQuery> type() {
        return TYPE;
    }

    static JavadocQuery deserialize(JsonObject json) {
        JavadocQuery.Action action = GSON.fromJson(json.get("action"), JavadocQuery.Action.class);
        return GSON.fromJson(json, action.queryClass());
    }

    default void handle(CefQueryCallback callback, CefQueryContext context) throws Exception {
        Object result = handleAction(callback, context);
        if (result instanceof CompletableFuture<?> future) {
            future.thenRun(() -> callback.success(GSON.toJson(future.join())))
                    .exceptionally(throwable -> {
                        LOGGER.error("Error handling JavadocQuery", throwable);
                        callback.failure(-2, "Failed to handle JavadocQuery \n" + throwable);
                        return null;
                    });
        } else {
            callback.success(GSON.toJson(result));
        }
    }

    Object handleAction(CefQueryCallback callback, CefQueryContext context) throws Exception;

    enum Action {
        @SerializedName("write")
        WRITE(WriteJavadocQuery.class),
        @SerializedName("open_project")
        OPEN_PROJECT(OpenProjectJavadocQuery.class);
        private final Class<? extends JavadocQuery> queryClass;

        Action(Class<? extends JavadocQuery> queryClass) {
            this.queryClass = queryClass;
        }

        public Class<? extends JavadocQuery> queryClass() {
            return queryClass;
        }
    }

}
