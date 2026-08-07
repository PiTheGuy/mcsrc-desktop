package pitheguy.mcsrcdesktop.download;

import com.google.common.hash.Hashing;
import com.google.gson.*;
import pitheguy.mcsrcdesktop.util.ExtraTypeAdapters;
import pitheguy.mcsrcdesktop.util.OS;

import java.io.*;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public class MinecraftDownloader {
    public static final String MANIFEST_URL = "https://piston-meta.mojang.com/mc/game/version_manifest_v2.json";
    public static final Gson GSON = new GsonBuilder()
            .registerTypeAdapter(Instant.class, new ExtraTypeAdapters.InstantAdapter())
            .create();
    public static final HttpClient HTTP_CLIENT = HttpClient.newHttpClient();
    private final Path dataDir;

    public MinecraftDownloader(Path dataDir) {
        this.dataDir = dataDir;
    }

    public VersionManifest fetchVersionManifest() throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder().uri(URI.create("https://piston-meta.mojang.com/mc/game/version_manifest_v2.json")).build();
        var response = HTTP_CLIENT.send(request, HttpResponse.BodyHandlers.ofString());
        return GSON.fromJson(response.body(), VersionManifest.class);
    }

    public VersionInfo fetchVersionInfo(VersionManifest manifest, String version) throws IOException, InterruptedException {
        VersionManifest.VersionEntry entry = manifest.findVersionEntry(version);
        if (entry == null) throw new IllegalArgumentException("Unknown version: " + version);
        HttpRequest request = HttpRequest.newBuilder().uri(URI.create(entry.url())).build();
        var response = HTTP_CLIENT.send(request, HttpResponse.BodyHandlers.ofString());
        return GSON.fromJson(response.body(), VersionInfo.class);
    }

    public CompletableFuture<File> fetchJar(VersionInfo versionInfo, ProgressListener progressListener) throws IOException {
        String fileName = versionInfo.id() + ".jar";
        Path path = dataDir.resolve("versions").resolve(fileName);
        Files.createDirectories(path.getParent());
        if (Files.exists(path)) {
            String fileHash = Hashing.sha1().hashBytes(Files.readAllBytes(path)).toString();
            if (fileHash.equals(versionInfo.downloads().client().sha1())) {
                return CompletableFuture.completedFuture(path.toFile());
            } else {
                System.out.println("Deleting old version file; hash didn't match"); //TODO proper logging
                Files.delete(path);
            }
        }
        String url = versionInfo.downloads().client().url();
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

    //TODO add support for mappings

    public CompletableFuture<List<File>> fetchLibraries(VersionInfo versionInfo) throws IOException {
        Path librariesDir = dataDir.resolve("libraries");
        List<CompletableFuture<File>> pathFutures = new ArrayList<>();
        for (VersionInfo.Library library : versionInfo.libraries()) {
            if (library.matchesRules()) {
                Path path = librariesDir.resolve(library.downloads().artifact().path());
                if (Files.exists(path)) {
                    String fileHash = Hashing.sha1().hashBytes(Files.readAllBytes(path)).toString();
                    if (Files.size(path) == library.downloads().artifact().size() && fileHash.equals(library.downloads().artifact().sha1())) {
                        pathFutures.add(CompletableFuture.completedFuture(path.toFile()));
                        continue;
                    } else {
                        Files.delete(path);
                    }
                }
                Files.createDirectories(path.getParent());
                System.out.println("Downloading " + path.getFileName() + " (" + library.downloads().artifact().size() + " bytes)");
                HttpRequest request = HttpRequest.newBuilder().uri(URI.create(library.downloads().artifact().url())).build();
                CompletableFuture<File> future = HTTP_CLIENT.sendAsync(request, HttpResponse.BodyHandlers.ofFile(path))
                        .thenApply(response -> response.body().toFile());
                pathFutures.add(future);
            }
        }
        return CompletableFuture.allOf(pathFutures.toArray(CompletableFuture[]::new))
                .thenApply(_ -> pathFutures.stream().map(CompletableFuture::join).toList());
    }

    public interface ProgressListener {
        void update(double progress);
    }
}
