package pitheguy.mcsrcdesktop.cef.query;

import pitheguy.mcsrcdesktop.download.MinecraftDownloader;
import pitheguy.mcsrcdesktop.index.MinecraftIndexer;

public interface CefQueryContext {
    MinecraftDownloader downloader();

    MinecraftIndexer indexer();

    void setIndexer(MinecraftIndexer indexer);
}
