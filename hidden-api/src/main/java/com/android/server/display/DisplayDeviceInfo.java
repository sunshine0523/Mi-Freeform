package com.android.server.display;

import android.view.DisplayHidden;
import android.view.DisplayShape;

final class DisplayDeviceInfo {
    /**
     * Flag: Indicates that this display device has secure video output, such as HDCP.
     */
    public static final int FLAG_SECURE = 1 << 2;

    /**
     * Flag: Indicates that the display is suitable for presentations.
     */
    public static final int FLAG_PRESENTATION = 1 << 6;

    /**
     * Flag: Only show this display's own content; do not mirror
     * the content of another display.
     */
    public static final int FLAG_OWN_CONTENT_ONLY = 1 << 7;

    /**
     * Flag: This flag identifies secondary displays that should show system decorations, such as
     * status bar, navigation bar, home activity or IME.
     * <p>Note that this flag doesn't work without {@link #FLAG_TRUSTED}</p>
     * @hide
     */
    // TODO (b/114338689): Remove the flag and use IWindowManager#setShouldShowSystemDecors
    public static final int FLAG_SHOULD_SHOW_SYSTEM_DECORATIONS = 1 << 12;

    /**
     * Flag: The display is trusted to show system decorations and receive inputs without users'
     * touch.
     * @see #FLAG_SHOULD_SHOW_SYSTEM_DECORATIONS
     */
    public static final int FLAG_TRUSTED = 1 << 13;

    /**
     * Touch attachment: Touch input is via the internal interface.
     */
    public static final int TOUCH_INTERNAL = 1;

    /**
     * Touch attachment: Touch input is via an input device matching {@link VirtualDisplay}'s
     * uniqueId.
     * @hide
     */
    public static final int TOUCH_VIRTUAL = 3;

    /**
     * Gets the name of the display device, which may be derived from EDID or
     * other sources. The name may be localized and displayed to the user.
     */
    public String name;

    /**
     * Unique Id of display device.
     */
    public String uniqueId;

    /**
     * The width of the display in its natural orientation, in pixels.
     * This value is not affected by display rotation.
     */
    public int width;

    /**
     * The height of the display in its natural orientation, in pixels.
     * This value is not affected by display rotation.
     */
    public int height;

    /**
     * The active mode of the display.
     */
    public int modeId;

    /**
     * The default mode of the display.
     */
    public int defaultModeId;

    /**
     * The supported modes of the display.
     */
    public DisplayHidden.Mode[] supportedModes = DisplayHidden.Mode.EMPTY_ARRAY;


    /**
     * The nominal apparent density of the display in DPI used for layout calculations.
     * This density is sensitive to the viewing distance.  A big TV and a tablet may have
     * the same apparent density even though the pixels on the TV are much bigger than
     * those on the tablet.
     */
    public int densityDpi;

    /**
     * The physical density of the display in DPI in the X direction.
     * This density should specify the physical size of each pixel.
     */
    public float xDpi;

    /**
     * The physical density of the display in DPI in the X direction.
     * This density should specify the physical size of each pixel.
     */
    public float yDpi;

    /**
     * This is how far in advance a buffer must be queued for presentation at
     * a given time.  If you want a buffer to appear on the screen at
     * time N, you must submit the buffer before (N - bufferDeadlineNanos).
     */
    public long presentationDeadlineNanos;

    /**
     * Display flags.
     */
    public int flags;

    /**
     * The {@link RoundedCorners} if present or {@code null} otherwise.
     */
    public DisplayShape displayShape;

    /**
     * The touch attachment, per {@link DisplayViewport#touch}.
     */
    public int touch;

    /**
     * Display type.
     */
    public int type;

    /**
     * Display state.
     */
    public int state = DisplayHidden.STATE_ON;
}
