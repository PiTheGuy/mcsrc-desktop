package pitheguy.mcsrcdesktop.util;

@SuppressWarnings("unused")
public class KeyCodes {
    // Mouse buttons
    public static final int LBUTTON    = 0x01; // Left mouse button
    public static final int RBUTTON    = 0x02; // Right mouse button
    public static final int MBUTTON    = 0x04; // Middle mouse button
    public static final int BACK       = 0x08; // Backspace key
    public static final int TAB        = 0x09; // Tab key
    public static final int CLEAR      = 0x0C; // Clear key
    public static final int RETURN     = 0x0D; // Enter key

    public static final int SHIFT      = 0x10; // Shift key
    public static final int CONTROL    = 0x11; // Ctrl key
    public static final int ALT        = 0x12; // Alt key
    public static final int PAUSE      = 0x13; // Pause key
    public static final int CAPS_LOCK  = 0x14; // Caps lock key
    public static final int ESCAPE     = 0x1B; // Esc key

    // Navigation / editing
    public static final int SPACE      = 0x20; // Spacebar key
    public static final int PAGE_UP    = 0x21; // Page up key
    public static final int NEXT       = 0x22; // Page down key
    public static final int END        = 0x23; // End key
    public static final int HOME       = 0x24; // Home key
    public static final int LEFT       = 0x25; // Left arrow key
    public static final int UP         = 0x26; // Up arrow key
    public static final int RIGHT      = 0x27; // Right arrow key
    public static final int DOWN       = 0x28; // Down arrow key
    public static final int SELECT     = 0x29; // Select key
    public static final int PRINT      = 0x2A; // Print key
    public static final int EXECUTE    = 0x2B; // Execute key
    public static final int SNAPSHOT   = 0x2C; // Print screen key
    public static final int INSERT     = 0x2D; // Insert key
    public static final int DELETE     = 0x2E; // Delete key
    public static final int HELP       = 0x2F; // Help key

    // Digits (top row)
    public static final int KEY_0 = 0x30;
    public static final int KEY_1 = 0x31;
    public static final int KEY_2 = 0x32;
    public static final int KEY_3 = 0x33;
    public static final int KEY_4 = 0x34;
    public static final int KEY_5 = 0x35;
    public static final int KEY_6 = 0x36;
    public static final int KEY_7 = 0x37;
    public static final int KEY_8 = 0x38;
    public static final int KEY_9 = 0x39;

    // Letters
    public static final int A = 0x41;
    public static final int B = 0x42;
    public static final int C = 0x43;
    public static final int D = 0x44;
    public static final int E = 0x45;
    public static final int F = 0x46;
    public static final int G = 0x47;
    public static final int H = 0x48;
    public static final int I = 0x49;
    public static final int J = 0x4A;
    public static final int K = 0x4B;
    public static final int L = 0x4C;
    public static final int M = 0x4D;
    public static final int N = 0x4E;
    public static final int O = 0x4F;
    public static final int P = 0x50;
    public static final int Q = 0x51;
    public static final int R = 0x52;
    public static final int S = 0x53;
    public static final int T = 0x54;
    public static final int U = 0x55;
    public static final int V = 0x56;
    public static final int W = 0x57;
    public static final int X = 0x58;
    public static final int Y = 0x59;
    public static final int Z = 0x5A;

    public static final int LWIN        = 0x5B; // Left Windows logo key
    public static final int RWIN        = 0x5C; // Right Windows logo key

    // Numeric keypad
    public static final int NUMPAD0     = 0x60;
    public static final int NUMPAD1     = 0x61;
    public static final int NUMPAD2     = 0x62;
    public static final int NUMPAD3     = 0x63;
    public static final int NUMPAD4     = 0x64;
    public static final int NUMPAD5     = 0x65;
    public static final int NUMPAD6     = 0x66;
    public static final int NUMPAD7     = 0x67;
    public static final int NUMPAD8     = 0x68;
    public static final int NUMPAD9     = 0x69;
    public static final int MULTIPLY    = 0x6A; // Multiply key
    public static final int ADD         = 0x6B; // Add key
    public static final int SEPARATOR   = 0x6C; // Separator key
    public static final int SUBTRACT    = 0x6D; // Subtract key
    public static final int DECIMAL     = 0x6E; // Decimal key
    public static final int DIVIDE      = 0x6F; // Divide key

    // Function keys
    public static final int F1  = 0x70;
    public static final int F2  = 0x71;
    public static final int F3  = 0x72;
    public static final int F4  = 0x73;
    public static final int F5  = 0x74;
    public static final int F6  = 0x75;
    public static final int F7  = 0x76;
    public static final int F8  = 0x77;
    public static final int F9  = 0x78;
    public static final int F10 = 0x79;
    public static final int F11 = 0x7A;
    public static final int F12 = 0x7B;
    public static final int F13 = 0x7C;
    public static final int F14 = 0x7D;
    public static final int F15 = 0x7E;
    public static final int F16 = 0x7F;
    public static final int F17 = 0x80;
    public static final int F18 = 0x81;
    public static final int F19 = 0x82;
    public static final int F20 = 0x83;
    public static final int F21 = 0x84;
    public static final int F22 = 0x85;
    public static final int F23 = 0x86;
    public static final int F24 = 0x87;

    public static final int NUM_LOCK    = 0x90; // Num lock key
    public static final int SCROLL_LOCK = 0x91; // Scroll lock key

    // Left/right modifiers
    public static final int LSHIFT      = 0xA0; // Left Shift key
    public static final int RSHIFT      = 0xA1; // Right Shift key
    public static final int LCONTROL    = 0xA2; // Left Ctrl key
    public static final int RCONTROL    = 0xA3; // Right Ctrl key
    public static final int LMENU       = 0xA4; // Left Alt key
    public static final int RMENU       = 0xA5; // Right Alt key

    public static final int EQUALS  = 0xBB;
    public static final int COMMA   = 0xBC;
    public static final int MINUS   = 0xBD;
    public static final int PERIOD  = 0xBE;
}
