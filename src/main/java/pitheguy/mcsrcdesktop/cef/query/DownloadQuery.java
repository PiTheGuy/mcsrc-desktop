package pitheguy.mcsrcdesktop.cef.query;

import com.google.gson.JsonObject;
import com.google.gson.annotations.SerializedName;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.cef.callback.CefQueryCallback;
import pitheguy.mcsrcdesktop.cef.ProgressUpdater;
import pitheguy.mcsrcdesktop.download.MinecraftDownloader;
import pitheguy.mcsrcdesktop.download.VersionInfo;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.util.Base64;

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
    public void handle(CefQueryCallback callback, CefQueryContext context) {
        try {
            MinecraftDownloader downloader = context.downloader();
            VersionInfo versionInfo = downloader.fetchVersionInfo(version);
            ProgressUpdater progressUpdater = new ProgressUpdater(callback);
            var future = downloader.downloadVersion(versionInfo, progressUpdater);
            future.thenAccept(downloadInfo -> {
                try {
                    JsonObject response = new JsonObject();
                    String jarUrl = "data:application/java-archive;base64," + Base64.getEncoder().encodeToString(Files.readAllBytes(downloadInfo.jar().toPath()));
                    response.addProperty("jar", jarUrl);
                    if (downloadInfo.mappings() != null) {
                        String mappingsUrl = "data:text/plain;base64," + Base64.getEncoder().encodeToString(Files.readAllBytes(downloadInfo.mappings().toPath()));
                        response.addProperty("mappings", mappingsUrl);
                    }
                    progressUpdater.finish(response);
                } catch (IOException e) {
                    LOGGER.error("Failed to read downloaded file", e);
                    throw new UncheckedIOException(e);
                }
            });
        } catch (Exception e) {
            LOGGER.error("Failed to download version {}", version, e);
            callback.failure(-2, "Download failed: " + e.getMessage());
        }
    }
}
