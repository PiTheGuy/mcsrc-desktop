package pitheguy.mcsrcdesktop.download;

import com.google.gson.annotations.SerializedName;

import java.time.Instant;

public record VersionInfo(
        @SerializedName("assetIndex")
        AssetIndex assetIndex,
        @SerializedName("assets")
        String assets,
        @SerializedName("downloads")
        VersionDownloads downloads,
        @SerializedName("id")
        String id,
        @SerializedName("libraries")
        Library[] libraries,
        @SerializedName("releaseTime")
        Instant releaseTime,
        @SerializedName("time")
        Instant time,
        @SerializedName("type")
        VersionType type
) {

    public record Rule(
            @SerializedName("action")
            Action action,
            @SerializedName("os")
            OS os
    ) {
        public enum Action {
            @SerializedName("allow")
            ALLOW,
            @SerializedName("disallow")
            DISALLOW
        }
        public record OS(
                @SerializedName("name")
                String name
        ) {
            public pitheguy.mcsrcdesktop.util.OS getOS() {
                return switch (name) {
                    case "osx" -> pitheguy.mcsrcdesktop.util.OS.OSX;
                    case "windows" -> pitheguy.mcsrcdesktop.util.OS.WINDOWS;
                    case "linux" -> pitheguy.mcsrcdesktop.util.OS.LINUX;
                    default -> pitheguy.mcsrcdesktop.util.OS.OTHER;
                };
            }
        }

        public boolean matches() {
            if (os == null) return true;
            return switch (action) {
                case ALLOW -> pitheguy.mcsrcdesktop.util.OS.get() == os.getOS();
                case DISALLOW -> pitheguy.mcsrcdesktop.util.OS.get() != os.getOS();
            };
        }
    }

    public record AssetIndex(
            @SerializedName("id")
            String id,
            @SerializedName("sha1")
            String sha1,
            @SerializedName("size")
            int size,
            @SerializedName("totalSize")
            int totalSize,
            @SerializedName("url")
            String url
    ) {}

    public record VersionDownloads(
            @SerializedName("client")
            Download client,
            @SerializedName("client_mappings")
            Download clientMappings,
            @SerializedName("server")
            Download server,
            @SerializedName("server_mappings")
            Download serverMappings
    ) {
        public record Download(
                @SerializedName("sha1")
                String sha1,
                @SerializedName("size")
                int size,
                @SerializedName("url")
                String url
        ) {}
    }

    public record Library(
            @SerializedName("name")
            String name,
            @SerializedName("downloads")
            LibraryDownloads downloads,
            @SerializedName("rules")
            Rule[] rules
    ) {
        public record LibraryDownloads(
                @SerializedName("artifact")
                Artifact artifact
        ) {
            public record Artifact(
                    @SerializedName("path")
                    String path,
                    @SerializedName("sha1")
                    String sha1,
                    @SerializedName("size")
                    int size,
                    @SerializedName("url")
                    String url
            ) {}
        }

        public boolean matchesRules() {
            if (rules == null) return true;
            for (Rule rule : rules) {
                if (rule.matches()) return true;
            }
            return false;
        }
    }
}
