package pitheguy.mcsrcdesktop.cef;

import org.cef.CefApp;
import org.cef.browser.CefBrowser;

import javax.swing.*;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.function.Function;

public class MainFrame extends JFrame {
    public MainFrame(Function<Dimension, CefBrowser> browserFactory) {
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1280, 720);
        setExtendedState(getExtendedState() | JFrame.MAXIMIZED_BOTH);
        setTitle("mcsrc Desktop");
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                CefApp.getInstance().dispose();
                dispose();
            }
        });
        setIconImage(Toolkit.getDefaultToolkit().getImage(getClass().getResource("/icon.png")));
        setVisible(true);
        CefBrowser browser = browserFactory.apply(getSize());
        getContentPane().add(browser.getUIComponent());
        revalidate();
    }
}
