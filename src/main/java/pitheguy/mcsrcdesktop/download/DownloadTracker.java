package pitheguy.mcsrcdesktop.download;

import pitheguy.mcsrcdesktop.util.ProgressListener;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;

public class DownloadTracker {
    private final List<FileDownload> downloads = new ArrayList<>();
    private int totalSize = 0;
    private final AtomicInteger downloaded = new AtomicInteger(0);

    public void addDownload(FileDownload download) {
        downloads.add(download);
        if (!download.isCached()) {
            totalSize += download.size();
        }
    }

    public void start(ProgressListener progressListener) {
        for (FileDownload download : downloads) {
            download.consume(bytes -> progressListener.update((double) downloaded.addAndGet(bytes) / totalSize));
        }
    }

    public CompletableFuture<Void> future() {
        return CompletableFuture.allOf(downloads.stream().map(FileDownload::future).toArray(CompletableFuture[]::new));
    }
}
