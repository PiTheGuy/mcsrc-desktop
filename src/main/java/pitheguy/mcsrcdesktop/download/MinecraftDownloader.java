package pitheguy.mcsrcdesktop.download;

import com.google.common.hash.Hashing;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import pitheguy.mcsrcdesktop.util.ExtraTypeAdapters;
import pitheguy.mcsrcdesktop.util.ProgressListener;

import java.io.*;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

public class MinecraftDownloader {
    private static final Logger LOGGER = LogManager.getLogger();
    public static final String MANIFEST_URL = "https://piston-meta.mojang.com/mc/game/version_manifest_v2.json";
    public static final Gson GSON = new GsonBuilder()
            .registerTypeAdapter(Instant.class, ExtraTypeAdapters.INSTANT)
            .create();
    public static final HttpClient HTTP_CLIENT = HttpClient.newHttpClient();
    private final Path dataDir;

    private VersionManifest manifest;
    private final Map<String, VersionInfo> versions = new HashMap<>();

    public MinecraftDownloader(Path dataDir) {
        this.dataDir = dataDir;
    }

    public VersionManifest fetchVersionManifest() throws IOException, InterruptedException {
        if (manifest != null) return manifest;
        HttpRequest request = HttpRequest.newBuilder().uri(URI.create(MANIFEST_URL)).build();
        var response = HTTP_CLIENT.send(request, HttpResponse.BodyHandlers.ofString());
        return manifest = GSON.fromJson(response.body(), VersionManifest.class);
    }

    public VersionInfo fetchVersionInfo(String version) throws IOException, InterruptedException {
        if (versions.containsKey(version)) return versions.get(version);
        VersionManifest manifest = fetchVersionManifest();
        VersionManifest.VersionEntry entry = manifest.findVersionEntry(version);
        if (entry == null) throw new IllegalArgumentException("Unknown version: " + version);
        HttpRequest request = HttpRequest.newBuilder().uri(URI.create(entry.url())).build();
        var response = HTTP_CLIENT.send(request, HttpResponse.BodyHandlers.ofString());
        VersionInfo versionInfo = GSON.fromJson(response.body(), VersionInfo.class);
        versions.put(version, versionInfo);
        return versionInfo;
    }

    public File fetchJar(VersionInfo versionInfo) {
        String fileName = versionInfo.id() + ".jar";
        Path path = dataDir.resolve("versions").resolve(fileName);
        return path.toFile();
    }

    public File fetchRemappedJar(VersionInfo versionInfo) {
        boolean remap = versionInfo.downloads().clientMappings() != null;
        String fileName = remap ? versionInfo.id() + "-remapped.jar" : versionInfo.id() + ".jar";
        Path path = dataDir.resolve("versions").resolve(fileName);
        return path.toFile();
    }

    public File fetchMappings(VersionInfo versionInfo) {
        if (versionInfo.downloads().clientMappings() == null) return null;
        String fileName = versionInfo.id() + ".txt";
        Path path = dataDir.resolve("mappings").resolve(fileName);
        return path.toFile();
    }

    public List<File> fetchLibraries(VersionInfo versionInfo) {
        Path librariesDir = dataDir.resolve("libraries");
        List<File> libraries = new ArrayList<>();
        for (VersionInfo.Library library : versionInfo.libraries()) {
            if (!library.matchesRules()) continue;
            File libraryFile = librariesDir.resolve(library.downloads().artifact().path()).toFile();
            libraries.add(libraryFile);
        }
        return libraries;
    }

    public CompletableFuture<DownloadInfo> downloadVersion(VersionInfo versionInfo, ProgressListener progressListener) throws IOException {
        DownloadTracker tracker = new DownloadTracker();
        Path jarPath = fetchJar(versionInfo).toPath();
        var jarDownload = versionInfo.downloads().client();
        var jarFuture = downloadFile(jarDownload.url(), jarPath, jarDownload.sha1(), jarDownload.size());
        tracker.addDownload(jarFuture, jarPath, jarDownload.size());
        CompletableFuture<InputStream> mappingsFuture;
        if (versionInfo.downloads().clientMappings() != null) {
            Path mappingsPath = fetchMappings(versionInfo).toPath();
            var mappingsDownload = versionInfo.downloads().clientMappings();
            mappingsFuture = downloadFile(mappingsDownload.url(), mappingsPath, mappingsDownload.sha1(), mappingsDownload.size());
            tracker.addDownload(mappingsFuture, mappingsPath, mappingsDownload.size());
        } else {
            mappingsFuture = CompletableFuture.completedFuture(null);
        }
        List<CompletableFuture<InputStream>> libraryFutures = new ArrayList<>();
        for (VersionInfo.Library library : versionInfo.libraries()) {
            if (!library.matchesRules()) continue;
            var libraryDownload = library.downloads().artifact();
            Path libraryPath = dataDir.resolve("libraries").resolve(libraryDownload.path());
            var libraryFuture = downloadFile(libraryDownload.url(), libraryPath, libraryDownload.sha1(), libraryDownload.size());
            tracker.addDownload(libraryFuture, libraryPath, libraryDownload.size());
            libraryFutures.add(libraryFuture);
        }
        var librariesFuture = CompletableFuture.allOf(libraryFutures.toArray(CompletableFuture[]::new))
                .thenApply(_ -> libraryFutures.stream().map(CompletableFuture::join).toList());
        tracker.start(progressListener);
        return CompletableFuture.allOf(jarFuture, mappingsFuture, librariesFuture)
                .thenApply(_ -> new DownloadInfo(fetchJar(versionInfo), fetchMappings(versionInfo), fetchLibraries(versionInfo)));
    }

    private static CompletableFuture<InputStream> downloadFile(String url, Path path, String sha1, int size) throws IOException {
        if (Files.exists(path)) {
            String fileHash = Hashing.sha1().hashBytes(Files.readAllBytes(path)).toString();
            if (Files.size(path) == size && fileHash.equals(sha1)) {
                return CompletableFuture.completedFuture(null);
            } else {
                LOGGER.warn("File {} already exists, but hash does not match. Deleting.", path.getFileName());
                Files.delete(path);
            }
        }
        Files.createDirectories(path.getParent());
        LOGGER.info("Downloading {} ({} bytes)", path.getFileName(), size);
        HttpRequest request = HttpRequest.newBuilder().uri(URI.create(url)).build();
        return HTTP_CLIENT.sendAsync(request, HttpResponse.BodyHandlers.ofInputStream())
                .thenApply(HttpResponse::body);

    }

}
