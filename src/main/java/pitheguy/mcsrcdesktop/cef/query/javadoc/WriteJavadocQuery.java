package pitheguy.mcsrcdesktop.cef.query.javadoc;

import com.google.gson.annotations.SerializedName;
import org.cef.callback.CefQueryCallback;
import pitheguy.mcsrcdesktop.cef.query.CefQueryContext;
import pitheguy.mcsrcdesktop.util.FileUtil;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Base64;

public record WriteJavadocQuery(
        @SerializedName("path")
        Path path,
        @SerializedName("base64")
        String base64) implements JavadocQuery {
    @Override
    public Object handleAction(CefQueryCallback callback, CefQueryContext context) throws Exception {
        Path tempFile = Files.createTempFile(path.getFileName().toString(), ".mapping");
        byte[] bytes = Base64.getDecoder().decode(base64);
        Files.write(tempFile, bytes);
        Files.createDirectories(path.getParent());
        FileUtil.safeAtomicMove(tempFile, path);
        return null;
    }
}
