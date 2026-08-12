package pitheguy.mcsrcdesktop;

import io.github.trethore.jcefgithub.CefAppBuilder;
import io.github.trethore.jcefgithub.MavenCefAppHandlerAdapter;
import joptsimple.OptionParser;
import joptsimple.OptionSet;
import joptsimple.OptionSpec;
import org.cef.CefApp;
import org.cef.CefClient;
import org.cef.browser.CefBrowser;
import org.cef.browser.CefMessageRouter;
import pitheguy.mcsrcdesktop.cef.EventHandler;
import pitheguy.mcsrcdesktop.cef.MainFrame;
import pitheguy.mcsrcdesktop.download.MinecraftDownloader;
import pitheguy.mcsrcdesktop.util.Util;

public class Main {
    public static final String PRODUCTION_URL = "https://mcsrc.pitheguy.workers.dev/";
    public static final String DEV_URL = "http://localhost:5173";

    void main(String[] args) throws Exception {
        OptionParser parser = new OptionParser();
        parser.accepts("help", "Show help").forHelp();
        OptionSpec<Void> devOpt = parser.accepts("dev", "Run in development mode");
        OptionSet options = parser.parse(args);
        boolean devMode = options.has(devOpt);

        CefAppBuilder builder = new CefAppBuilder();
        builder.setInstallDir(Util.getAppDataDir().resolve("jcef-bundle").toFile());
        builder.getCefSettings().windowless_rendering_enabled = false;
        builder.getCefSettings().cache_path = Util.getAppDataDir().resolve("cache").toString();
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
        CefBrowser browser = client.createBrowser(devMode ? DEV_URL : PRODUCTION_URL, false, false);
        CefMessageRouter messageRouter = CefMessageRouter.create();
        MinecraftDownloader downloader = new MinecraftDownloader(Util.getAppDataDir());
        messageRouter.addHandler(new EventHandler(downloader), true);
        client.addMessageRouter(messageRouter);
        new MainFrame(browser, devMode);
    }
}