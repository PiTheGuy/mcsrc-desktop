package pitheguy.mcsrcdesktop.decompile;

import org.jetbrains.java.decompiler.api.Decompiler;
import org.jetbrains.java.decompiler.main.extern.TextTokenVisitor;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MinecraftDecompiler {
    public static DecompileResult decompile(File input, List<File> libraries, String className) {
        List<File> allLibraries = new ArrayList<>(libraries);
        allLibraries.add(input);
        Map<String, String> output = new HashMap<>();
        ClassSource classSource = new ClassSource(input, className, output);
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
    }
}
