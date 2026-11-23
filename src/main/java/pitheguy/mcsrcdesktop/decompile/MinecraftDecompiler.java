package pitheguy.mcsrcdesktop.decompile;

import mcsrc.BytecodePrinter;
import mcsrc.Unpick;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.java.decompiler.api.Decompiler;
import org.jetbrains.java.decompiler.main.extern.TextTokenVisitor;
import pitheguy.mcsrcdesktop.download.MinecraftDownloader;
import pitheguy.mcsrcdesktop.download.VersionInfo;
import pitheguy.mcsrcdesktop.javadoc.UnpickConfig;
import pitheguy.mcsrcdesktop.javadoc.UnpickDefinition;
import pitheguy.mcsrcdesktop.util.FileUtil;
import pitheguy.mcsrcdesktop.util.SharedConstants;

import java.io.*;
import java.util.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

public class MinecraftDecompiler {
    private static final Logger LOGGER = LogManager.getLogger(MinecraftDecompiler.class);

    private static UnpickConfig lastUnpickConfig;

    private final String version;
    private final File jar;
    private final List<File> libraries;
    private final Unpick unpick;

    private MinecraftDecompiler(String version, File jar, List<File> libraries) {
        this.version = version;
        this.jar = jar;
        this.libraries = libraries;
        this.unpick = new Unpick(this::getClassBytes);
    }

    public static MinecraftDecompiler create(MinecraftDownloader downloader, String version) throws IOException, InterruptedException {
        VersionInfo versionInfo = downloader.fetchVersionInfo(version);
        File jar = downloader.fetchRemappedJar(versionInfo);
        List<File> libraries = downloader.fetchLibraries(versionInfo);
        return new MinecraftDecompiler(version, jar, libraries);
    }

    public DecompileResult decompile(String className, DecompilerOptions options, UnpickConfig unpickConfig) {
        try {
            if (!Objects.equals(unpickConfig, lastUnpickConfig)) {
                DecompilerCache.clear();
                lastUnpickConfig = unpickConfig;
            }
            if (DecompilerCache.contains(className, version, options)) {
                return DecompilerCache.get(className, version, options);
            }
            checkVersionDownloaded();
            List<File> allLibraries = new ArrayList<>(libraries);
            allLibraries.add(jar);
            Map<String, String> output = new HashMap<>();
            if (unpickConfig != null) {
                String[] definitions = unpickConfig.definitions().stream()
                        .map(UnpickDefinition::content)
                        .toArray(String[]::new);
                unpick.loadDefinitions(definitions);
            }
            ClassSource classSource = new ClassSource(jar, className, output, unpick, unpickConfig != null);
            Map<String, List<Token>> tokens = new HashMap<>();
            Decompiler decompiler = Decompiler.builder()
                    .inputs(classSource)
                    .libraries(allLibraries.toArray(File[]::new))
                    .options(options.toVineflowerOptions())
                    .output(ResultSaverImpl.INSTANCE)
                    .build();
            TextTokenVisitor.addVisitor(next -> new TokenCollector(next, tokens));
            decompiler.decompile();

            String source = output.get(className);
            Token[] classTokens = tokens.get(source).toArray(Token[]::new);
            DecompileResult result = new DecompileResult(className, FileUtil.stripExtension(jar.getName()), classSource.crc32(), source, classTokens, DecompileResult.Language.JAVA, SharedConstants.VINEFLOWER_VERSION);
            DecompilerCache.put(className, version, options, result);
            return result;
        } catch (Exception e) {
            StackTraceWriter sw = new StackTraceWriter();
            e.printStackTrace(new PrintWriter(sw));
            String message = "// Error during decompilation" + "\n" + sw;
            return new DecompileResult(className, FileUtil.stripExtension(jar.getName()), 0, message, new Token[0], DecompileResult.Language.JAVA, SharedConstants.VINEFLOWER_VERSION);
        }
    }

    public DecompileResult getBytecode(String className) {
        checkVersionDownloaded();
        try (ZipFile zip = new ZipFile(jar)){
            ZipEntry entry = zip.getEntry(className + ".class");
            byte[] bytes = zip.getInputStream(entry).readAllBytes();
            String result = BytecodePrinter.print(bytes);
            return new DecompileResult(className, FileUtil.stripExtension(jar.getName()), entry.getCrc(), result, new Token[0], DecompileResult.Language.BYTECODE, SharedConstants.VINEFLOWER_VERSION);
        } catch (Exception e) {
            StackTraceWriter sw = new StackTraceWriter();
            e.printStackTrace(new PrintWriter(sw));
            String message = "// Error during bytecode retrieval" + "\n" + sw;
            return new DecompileResult(className, FileUtil.stripExtension(jar.getName()), 0, message, new Token[0], DecompileResult.Language.BYTECODE, SharedConstants.VINEFLOWER_VERSION);
        }
    }

    private void checkVersionDownloaded() {
        if (!jar.exists() || libraries.stream().anyMatch(lib -> !lib.exists()))
            throw new IllegalStateException("Version not downloaded!");
    }

    private byte[] getClassBytes(String className) {
        byte[] jarResult = findClassInJar(jar, className);
        if (jarResult != null) return jarResult;
        for (File library : libraries) {
            byte[] libraryResult = findClassInJar(library, className);
            if (libraryResult != null) return libraryResult;
        }
        return null;
    }

    private byte[] findClassInJar(File jar, String className) {
        try (ZipFile zipFile = new ZipFile(jar)) {
            ZipEntry entry = zipFile.getEntry(className + ".class");
            if (entry == null) return null;
            InputStream stream = zipFile.getInputStream(entry);
            return stream.readAllBytes();
        } catch (IOException e) {
            LOGGER.error("Failed to open jar: {}", jar.getName(), e);
            return null;
        }
    }

    private static class StackTraceWriter extends StringWriter {
        private boolean atLineStart = true;

        @Override
        public void write(String str) {
            super.write(processString(str));
        }

        @Override
        public void write(String str, int off, int len) {
            super.write(processString(str.substring(off, off + len)));
        }

        @Override
        public void write(char[] cbuf, int off, int len) {
            super.write(processString(new String(cbuf, off, len)));
        }

        private String processString(String str) {
            StringBuilder result = new StringBuilder();
            for (int i = 0; i < str.length(); i++) {
                char c = str.charAt(i);
                if (atLineStart && c != '\n' && c != '\r') {
                    result.append("// ");
                    atLineStart = false;
                }
                result.append(c);
                if (c == '\n') {
                    atLineStart = true;
                }
            }
            return result.toString();
        }
    }

}
