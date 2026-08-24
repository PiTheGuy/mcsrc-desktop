package pitheguy.mcsrcdesktop.cef.query;

import com.google.gson.annotations.SerializedName;
import org.cef.callback.CefQueryCallback;
import pitheguy.mcsrcdesktop.decompile.DecompileResult;
import pitheguy.mcsrcdesktop.decompile.MinecraftDecompiler;

import static pitheguy.mcsrcdesktop.util.SharedConstants.GSON;

public record BytecodeQuery(
        @SerializedName("className")
        String className,
        @SerializedName("version")
        String version
) implements CefQuery {
    public static final CefQueryType<BytecodeQuery> TYPE = CefQueryType.of(BytecodeQuery.class);

    @Override
    public CefQueryType<? extends CefQuery> type() {
        return TYPE;
    }

    @Override
    public void handle(CefQueryCallback callback, CefQueryContext context) throws Exception {
        MinecraftDecompiler decompiler = MinecraftDecompiler.create(context.downloader(), version);
        DecompileResult result = decompiler.getBytecode(className);
        callback.success(GSON.toJson(result));
    }
}
