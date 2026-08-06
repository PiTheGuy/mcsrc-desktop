package pitheguy.mcsrcdesktop.cef;

import java.io.File;
import java.util.List;

public record DownloadInfo(File mainJar, List<File> libraries) {
}
