package pitheguy.mcsrcdesktop.cef;

import org.cef.CefApp;
import org.cef.browser.CefBrowser;

import javax.swing.*;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

public class MainFrame extends JFrame {
    public MainFrame(CefBrowser browser) {
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1280, 720);
        setExtendedState(getExtendedState() | JFrame.MAXIMIZED_BOTH);
        setTitle("mcsrc Desktop");
        JButton devToolsBtn = new JButton("DevTools");
        devToolsBtn.addActionListener(_ -> browser.openDevTools());
        getContentPane().add(devToolsBtn, BorderLayout.NORTH);
        getContentPane().add(browser.getUIComponent());
        setVisible(true);
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                CefApp.getInstance().dispose();
                dispose();
            }
        });
        setIconImage(Toolkit.getDefaultToolkit().getImage(getClass().getResource("/icon.png")));
    }
}
