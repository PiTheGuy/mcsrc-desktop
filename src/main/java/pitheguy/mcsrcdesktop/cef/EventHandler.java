package pitheguy.mcsrcdesktop.cef;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.cef.browser.CefBrowser;
import org.cef.browser.CefFrame;
import org.cef.callback.CefQueryCallback;
import org.cef.handler.CefMessageRouterHandlerAdapter;
import pitheguy.mcsrcdesktop.cef.query.CefQuery;
import pitheguy.mcsrcdesktop.cef.query.CefQueryContext;
import pitheguy.mcsrcdesktop.download.MinecraftDownloader;
import pitheguy.mcsrcdesktop.index.MinecraftIndexer;

import java.io.PrintWriter;
import java.io.StringWriter;

public class EventHandler extends CefMessageRouterHandlerAdapter implements CefQueryContext {
    private static final Logger LOGGER = LogManager.getLogger(EventHandler.class);

    private final MinecraftDownloader downloader;
    private MinecraftIndexer indexer;


    public EventHandler(MinecraftDownloader downloader) {
        this.downloader = downloader;
    }

    @Override
    public boolean onQuery(CefBrowser browser, CefFrame frame, long queryId, String request, boolean persistent, CefQueryCallback callback) {
        LOGGER.debug("Received query: {}", request);
        JsonObject json = JsonParser.parseString(request).getAsJsonObject();
        CefQuery query = CefQuery.deserialize(json);
        if (query == null) return false;
        try {
            query.handle(callback, this);
        } catch (Exception e) {
            LOGGER.error("Failed to handle query {}", query, e);
            StringWriter sw = new StringWriter();
            e.printStackTrace(new PrintWriter(sw));
            callback.failure(-2, "Failed to handle query " + query + "\n" + sw);
        }
        return true;
    }

    @Override
    public MinecraftDownloader downloader() {
        return downloader;
    }

    @Override
    public MinecraftIndexer indexer() {
        return indexer;
    }

    @Override
    public void setIndexer(MinecraftIndexer indexer) {
        this.indexer = indexer;
    }
}
