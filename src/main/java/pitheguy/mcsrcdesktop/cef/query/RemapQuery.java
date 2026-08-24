package pitheguy.mcsrcdesktop.cef.query;

import com.google.gson.annotations.SerializedName;
import org.cef.callback.CefQueryCallback;
import pitheguy.mcsrcdesktop.cef.ProgressUpdater;
import pitheguy.mcsrcdesktop.download.VersionInfo;
import pitheguy.mcsrcdesktop.remap.MinecraftRemapper;
import pitheguy.mcsrcdesktop.util.FileUtil;

import java.io.File;

public record RemapQuery(
        @SerializedName("version")
        String version
) implements CefQuery {
    public static final CefQueryType<RemapQuery> TYPE = CefQueryType.of(RemapQuery.class);

    @Override
    public CefQueryType<? extends CefQuery> type() {
        return TYPE;
    }

    @Override
    public void handle(CefQueryCallback callback, CefQueryContext context) throws Exception {
        ProgressUpdater progressUpdater = new ProgressUpdater(callback);
        VersionInfo versionInfo = context.downloader().fetchVersionInfo(version);
        File jar = context.downloader().fetchJar(versionInfo);
        File mappings = context.downloader().fetchMappings(versionInfo);
        MinecraftRemapper remapper = new MinecraftRemapper(jar, mappings);
        File remappedJar = remapper.remap(progressUpdater);
        progressUpdater.finish(FileUtil.toDataUrl(remappedJar, "application/java-archive"));
    }
}
