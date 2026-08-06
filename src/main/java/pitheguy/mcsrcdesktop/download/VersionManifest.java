package pitheguy.mcsrcdesktop.download;

import com.google.gson.annotations.SerializedName;

import java.time.Instant;

public record VersionManifest(Latest latest, VersionEntry[] versions) {

    public VersionEntry findVersionEntry(String id) {
        for (VersionEntry entry : versions) {
            if (entry.id().equals(id)) {
                return entry;
            }
        }
        return null;
    }

    public record Latest(
            @SerializedName("release")
            String release,
            @SerializedName("snapshot")
            String snapshot
    ) {}

    public record VersionEntry(
            @SerializedName("id")
            String id,
            @SerializedName("type")
            VersionType type,
            @SerializedName("url")
            String url,
            @SerializedName("time")
            Instant time,
            @SerializedName("releaseTime")
            Instant releaseTime,
            @SerializedName("sha1")
            String sha1
    ) {}

}
