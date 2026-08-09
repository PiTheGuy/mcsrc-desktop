package pitheguy.mcsrcdesktop.decompile;

import org.jetbrains.java.decompiler.main.extern.IContextSource;
import org.jetbrains.java.decompiler.main.extern.IResultSaver;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipFile;

public class ClassSource implements IContextSource {
    private final String className;
    private final Map<String, String> output;
    private final ZipFile zipFile;

    public ClassSource(File inputJar, String className, Map<String, String> output) {
        this.className = className;
        this.output = output;
        try {
            this.zipFile = new ZipFile(inputJar);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    @Override
    public String getName() {
        return "class " + className;
    }

    @Override
    public Entries getEntries() {
        return new Entries(List.of(Entry.parse(className)), List.of(), List.of());
    }

    @Override
    public InputStream getInputStream(String resource) throws IOException {
        if (resource.equals(this.className + CLASS_SUFFIX)) {
            return zipFile.getInputStream(zipFile.getEntry(resource));
        } else {
            return null;
        }
    }

    public long crc32() {
        return zipFile.getEntry(className + CLASS_SUFFIX).getCrc();
    }

    @Override
    public IOutputSink createOutputSink(IResultSaver saver) {
        return new IOutputSink() {
            @Override
            public void begin() {

            }

            @Override
            public void acceptClass(String qualifiedName, String fileName, String content, int[] mapping) {
                output.put(qualifiedName, content);
            }

            @Override
            public void acceptDirectory(String directory) {

            }

            @Override
            public void acceptOther(String path) {

            }

            @Override
            public void close() {

            }
        };
    }
}
