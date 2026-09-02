package pitheguy.mcsrcdesktop.cef;

import org.cef.browser.CefBrowser;
import org.cef.handler.CefKeyboardHandlerAdapter;

import java.awt.event.KeyEvent;

public class BrowserKeyboardHandler extends CefKeyboardHandlerAdapter {
    @Override
    public boolean onKeyEvent(CefBrowser browser, CefKeyEvent event) {
        if (event.windows_key_code == KeyEvent.VK_F12) {
            browser.openDevTools();
            return true;
        }
        return false;
    }
}
