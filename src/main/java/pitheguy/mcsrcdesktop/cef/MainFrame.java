package pitheguy.mcsrcdesktop.cef;

import org.cef.CefApp;
import org.cef.CefClient;
import org.cef.browser.CefBrowser;
import org.cef.handler.CefLifeSpanHandlerAdapter;

import javax.swing.*;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.function.Function;

public class MainFrame extends JFrame {
    private boolean closeConfirmed;

    public MainFrame(CefClient client, Function<Dimension, CefBrowser> browserFactory) {
        setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        setSize(1280, 720);
        setExtendedState(getExtendedState() | JFrame.MAXIMIZED_BOTH);
        setTitle("mcsrc Desktop");
        setIconImage(Toolkit.getDefaultToolkit().getImage(getClass().getResource("/icon.png")));
        setVisible(true);
        CefBrowser browser = browserFactory.apply(getSize());
        client.addLifeSpanHandler(new CefLifeSpanHandlerAdapter() {
            @Override
            public boolean doClose(CefBrowser browser) {
                closeConfirmed = true;
                return browser.doClose();
            }

            @Override
            public void onBeforeClose(CefBrowser browser) {
                CefApp.getInstance().dispose();
            }
        });
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                if (closeConfirmed) {
                    browser.setCloseAllowed();
                    browser.close(true);
                    dispose();
                } else {
                    browser.close(false);
                }
            }
        });
        getContentPane().add(browser.getUIComponent());
        revalidate();
    }
}
