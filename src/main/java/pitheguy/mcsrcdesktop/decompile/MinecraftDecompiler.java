package pitheguy.mcsrcdesktop.decompile;

import mcsrc.BytecodePrinter;
import org.jetbrains.java.decompiler.api.Decompiler;
import org.jetbrains.java.decompiler.main.extern.TextTokenVisitor;
import pitheguy.mcsrcdesktop.download.MinecraftDownloader;
import pitheguy.mcsrcdesktop.download.VersionInfo;
import pitheguy.mcsrcdesktop.download.VersionManifest;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

public class MinecraftDecompiler {
    private final String version;
    private final File jar;
    private final List<File> libraries;

    private MinecraftDecompiler(String version, File jar, List<File> libraries) {
        this.version = version;
        this.jar = jar;
        this.libraries = libraries;
    }

    public static MinecraftDecompiler create(MinecraftDownloader downloader, String version) throws IOException, InterruptedException {
        VersionManifest manifest = downloader.fetchVersionManifest();
        VersionInfo versionInfo = downloader.fetchVersionInfo(manifest, version);
        File jar = downloader.fetchJar(versionInfo);
        List<File> libraries = downloader.fetchLibraries(versionInfo);
        return new MinecraftDecompiler(version, jar, libraries);
    }

    public DecompileResult decompile(String className) {
        try {
            if (DecompilerCache.contains(className, version)) {
                return DecompilerCache.get(className, version);
            }
            checkVersionDownloaded();
            List<File> allLibraries = new ArrayList<>(libraries);
            allLibraries.add(jar);
            Map<String, String> output = new HashMap<>();
            ClassSource classSource = new ClassSource(jar, className, output);
            Map<String, List<Token>> tokens = new HashMap<>();
            Decompiler decompiler = Decompiler.builder()
                    .inputs(classSource)
                    .libraries(allLibraries.toArray(File[]::new))
                    .output(ResultSaverImpl.INSTANCE)
                    .build();
            TextTokenVisitor.addVisitor(next -> new TokenCollector(next, tokens));
            decompiler.decompile();

            String source = output.get(className);
            Token[] classTokens = tokens.get(source).toArray(Token[]::new);
            DecompileResult result = new DecompileResult(className, classSource.crc32(), source, classTokens, DecompileResult.Language.JAVA);
            DecompilerCache.put(className, version, result);
            return result;
        } catch (Exception e) {
            StackTraceWriter sw = new StackTraceWriter();
            e.printStackTrace(new PrintWriter(sw));
            String message = "// Error during decompilation" + "\n" + sw;
            return new DecompileResult(className, 0, message, new Token[0], DecompileResult.Language.JAVA);
        }
    }

    public DecompileResult getBytecode(String className) {
        checkVersionDownloaded();
        try (ZipFile zip = new ZipFile(jar)){
            ZipEntry entry = zip.getEntry(className + ".class");
            byte[] bytes = zip.getInputStream(entry).readAllBytes();
            String result = BytecodePrinter.print(bytes);
            return new DecompileResult(className, entry.getCrc(), result, new Token[0], DecompileResult.Language.BYTECODE);
        } catch (Exception e) {
            StackTraceWriter sw = new StackTraceWriter();
            e.printStackTrace(new PrintWriter(sw));
            String message = "// Error during bytecode retrieval" + "\n" + sw;
            return new DecompileResult(className, 0, message, new Token[0], DecompileResult.Language.BYTECODE);
        }
    }

    private void checkVersionDownloaded() {
        if (!jar.exists() || libraries.stream().anyMatch(lib -> !lib.exists()))
            throw new IllegalStateException("Version not downloaded!");
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
