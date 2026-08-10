package pitheguy.mcsrcdesktop.decompile;

import java.util.HashMap;
import java.util.Map;

public class DecompilerCache {
    private static final Map<CacheKey, DecompileResult> cache = new HashMap<>();

    private DecompilerCache() {
    }

    public static boolean contains(String className, String version) {
        return cache.containsKey(new CacheKey(className, version));
    }

    public static DecompileResult get(String className, String version) {
        return cache.get(new CacheKey(className, version));
    }

    public static void put(String className, String version, DecompileResult result) {
        cache.put(new CacheKey(className, version), result);
    }

    private record CacheKey(String className, String version) {}
}
