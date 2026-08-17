package pitheguy.mcsrcdesktop.download;

import org.jspecify.annotations.Nullable;

import java.io.File;
import java.util.List;

public record DownloadInfo(File jar, @Nullable File mappings, List<File> libraries) {
}
