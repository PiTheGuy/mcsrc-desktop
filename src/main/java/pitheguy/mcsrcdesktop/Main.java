package pitheguy.mcsrcdesktop;

import io.github.trethore.jcefgithub.CefAppBuilder;
import io.github.trethore.jcefgithub.MavenCefAppHandlerAdapter;
import joptsimple.OptionParser;
import joptsimple.OptionSet;
import joptsimple.OptionSpec;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.core.config.Configurator;
import org.cef.*;
import org.cef.browser.CefBrowser;
import org.cef.browser.CefMessageRouter;
import pitheguy.mcsrcdesktop.cef.Browser;
import pitheguy.mcsrcdesktop.cef.EventHandler;
import pitheguy.mcsrcdesktop.cef.MainFrame;
import pitheguy.mcsrcdesktop.download.MinecraftDownloader;
import pitheguy.mcsrcdesktop.util.Util;

import javax.swing.*;
import java.awt.*;
import java.util.function.Function;

public class Main {
    public static final String PRODUCTION_URL = "https://mcsrc.pitheguy.workers.dev/";
    public static final String DEV_URL = "http://localhost:5173";

    void main(String[] args) throws Exception {
        setupLogging();
        OptionParser parser = new OptionParser();
        parser.accepts("help", "Show help").forHelp();
        OptionSpec<Void> devOpt = parser.accepts("dev", "Run in development mode");
        OptionSpec<Void> windowlessOpt = parser.accepts("windowless", "Use windowless rendering");
        OptionSpec<Void> verboseOpt = parser.accepts("verbose", "Enable verbose logging");
        OptionSpec<Void> cefLogOpt = parser.accepts("cef-log", "Enable CEF logging");
        OptionSet options = parser.parse(args);
        if (options.has("help")) {
            parser.printHelpOn(System.out);
            return;
        }
        boolean devMode = options.has(devOpt);
        boolean useWindowlessRendering = options.has(windowlessOpt) || OS.isLinux(); // Use windowless rendering on Linux to avoid windowing issues
        if (options.has(verboseOpt)) {
            Configurator.setRootLevel(Level.DEBUG);
        }
        CefAppBuilder builder = new CefAppBuilder();
        builder.setInstallDir(Util.getAppDataDir().resolve("jcef-bundle").toFile());
        builder.getCefSettings().windowless_rendering_enabled = useWindowlessRendering;
        builder.getCefSettings().cache_path = Util.getAppDataDir().resolve("cache").toString();
        if (options.has(cefLogOpt)) {
            builder.getCefSettings().log_severity = CefSettings.LogSeverity.LOGSEVERITY_VERBOSE;
            builder.getCefSettings().log_file = Util.getAppDataDir().resolve("cef.log").toString();
        }
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
        Function<Dimension, CefBrowser> windowedBrowserFactory = _ -> client.createBrowser(devMode ? DEV_URL : PRODUCTION_URL, false, false);
        Function<Dimension, CefBrowser> windowlessBrowserFactory = canvasSize -> new Browser(client, devMode ? DEV_URL : PRODUCTION_URL, null, createBrowserSettings(), canvasSize);
        CefMessageRouter messageRouter = CefMessageRouter.create();
        MinecraftDownloader downloader = new MinecraftDownloader(Util.getAppDataDir());
        messageRouter.addHandler(new EventHandler(downloader), true);
        client.addMessageRouter(messageRouter);
        new MainFrame(useWindowlessRendering ? windowlessBrowserFactory : windowedBrowserFactory, devMode);
    }

    CefBrowserSettings createBrowserSettings() {
        CefBrowserSettings settings = new CefBrowserSettings();
        settings.windowless_frame_rate = 60;
        return settings;
    }

    void setupLogging() {
        System.setProperty("APP_DATA_FOLDER", Util.getAppDataDir().toString());
    }
}