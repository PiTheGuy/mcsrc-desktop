package pitheguy.mcsrcdesktop.decompile;

import org.jetbrains.java.decompiler.main.extern.IResultSaver;

import java.util.HashMap;
import java.util.Map;
import java.util.jar.Manifest;

public class ResultSaverImpl implements IResultSaver {
    public static final ResultSaverImpl INSTANCE = new ResultSaverImpl();

    @Override
    public void saveFolder(String path) {
    }

    @Override
    public void copyFile(String source, String path, String entryName) {
    }

    @Override
    public void saveClassFile(String path, String qualifiedName, String entryName, String content, int[] mapping) {
    }

    @Override
    public void createArchive(String path, String archiveName, Manifest manifest) {
    }

    @Override
    public void saveDirEntry(String path, String archiveName, String entryName) {
    }

    @Override
    public void copyEntry(String source, String path, String archiveName, String entry) {
    }

    @Override
    public void saveClassEntry(String path, String archiveName, String qualifiedName, String entryName, String content) {
    }

    @Override
    public void closeArchive(String path, String archiveName) {
    }
}
