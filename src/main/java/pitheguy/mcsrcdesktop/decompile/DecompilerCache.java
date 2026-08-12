package pitheguy.mcsrcdesktop.decompile;

import java.util.HashMap;
import java.util.Map;

public class DecompilerCache {
    private static final Map<CacheKey, DecompileResult> cache = new HashMap<>();

    private DecompilerCache() {
    }

    public static boolean contains(String className, String version, DecompilerOptions options) {
        return cache.containsKey(new CacheKey(className, version, options));
    }

    public static DecompileResult get(String className, String version, DecompilerOptions options) {
        return cache.get(new CacheKey(className, version, options));
    }

    public static void put(String className, String version, DecompilerOptions options, DecompileResult result) {
        cache.put(new CacheKey(className, version, options), result);
    }

    private record CacheKey(String className, String version, DecompilerOptions options) {}
}
