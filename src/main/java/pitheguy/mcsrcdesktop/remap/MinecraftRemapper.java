package pitheguy.mcsrcdesktop.remap;

import mcsrc.ClassFileRemapper;
import mcsrc.IndexData;
import mcsrc.Indexer;
import pitheguy.mcsrcdesktop.util.ProgressListener;

import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;

public class MinecraftRemapper {
    private final File jar;
    private final File mappings;

    public MinecraftRemapper(File jar, File mappings) {
        this.jar = jar;
        this.mappings = mappings;
    }

    public File remap(ProgressListener progressListener) {
        if (getOutputJar().exists()) {
            return getOutputJar();
        }
        try (ZipFile zip = new ZipFile(jar)) {
            Indexer indexer = new Indexer();
            var entries = zip.stream()
                    .filter(entry -> !entry.isDirectory())
                    .filter(entry -> entry.getName().endsWith(".class"))
                    .toList();
            AtomicInteger completed = new AtomicInteger();
            for (var entry : entries) {
                byte[] bytes = zip.getInputStream(entry).readAllBytes();
                indexer.indexDeclarations(bytes);
                progressListener.update((double) completed.incrementAndGet() / entries.size() * 0.5);
            }
            progressListener.update(0.5);
            completed.set(0);
            IndexData data = indexer.data();
            ClassFileRemapper remapper = new ClassFileRemapper(Files.readAllBytes(mappings.toPath()), data);
            Map<String, String> classMappings = remapper.classMappings();
            ZipOutputStream outputStream = new ZipOutputStream(Files.newOutputStream(getOutputJar().toPath()));
            for (var entry : entries) {
                String oldName = entry.getName().substring(0, entry.getName().length() - ".class".length());
                String newName = classMappings.get(oldName);
                ZipEntry newEntry = new ZipEntry(newName + ".class");
                outputStream.putNextEntry(newEntry);
                byte[] bytes = remapper.remap(zip.getInputStream(entry).readAllBytes());
                outputStream.write(bytes);
                outputStream.closeEntry();
                progressListener.update(0.5 + (double) completed.incrementAndGet() / entries.size() * 0.5);
            }
            outputStream.finish();
            outputStream.close();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        return getOutputJar();
    }

    private File getOutputJar() {
        String fileName = jar.getName().replace(".jar", "-remapped.jar");
        return new File(jar.getParentFile(), fileName);
    }
}
