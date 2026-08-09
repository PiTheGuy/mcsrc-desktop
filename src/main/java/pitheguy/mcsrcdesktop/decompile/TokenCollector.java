package pitheguy.mcsrcdesktop.decompile;

import org.jetbrains.java.decompiler.main.extern.TextTokenVisitor;
import org.jetbrains.java.decompiler.struct.gen.FieldDescriptor;
import org.jetbrains.java.decompiler.struct.gen.MethodDescriptor;
import org.jetbrains.java.decompiler.util.token.TextRange;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class TokenCollector extends TextTokenVisitor {
    private String currentContent = null;
    private final List<Token> currentTokens = new ArrayList<>();
    private final Map<String, List<Token>> tokens;

    public TokenCollector(TextTokenVisitor next, Map<String, List<Token>> tokens) {
        super(next);
        this.tokens = tokens;
    }

    @Override
    public void start(String content) {
        super.start(content);
        currentContent = content;
        currentTokens.clear();
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
