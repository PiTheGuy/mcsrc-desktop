package pitheguy.mcsrcdesktop.download;

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
    private final List<Download> downloads = new ArrayList<>();
    private int totalSize = 0;
    private final AtomicInteger downloaded = new AtomicInteger(0);

    public void addDownload(CompletableFuture<InputStream> future, Path path, int size) {
        downloads.add(new Download(future, path, size));
        if (!future.isDone()) totalSize += size;
    }

    public void start(ProgressListener progressListener) {
        for (Download download : downloads) {
            download.future.thenAccept(in -> {
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
                    throw new UncheckedIOException(e);
                }
            });
        }
    }

    private record Download(CompletableFuture<InputStream> future, Path path, int size) {}
}
