package pitheguy.mcsrcdesktop.cef;

import org.cef.browser.CefBrowser;
import org.cef.handler.CefKeyboardHandlerAdapter;
import org.cef.misc.EventFlags;
import pitheguy.mcsrcdesktop.util.KeyCodes;

public class BrowserKeyboardHandler extends CefKeyboardHandlerAdapter {
    @Override
    public boolean onKeyEvent(CefBrowser browser, CefKeyEvent event) {
        if (event.type == CefKeyEvent.EventType.KEYEVENT_KEYUP) {
            if (event.windows_key_code == KeyCodes.F12) {
                browser.openDevTools();
                return true;
            }

            if (event.modifiers == EventFlags.EVENTFLAG_CONTROL_DOWN && event.windows_key_code == KeyCodes.MINUS) {
                browser.setZoomLevel(browser.getZoomLevel() - 1.0);
                return true;
            }

            if (event.modifiers == EventFlags.EVENTFLAG_CONTROL_DOWN && event.windows_key_code == KeyCodes.EQUALS) {
                browser.setZoomLevel(browser.getZoomLevel() + 1.0);
                return true;
            }

            if (event.modifiers == EventFlags.EVENTFLAG_CONTROL_DOWN && event.windows_key_code == KeyCodes.KEY_0) {
                browser.setZoomLevel(0.0);
                return true;
            }
        }

        return false;
    }
}
