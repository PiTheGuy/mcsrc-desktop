package pitheguy.mcsrcdesktop.cef;

import org.cef.CefBrowserSettings;
import org.cef.CefClient;
import org.cef.browser.*;
import org.cef.callback.CefDragData;
import org.cef.handler.CefRenderHandler;
import org.cef.handler.CefScreenInfo;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferInt;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

public class Browser extends CefBrowserWindowless implements CefRenderHandler {
    private final BrowserCanvas canvas = new BrowserCanvas();
    private final List<Consumer<CefPaintEvent>> paintListeners = new ArrayList<>();
    private BufferedImage image;

    public Browser(CefClient client, String url, CefRequestContext context, CefBrowserSettings settings, Dimension canvasSize) {
        this(client, url, context, settings, canvasSize, null);
    }

    public Browser(CefClient client, String url, CefRequestContext context, CefBrowserSettings settings, Dimension canvasSize, CefBrowserWindowless parent) {
        super(client, url, context, settings);
        canvas.setFocusable(true);
        canvas.addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent e) {
                wasResized(canvas.getWidth(), canvas.getHeight());
            }
        });
        MouseAdapter mouse = new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                sendMouseEvent(e);
            }

            @Override
            public void mousePressed(MouseEvent e) {
                sendMouseEvent(e);
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                sendMouseEvent(e);
            }

            @Override
            public void mouseEntered(MouseEvent e) {
                sendMouseEvent(e);
            }

            @Override
            public void mouseExited(MouseEvent e) {
                sendMouseEvent(e);
            }

            @Override
            public void mouseDragged(MouseEvent e) {
                sendMouseEvent(e);
            }

            @Override
            public void mouseMoved(MouseEvent e) {
                sendMouseEvent(e);
            }

            @Override
            public void mouseWheelMoved(MouseWheelEvent e) {
                e.consume();
                MouseWheelEvent event = new MouseWheelEvent(canvas, e.getID(), e.getWhen(), e.getModifiersEx(), e.getX(),
                        e.getY(), e.getClickCount(), e.isPopupTrigger(), e.getScrollType(), -e.getScrollAmount() * 25, e.getWheelRotation());
                sendMouseWheelEvent(event);
            }
        };
        canvas.addMouseListener(mouse);
        canvas.addMouseMotionListener(mouse);
        canvas.addMouseWheelListener(mouse);
        canvas.addKeyListener(new KeyListener() {
            @Override
            public void keyTyped(KeyEvent e) {
                sendKeyEvent(e);
            }

            @Override
            public void keyPressed(KeyEvent e) {
                sendKeyEvent(e);
            }

            @Override
            public void keyReleased(KeyEvent e) {
                sendKeyEvent(e);
            }
        });
        canvas.addFocusListener(new FocusAdapter() {
            @Override
            public void focusGained(FocusEvent e) {
                setFocus(true);
            }

            @Override
            public void focusLost(FocusEvent e) {
                setFocus(false);
            }
        });
        canvas.setSize(canvasSize);
        if (parent != null) {
            createDevTools(parent, client, 0, true, true, null, null);
            wasResized(900, 600);
        } else {
            createBrowser(client, 0, url, true, false, null, context);
        }
    }

    //TODO fix DevTools window being frozen
    @Override
    protected CefBrowserWindowless createDevToolsBrowserWindowless(CefClient client, String url, CefRequestContext context, CefBrowserWindowless parent, Point inspectAt) {
        return new Browser(client, url, context, new CefBrowserSettings(), new Dimension(900, 600), this);
    }

    @Override
    public CefRenderHandler getRenderHandler() {
        return this;
    }

    @Override
    public void createImmediately() {
    }

    @Override
    public Component getUIComponent() {
        return canvas;
    }

    @Override
    public CompletableFuture<BufferedImage> createScreenshot(boolean nativeResolution) {
        throw new UnsupportedOperationException("Not implemented");
    }

    @Override
    public Rectangle getViewRect(CefBrowser browser) {
        Dimension size = canvas.getSize();
        return new Rectangle(0, 0, size.width, size.height);
    }

    @Override
    public boolean getScreenInfo(CefBrowser browser, CefScreenInfo screenInfo) {
        return false;
    }

    @Override
    public Point getScreenPoint(CefBrowser browser, Point viewPoint) {
        return null;
    }

    @Override
    public void onPopupShow(CefBrowser browser, boolean show) {
    }

    @Override
    public void onPopupSize(CefBrowser browser, Rectangle size) {

    }

    @Override
    public void onPaint(CefBrowser browser, boolean popup, Rectangle[] dirtyRects, ByteBuffer buffer, int width, int height) {
        BufferedImage frame = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        DataBufferInt frameBuffer = (DataBufferInt) frame.getRaster().getDataBuffer();
        int[] pixels = frameBuffer.getData();
        for (int i = 0; i < pixels.length; i++) {
            int b = buffer.get() & 0xFF;
            int g = buffer.get() & 0xFF;
            int r = buffer.get() & 0xFF;
            int a = buffer.get() & 0xFF;
            pixels[i] = (a << 24) | (r << 16) | (g << 8) | b;
        }
        image = frame;
        SwingUtilities.invokeLater(canvas::repaint);
        CefPaintEvent event = new CefPaintEvent(browser, popup, dirtyRects, buffer, width, height);
        paintListeners.forEach(l -> l.accept(event));
    }

    @Override
    public void addOnPaintListener(Consumer<CefPaintEvent> listener) {
        paintListeners.add(listener);
    }

    @Override
    public void setOnPaintListener(Consumer<CefPaintEvent> listener) {
        paintListeners.clear();
        paintListeners.add(listener);
    }

    @Override
    public void removeOnPaintListener(Consumer<CefPaintEvent> listener) {
        paintListeners.remove(listener);
    }

    @Override
    public boolean onCursorChange(CefBrowser browser, int cursorType) {
        canvas.setCursor(Cursor.getPredefinedCursor(cursorType));
        return true;
    }

    @Override
    public boolean startDragging(CefBrowser browser, CefDragData dragData, int mask, int x, int y) {
        return false;
    }

    @Override
    public void updateDragCursor(CefBrowser browser, int operation) {

    }

    private class BrowserCanvas extends JComponent {
        @Override
        public void paintComponent(Graphics g) {
            if (image != null) {
                g.drawImage(image, 0, 0, null);
            }
        }
    }
}
