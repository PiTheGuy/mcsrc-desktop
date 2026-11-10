package pitheguy.mcsrcdesktop.decompile;

import mcsrc.Unpick;
import org.jetbrains.java.decompiler.main.extern.IContextSource;
import org.jetbrains.java.decompiler.main.extern.IResultSaver;

import java.io.*;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

public class ClassSource implements IContextSource {
    private final String className;
    private final Map<String, String> output;
    private final Unpick unpick;
    private final boolean transform;
    private final ZipFile zipFile;

    public ClassSource(File inputJar, String className, Map<String, String> output, Unpick unpick, boolean transform) {
        this.className = className;
        this.output = output;
        this.unpick = unpick;
        this.transform = transform;
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
        List<Entry> classes = zipFile.stream()
                .filter(zipEntry -> !zipEntry.isDirectory())
                .map(ZipEntry::getName)
                .filter(name -> name.endsWith(".class"))
                .filter(this::isOwn)
                .map(name -> Entry.parse(name.substring(0, name.length() - CLASS_SUFFIX.length())))
                .toList();
        return new Entries(classes, List.of(), List.of());
    }

    @Override
    public InputStream getInputStream(String resource) throws IOException {
        if (isOwn(resource)) {
            InputStream entryInputStream = zipFile.getInputStream(zipFile.getEntry(resource));
            if (!transform) return entryInputStream;
            byte[] transformedClass = unpick.apply(entryInputStream.readAllBytes());
            return new ByteArrayInputStream(transformedClass);
        } else {
            return null;
        }
    }



    private boolean isOwn(String className) {
        return className.equals(this.className + CLASS_SUFFIX) || className.startsWith(this.className + "$");
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
