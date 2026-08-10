package pitheguy.mcsrcdesktop.decompile;

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

public class MinecraftDecompiler {
    private final File jar;
    private final List<File> libraries;

    private MinecraftDecompiler(File jar, List<File> libraries) {
        this.jar = jar;
        this.libraries = libraries;
    }

    public static MinecraftDecompiler create(MinecraftDownloader downloader, String version) throws IOException, InterruptedException {
        VersionManifest manifest = downloader.fetchVersionManifest();
        VersionInfo versionInfo = downloader.fetchVersionInfo(manifest, version);
        File jar = downloader.fetchJar(versionInfo);
        List<File> libraries = downloader.fetchLibraries(versionInfo);
        return new MinecraftDecompiler(jar, libraries);
    }

    public DecompileResult decompile(String className) {
        try {
            if (!jar.exists() || libraries.stream().anyMatch(lib -> !lib.exists()))
                throw new IllegalStateException("Version not downloaded!");
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
            return new DecompileResult(className, classSource.crc32(), source, classTokens, DecompileResult.Language.JAVA);
        } catch (Exception e) {
            StackTraceWriter sw = new StackTraceWriter();
            e.printStackTrace(new PrintWriter(sw));

            String message = "// Error during decompilation" + "\n" + sw;
            return new DecompileResult(className, 0, message, new Token[0], DecompileResult.Language.JAVA);
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
