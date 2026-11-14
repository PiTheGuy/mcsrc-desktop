package pitheguy.mcsrcdesktop.cef.query;

import org.cef.callback.CefQueryCallback;

import java.awt.*;
import java.net.URI;

import static pitheguy.mcsrcdesktop.util.SharedConstants.GSON;

public record UpdateQuery() implements CefQuery {
    public static final CefQueryType<UpdateQuery> TYPE = CefQueryType.of(UpdateQuery.class);
    private static final String UPDATE_URL = "https://github.com/PiTheGuy/mcsrc-desktop/releases/latest";

    @Override
    public CefQueryType<? extends CefQuery> type() {
        return TYPE;
    }

    @Override
    public void handle(CefQueryCallback callback, CefQueryContext context) throws Exception {
        //TODO update automatically?
        Desktop.getDesktop().browse(URI.create(UPDATE_URL));
        callback.success(GSON.toJson(null));
    }
}
