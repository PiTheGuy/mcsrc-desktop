package pitheguy.mcsrcdesktop.cef.query;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.cef.callback.CefQueryCallback;

import java.awt.*;
import java.net.URI;

public record UpdateQuery() implements CefQuery {
    public static final CefQueryType<UpdateQuery> TYPE = CefQueryType.of(UpdateQuery.class);
    private static final Logger LOGGER = LogManager.getLogger(UpdateQuery.class);
    private static final String UPDATE_URL = "https://github.com/PiTheGuy/mcsrc-desktop/releases/latest";

    @Override
    public CefQueryType<? extends CefQuery> type() {
        return TYPE;
    }

    @Override
    public void handle(CefQueryCallback callback, CefQueryContext context) {
        try {
            //TODO update automatically?
            Desktop.getDesktop().browse(URI.create(UPDATE_URL));
            callback.success("{}");
        } catch (Exception e) {
            LOGGER.error("Update failed", e);
            callback.failure(-2, "Update failed: " + e.getMessage());
        }
    }
}
