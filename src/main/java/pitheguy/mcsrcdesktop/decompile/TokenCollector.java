package pitheguy.mcsrcdesktop.decompile;

import org.jetbrains.java.decompiler.main.extern.TextTokenVisitor;
import org.jetbrains.java.decompiler.struct.gen.FieldDescriptor;
import org.jetbrains.java.decompiler.struct.gen.MethodDescriptor;
import org.jetbrains.java.decompiler.util.token.TextRange;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class TokenCollector extends TextTokenVisitor {
    private static final Pattern IMPORT_REGEX = Pattern.compile("^\\s*import\\s+(?!static\\b)([^\\s;]+)\\s*;", Pattern.MULTILINE);

    private String currentContent = null;
    private final List<Token> currentTokens = new ArrayList<>();
    private final Map<String, List<Token>> tokens;

    public TokenCollector(TextTokenVisitor next, Map<String, List<Token>> tokens) {
        super(next);
        this.tokens = tokens;
    }

    private void addImportTokens() {
        Matcher matcher = IMPORT_REGEX.matcher(currentContent);
        while (matcher.find()) {
            String importPath = matcher.group(1).replace(".","/");
            if (importPath.endsWith("*")) {
                continue;
            }
            String simpleName = importPath.substring(importPath.lastIndexOf('/') + 1);
            int start = matcher.start() + matcher.group().lastIndexOf(simpleName);
            int length = importPath.length() - importPath.lastIndexOf(simpleName);
            currentTokens.add(new Token.NonMethodToken(start, length, importPath, false, Token.TokenType.CLASS));
        }
    }

    @Override
    public void start(String content) {
        super.start(content);
        currentContent = content;
        currentTokens.clear();
        addImportTokens();
    }

    @Override
    public void visitClass(TextRange range, boolean declaration, String name) {
        super.visitClass(range, declaration, name);
        currentTokens.add(new Token.NonMethodToken(range.start, range.length, name, declaration, Token.TokenType.CLASS));
    }

    @Override
    public void visitField(TextRange range, boolean declaration, String className, String name, FieldDescriptor descriptor) {
        super.visitField(range, declaration, className, name, descriptor);
        currentTokens.add(new Token.MemberToken(range.start, range.length, className, declaration, Token.TokenType.FIELD, name, descriptor.descriptorString));
    }

    @Override
    public void visitMethod(TextRange range, boolean declaration, String className, String name, MethodDescriptor descriptor) {
        super.visitMethod(range, declaration, className, name, descriptor);
        currentTokens.add(new Token.MemberToken(range.start, range.length, className, declaration, Token.TokenType.METHOD, name, descriptor.toString()));

    }

    @Override
    public void visitParameter(TextRange range, boolean declaration, String className, String methodName, MethodDescriptor methodDescriptor, int index, String name) {
        super.visitParameter(range, declaration, className, methodName, methodDescriptor, index, name);
        currentTokens.add(new Token.NonMethodToken(range.start, range.length, name, declaration, Token.TokenType.PARAMETER));

    }

    @Override
    public void visitLocal(TextRange range, boolean declaration, String className, String methodName, MethodDescriptor methodDescriptor, int index, String name) {
        super.visitLocal(range, declaration, className, methodName, methodDescriptor, index, name);
        currentTokens.add(new Token.NonMethodToken(range.start, range.length, name, declaration, Token.TokenType.LOCAL));
    }

    @Override
    public void end() {
        super.end();
        tokens.put(currentContent, List.copyOf(currentTokens));
        currentTokens.clear();
        currentContent = null;
    }

}
