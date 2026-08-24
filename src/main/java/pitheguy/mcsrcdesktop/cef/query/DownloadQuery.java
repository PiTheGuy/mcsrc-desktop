package pitheguy.mcsrcdesktop.cef.query;

import com.google.gson.annotations.SerializedName;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.cef.callback.CefQueryCallback;
import pitheguy.mcsrcdesktop.cef.ProgressUpdater;
import pitheguy.mcsrcdesktop.download.MinecraftDownloader;
import pitheguy.mcsrcdesktop.download.VersionInfo;
import pitheguy.mcsrcdesktop.util.FileUtil;

import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;

public record DownloadQuery(
        @SerializedName("version")
        String version
) implements CefQuery {
    public static final CefQueryType<DownloadQuery> TYPE = CefQueryType.of(DownloadQuery.class);
    private static final Logger LOGGER = LogManager.getLogger(DownloadQuery.class);

    @Override
    public CefQueryType<? extends CefQuery> type() {
        return TYPE;
    }

    @Override
    public void handle(CefQueryCallback callback, CefQueryContext context) throws Exception {
        MinecraftDownloader downloader = context.downloader();
        VersionInfo versionInfo = downloader.fetchVersionInfo(version);
        ProgressUpdater progressUpdater = new ProgressUpdater(callback);
        var future = downloader.downloadVersion(versionInfo, progressUpdater);
        future.thenAccept(downloadInfo -> {
            try {
                Response response = new Response(downloadInfo.jar(), downloadInfo.mappings());
                progressUpdater.finish(response);
            } catch (IOException e) {
                LOGGER.error("Failed to read downloaded file", e);
                throw new UncheckedIOException(e);
            }
        });
    }

    private record Response(String jar, String mappings) {
        private Response(File jar, File mappings) throws IOException {
            this(FileUtil.toDataUrl(jar, "application/java-archive"), mappings == null ? null : FileUtil.toDataUrl(mappings, "text/plain"));
        }
    }
}
