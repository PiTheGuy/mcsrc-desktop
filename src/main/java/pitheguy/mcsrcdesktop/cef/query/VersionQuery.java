package pitheguy.mcsrcdesktop.cef.query;

import com.google.gson.annotations.SerializedName;
import org.cef.callback.CefQueryCallback;
import pitheguy.mcsrcdesktop.util.SharedConstants;

import static pitheguy.mcsrcdesktop.util.SharedConstants.GSON;

public record VersionQuery() implements CefQuery {
    public static final CefQueryType<VersionQuery> TYPE = CefQueryType.of(VersionQuery.class);

    @Override
    public CefQueryType<? extends CefQuery> type() {
        return TYPE;
    }

    @Override
    public void handle(CefQueryCallback callback) {
        Version version = new Version(SharedConstants.PROTOCOL_VERSION, SharedConstants.APP_VERSION);
        callback.success(GSON.toJson(version));
    }

    private record Version(
            @SerializedName("protocol")
            int protocol,
            @SerializedName("app")
            String app
    ) {}
}
