package android.view;

public class WindowInsetsHidden {
    public static final class Type {
        static final int FIRST = 1 << 0;
        public static final int STATUS_BARS = FIRST;
        public static final int NAVIGATION_BARS = 1 << 1;
        public static final int CAPTION_BAR = 1 << 2;

        static final int IME = 1 << 3;

        static final int SYSTEM_GESTURES = 1 << 4;
        static final int MANDATORY_SYSTEM_GESTURES = 1 << 5;
        static final int TAPPABLE_ELEMENT = 1 << 6;

        static final int DISPLAY_CUTOUT = 1 << 7;

        static final int WINDOW_DECOR = 1 << 8;

        static final int GENERIC_OVERLAYS = 1 << 9;
        static final int LAST = GENERIC_OVERLAYS;
        static final int SIZE = 10;
    }
}
