package pitheguy.mcsrcdesktop;

import me.friwi.jcefmaven.CefAppBuilder;
import me.friwi.jcefmaven.MavenCefAppHandlerAdapter;
import org.cef.CefApp;
import org.cef.CefClient;
import org.cef.browser.CefBrowser;
import org.cef.browser.CefMessageRouter;
import pitheguy.mcsrcdesktop.cef.EventHandler;
import pitheguy.mcsrcdesktop.cef.MainFrame;
import pitheguy.mcsrcdesktop.download.MinecraftDownloader;
import pitheguy.mcsrcdesktop.util.Util;

public class Main {
    void main() throws Exception {
        CefAppBuilder builder = new CefAppBuilder();
        builder.getCefSettings().windowless_rendering_enabled = false;
        builder.setAppHandler(new MavenCefAppHandlerAdapter() {
            @Override
            public void stateHasChanged(CefApp.CefAppState state) {
                if (state == CefApp.CefAppState.TERMINATED) {
                    System.exit(0);
                }
            }
        });
        CefApp app = builder.build();
        CefClient client = app.createClient();
        CefBrowser browser = client.createBrowser("http://localhost:5173", false, false);
        CefMessageRouter messageRouter = CefMessageRouter.create();
        MinecraftDownloader downloader = new MinecraftDownloader(Util.getAppDataDir());
        messageRouter.addHandler(new EventHandler(downloader), true);
        client.addMessageRouter(messageRouter);
        new MainFrame(browser);
    }
}