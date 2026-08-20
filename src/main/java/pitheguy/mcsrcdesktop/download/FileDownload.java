package pitheguy.mcsrcdesktop.download;

import com.google.common.hash.Hashing;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.*;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.CompletableFuture;

public class FileDownload {
    private static final Logger LOGGER = LogManager.getLogger(FileDownload.class);

    private final String url;
    private final Path path;
    private final String sha1;
    private final int size;
    private final boolean cached;
    private CompletableFuture<InputStream> future;
    private boolean cancelled = false;

    public FileDownload(String url, Path path, String sha1, int size) throws IOException {
        this.url = url;
        this.path = path;
        this.sha1 = sha1;
        this.size = size;
        this.cached = checkCache();
        Files.createDirectories(path.getParent());
    }

    public int size() {
        return size;
    }

    public boolean isCached() {
        return cached;
    }

    public CompletableFuture<File> future() {
        return future != null ? future.thenApply(_ -> path.toFile()) : CompletableFuture.completedFuture(path.toFile());
    }

    public void fetch(HttpClient client) {
        if (cached) return;
        LOGGER.info("Downloading {} ({} bytes)", path.getFileName(), size);
        HttpRequest request = HttpRequest.newBuilder().uri(URI.create(url)).build();
        future = client.sendAsync(request, HttpResponse.BodyHandlers.ofInputStream())
                .thenApply(HttpResponse::body);
    }

    public void consume(DownloadListener listener) {
        if (cached) return;
        if (future == null) throw new IllegalStateException("Download must be fetched before it can be consumed!");
        future.thenAccept(in -> {
            try (in; OutputStream out = Files.newOutputStream(path)) {
                byte[] buffer = new byte[8192];
                int bytesRead;
                while ((bytesRead = in.read(buffer)) != -1 && !cancelled) {
                    out.write(buffer, 0, bytesRead);
                    listener.onBytesDownloaded(bytesRead);
                }
                out.flush();
                if (cancelled) {
                    Files.delete(path);
                }
            } catch (IOException e) {
                LOGGER.error("Failed to download {}", path.getFileName(), e);
                throw new UncheckedIOException(e);
            }
        });
    }

    private boolean checkCache() throws IOException {
        if (Files.exists(path)) {
            String fileHash = Hashing.sha1().hashBytes(Files.readAllBytes(path)).toString();
            if (Files.size(path) == size && fileHash.equals(sha1)) {
                return true;
            } else {
                LOGGER.warn("File {} already exists, but hash does not match. Deleting.", path.getFileName());
                Files.delete(path);
            }
        }
        return false;
    }

    public void cancel() {
        cancelled = true;
    }

    @FunctionalInterface
    public interface DownloadListener {
        void onBytesDownloaded(int bytes);
    }
}
