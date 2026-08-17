package pitheguy.mcsrcdesktop.download;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import pitheguy.mcsrcdesktop.util.ProgressListener;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;

public class DownloadTracker {
    public static final Logger LOGGER = LogManager.getLogger();
    private final List<Download> downloads = new ArrayList<>();
    private int totalSize = 0;
    private final AtomicInteger downloaded = new AtomicInteger(0);

    public void addDownload(CompletableFuture<InputStream> future, Path path, int size) {
        if (!future.isDone()) {
            downloads.add(new Download(future, path, size));
            totalSize += size;
        }
    }

    public void start(ProgressListener progressListener) {
        for (Download download : downloads) {
            download.future.thenAccept(in -> {
                if (in == null) return;
                try (in; OutputStream out = Files.newOutputStream(download.path())) {
                    byte[] buffer = new byte[8192];
                    int read;

                    while ((read = in.read(buffer)) != -1) {
                        out.write(buffer, 0, read);
                        downloaded.addAndGet(read);

                        if (download.size() > 0) {
                            progressListener.update((double) downloaded.get() / totalSize);
                        }
                    }
                } catch (IOException e) {
                    LOGGER.error("Failed to download {}", download.path(), e);
                    throw new UncheckedIOException(e);
                }
            });
        }
    }

    private record Download(CompletableFuture<InputStream> future, Path path, int size) {}
}
