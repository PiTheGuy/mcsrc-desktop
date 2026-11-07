package pitheguy.mcsrcdesktop.util;

import org.lwjgl.PointerBuffer;
import org.lwjgl.system.MemoryStack;

import java.nio.file.Path;
import java.util.Optional;

import static org.lwjgl.util.nfd.NativeFileDialog.*;

public class NativeFileDialogs {
    static { NFD_Init(); }  // once per app

    public static Optional<Path> pickFolder(Path start) {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            PointerBuffer out = stack.mallocPointer(1);
            int result = NFD_PickFolder(out, start == null ? null : start.toString());
            if (result != NFD_OKAY) return Optional.empty();
            String path = out.getStringUTF8(0);
            NFD_FreePath(out.get(0));
            return Optional.of(Path.of(path));
        }
    }

    public static Optional<Path> openFile(Path start) {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            PointerBuffer out = stack.mallocPointer(1);
            int result = NFD_OpenDialog(out, null, start == null ? null : start.toString());
            if (result != NFD_OKAY) return Optional.empty();
            String path = out.getStringUTF8(0);
            NFD_FreePath(out.get(0));
            return Optional.of(Path.of(path));
        }
    }
}
