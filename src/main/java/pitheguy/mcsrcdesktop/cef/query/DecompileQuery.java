package pitheguy.mcsrcdesktop.cef.query;

import com.google.gson.annotations.SerializedName;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.cef.callback.CefQueryCallback;
import pitheguy.mcsrcdesktop.decompile.DecompileResult;
import pitheguy.mcsrcdesktop.decompile.DecompilerOptions;
import pitheguy.mcsrcdesktop.decompile.MinecraftDecompiler;

import static pitheguy.mcsrcdesktop.cef.EventHandler.GSON;

public record DecompileQuery(
        @SerializedName("className")
        String className,
        @SerializedName("version")
        String version,
        @SerializedName("options")
        DecompilerOptions options
) implements CefQuery {
    public static final CefQueryType<DecompileQuery> TYPE = CefQueryType.of(DecompileQuery.class);
    private static final Logger LOGGER = LogManager.getLogger(DecompileQuery.class);

    @Override
    public CefQueryType<? extends CefQuery> type() {
        return TYPE;
    }

    @Override
    public void handle(CefQueryCallback callback, CefQueryContext context) {
        try {
            MinecraftDecompiler decompiler = MinecraftDecompiler.create(context.downloader(), version);
            DecompileResult result = decompiler.decompile(className, options);
            callback.success(GSON.toJson(result));
        } catch (Exception e) {
            LOGGER.error("Failed to decompile {}", className, e);
            callback.failure(-2, "Decompile failed: " + e.getMessage());
        }
    }
}
