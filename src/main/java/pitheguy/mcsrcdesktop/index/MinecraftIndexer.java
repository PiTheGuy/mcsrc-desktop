package pitheguy.mcsrcdesktop.index;

import mcsrc.IndexData;
import mcsrc.Indexer;
import pitheguy.mcsrcdesktop.util.ProgressListener;

import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

public class MinecraftIndexer {
    private final File jar;
    private final Indexer indexer = new Indexer();

    public MinecraftIndexer(File jar) {
        this.jar = jar;
    }

    public CompletableFuture<Void> index(ProgressListener progressListener) {
        AtomicInteger completed = new AtomicInteger();

        return CompletableFuture.runAsync(() -> {
            try (ZipFile zip = new ZipFile(jar)) {
                var entries = zip.stream()
                        .filter(entry -> !entry.isDirectory())
                        .filter(entry -> entry.getName().endsWith(".class"))
                        .toList();
                entries.forEach(entry -> {
                    indexEntry(entry, zip);
                    progressListener.update((double) completed.incrementAndGet() / entries.size());
                });
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        });


        //TODO fix concurrent implementation
//        AtomicInteger completed = new AtomicInteger();
//        List<CompletableFuture<Void>> futures = new ArrayList<>();
//        ExecutorService executor = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());
//        try (ZipFile zip = new ZipFile(jar)) {
//            for (ZipEntry entry : entries) {
//                futures.add(CompletableFuture.runAsync(() -> {
//                    indexEntry(entry, zip);
//                    progressListener.update((double) completed.incrementAndGet() / entries.size());
//                }, executor));
//            }
//        }
//        return CompletableFuture.allOf(futures.toArray(CompletableFuture[]::new));
    }

    private void indexEntry(ZipEntry entry, ZipFile zip) {
        try {
            byte[] bytes = zip.getInputStream(entry).readAllBytes();
            indexer.index(bytes);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    public Set<String> references(String key) {
        return indexer.references(key);
    }

    public int referenceCount() {
        return indexer.referenceCount();
    }

    public IndexData data() {
        return indexer.data();
    }
}
