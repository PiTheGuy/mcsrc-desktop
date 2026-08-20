package pitheguy.mcsrcdesktop.download;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import pitheguy.mcsrcdesktop.util.ExtraTypeAdapters;
import pitheguy.mcsrcdesktop.util.ProgressListener;

import java.io.*;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

public class MinecraftDownloader {
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
        var jarDownloadInfo = versionInfo.downloads().client();
        FileDownload jarDownload = new FileDownload(jarDownloadInfo.url(), jarPath, jarDownloadInfo.sha1(), jarDownloadInfo.size());
        jarDownload.fetch(HTTP_CLIENT);
        tracker.addDownload(jarDownload);
        if (versionInfo.downloads().clientMappings() != null) {
            Path mappingsPath = fetchMappings(versionInfo).toPath();
            var mappingsDownloadInfo = versionInfo.downloads().clientMappings();
            FileDownload mappingsDownload = new FileDownload(mappingsDownloadInfo.url(), mappingsPath, mappingsDownloadInfo.sha1(), mappingsDownloadInfo.size());
            mappingsDownload.fetch(HTTP_CLIENT);
            tracker.addDownload(mappingsDownload);
        }
        for (VersionInfo.Library library : versionInfo.libraries()) {
            if (!library.matchesRules()) continue;
            var libraryDownloadInfo = library.downloads().artifact();
            Path libraryPath = dataDir.resolve("libraries").resolve(libraryDownloadInfo.path());
            FileDownload libraryDownload = new FileDownload(libraryDownloadInfo.url(), libraryPath, libraryDownloadInfo.sha1(), libraryDownloadInfo.size());
            libraryDownload.fetch(HTTP_CLIENT);
            tracker.addDownload(libraryDownload);
        }
        tracker.start(progressListener);
        return tracker.future().thenApply(_ -> new DownloadInfo(fetchJar(versionInfo), fetchMappings(versionInfo), fetchLibraries(versionInfo)));
    }

}
