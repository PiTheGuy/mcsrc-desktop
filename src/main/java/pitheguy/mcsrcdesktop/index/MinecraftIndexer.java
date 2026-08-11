package pitheguy.mcsrcdesktop.index;

import mcsrc.Indexer;
import pitheguy.mcsrcdesktop.util.ProgressListener;

import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

public class MinecraftIndexer {
    private final File jar;
    private final List<? extends ZipEntry> entries;

    public MinecraftIndexer(File jar) throws IOException {
        this.jar = jar;
        try (ZipFile zip = new ZipFile(jar)) {
            this.entries = zip.stream()
                    .filter(entry -> !entry.isDirectory())
                    .filter(entry -> entry.getName().endsWith(".class"))
                    .toList();
        }
    }

    public CompletableFuture<Void> index(ProgressListener progressListener) throws IOException {
        AtomicInteger completed = new AtomicInteger();

        ZipFile zip = new ZipFile(jar);
        var future = CompletableFuture.runAsync(() -> entries.forEach(entry -> {
            indexEntry(entry, zip);
            progressListener.update((double) completed.incrementAndGet() / entries.size());
        }));
        future.thenRun(() -> {
            try {
                zip.close();
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        });
        return future;


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

    private static void indexEntry(ZipEntry entry, ZipFile zip) {
        try {
            byte[] bytes = zip.getInputStream(entry).readAllBytes();
            Indexer.index(bytes);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    public String[] getReference(String key) {
        return Indexer.getReference(key);
    }

    public int getReferenceSize() {
        return Indexer.getReferenceSize();
    }

    public String getBytecode(String className) throws IOException {
        try (ZipFile zip = new ZipFile(jar)) {
            ZipEntry entry = zip.getEntry(className + ".class");
            byte[] bytes = zip.getInputStream(entry).readAllBytes();
            return Indexer.getBytecode(new byte[][]{bytes});
        }
    }

    public String[] getClassData() {
        return Indexer.getClassData();
    }

    public String[] getMemberData() {
        return Indexer.getMemberData();
    }
}
