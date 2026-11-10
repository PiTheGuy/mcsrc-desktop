package pitheguy.mcsrcdesktop.cef.query;

import com.google.gson.annotations.SerializedName;
import org.cef.callback.CefQueryCallback;
import pitheguy.mcsrcdesktop.decompile.DecompileResult;
import pitheguy.mcsrcdesktop.decompile.DecompilerOptions;
import pitheguy.mcsrcdesktop.decompile.MinecraftDecompiler;
import pitheguy.mcsrcdesktop.javadoc.UnpickConfig;

import static pitheguy.mcsrcdesktop.util.SharedConstants.GSON;

public record DecompileQuery(
        @SerializedName("className")
        String className,
        @SerializedName("version")
        String version,
        @SerializedName("unpick")
        UnpickConfig unpick,
        @SerializedName("options")
        DecompilerOptions options
) implements CefQuery {
    public static final CefQueryType<DecompileQuery> TYPE = CefQueryType.of(DecompileQuery.class);

    @Override
    public CefQueryType<? extends CefQuery> type() {
        return TYPE;
    }

    @Override
    public void handle(CefQueryCallback callback, CefQueryContext context) throws Exception {
        MinecraftDecompiler decompiler = MinecraftDecompiler.create(context.downloader(), version);
        DecompileResult result = decompiler.decompile(className, options, unpick);
        callback.success(GSON.toJson(result));
    }
}
