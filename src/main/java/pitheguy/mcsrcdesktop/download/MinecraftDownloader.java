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

    public CompletableFuture<File> downloadJar(VersionInfo versionInfo, ProgressListener progressListener) throws IOException {
        String fileName = versionInfo.id() + ".jar";
        Path path = dataDir.resolve("versions").resolve(fileName);
        Files.createDirectories(path.getParent());
        var download = versionInfo.downloads().client();
        if (checkFileExists(path, download.sha1(), download.size())) {
            return CompletableFuture.completedFuture(path.toFile());
        }
        String url = download.url();
        LOGGER.info("Downloading {} ({} bytes)", fileName, download.size());
        HttpRequest request = HttpRequest.newBuilder().uri(URI.create(url)).build();


        return HTTP_CLIENT.sendAsync(request, HttpResponse.BodyHandlers.ofInputStream())
                .thenApply(response -> {
                    long contentLength = response.headers()
                            .firstValueAsLong("Content-Length")
                            .orElse(-1L);

                    try (InputStream in = response.body();
                         OutputStream out = Files.newOutputStream(path)) {

                        byte[] buffer = new byte[8192];
                        long totalRead = 0;
                        int read;

                        while ((read = in.read(buffer)) != -1) {
                            out.write(buffer, 0, read);
                            totalRead += read;

                            if (progressListener != null && contentLength > 0) {
                                progressListener.update((double) totalRead / contentLength);
                            }
                        }
                    } catch (IOException e) {
                        throw new UncheckedIOException(e);
                    }

                    return path.toFile();
                });
    }

    public File fetchMappings(VersionInfo versionInfo) {
        if (versionInfo.downloads().clientMappings() == null) return null;
        String fileName = versionInfo.id() + ".txt";
        Path path = dataDir.resolve("mappings").resolve(fileName);
        return path.toFile();
    }

    public CompletableFuture<File> downloadMappings(VersionInfo versionInfo) throws IOException {
        if (versionInfo.downloads().clientMappings() == null) return CompletableFuture.completedFuture(null);
        String fileName = versionInfo.id() + ".txt";
        Path path = dataDir.resolve("mappings").resolve(fileName);
        VersionInfo.VersionDownloads.Download download = versionInfo.downloads().clientMappings();
        return downloadFile(download.url(), path, download.sha1(), download.size());
    }

    //TODO merge library downloads with jar downloads
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

    public CompletableFuture<List<File>> downloadLibraries(VersionInfo versionInfo) throws IOException {
        Path librariesDir = dataDir.resolve("libraries");
        List<CompletableFuture<File>> pathFutures = new ArrayList<>();
        for (VersionInfo.Library library : versionInfo.libraries()) {
            if (library.matchesRules()) {
                Path path = librariesDir.resolve(library.downloads().artifact().path());
                var artifact = library.downloads().artifact();
                var future = downloadFile(artifact.url(), path, artifact.sha1(), artifact.size());
                pathFutures.add(future);
            }
        }
        return CompletableFuture.allOf(pathFutures.toArray(CompletableFuture[]::new))
                .thenApply(_ -> pathFutures.stream().map(CompletableFuture::join).toList());
    }

    private static boolean checkFileExists(Path path, String sha1, int size) throws IOException {
        if (Files.exists(path)) {
            String fileHash = Hashing.sha1().hashBytes(Files.readAllBytes(path)).toString();
            if (Files.size(path) == size && fileHash.equals(sha1)) {
                return true;
            } else {
                Files.delete(path);
            }
        }
        return false;
    }

    private static CompletableFuture<File> downloadFile(String url, Path path, String sha1, int size) throws IOException {
        if (checkFileExists(path, sha1, size)) {
            return CompletableFuture.completedFuture(path.toFile());
        }
        Files.createDirectories(path.getParent());
        LOGGER.info("Downloading {} ({} bytes)", path.getFileName(), size);
        HttpRequest request = HttpRequest.newBuilder().uri(URI.create(url)).build();
        return HTTP_CLIENT.sendAsync(request, HttpResponse.BodyHandlers.ofFile(path))
                .thenApply(response -> response.body().toFile());
    }

}
