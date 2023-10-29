package android.view;

import dev.rikka.tools.refine.RefineAs;

@RefineAs(WindowManager.class)
public interface WindowManagerHidden{
    public static class LayoutParams extends ViewGroup.LayoutParams{
        /**
         * Flag to indicate that the window is controlling how it fits window insets on its own.
         * So we don't need to adjust its attributes for fitting window insets.
         */
        public static final int PRIVATE_FLAG_FIT_INSETS_CONTROLLED = 0x10000000;

        /**
         * Flag to request creation of a BLAST (Buffer as LayerState) Layer.
         * If not specified the client will receive a BufferQueue layer.
         */
        public static final int PRIVATE_FLAG_USE_BLAST = 0x02000000;

        /** In a multiuser system if this flag is set and the owner is a system process then this
         * window will appear on all user screens. This overrides the default behavior of window
         * types that normally only appear on the owning user's screen. Refer to each window type
         * to determine its default behavior.
         */
        public static final int SYSTEM_FLAG_SHOW_FOR_ALL_USERS = 0x00000010;

        /**
         * Indicates that this window is the rounded corners overlay present on some
         * devices this means that it will be excluded from: screenshots,
         * screen magnification, and mirroring.
         */
        public static final int PRIVATE_FLAG_IS_ROUNDED_CORNERS_OVERLAY = 0x00100000;

        /**
         * Flag to indicate that the window is a trusted overlay.
         */
        public static final int PRIVATE_FLAG_TRUSTED_OVERLAY = 0x20000000;

        private int mFitInsetsTypes;

        public int x;
        public int y;
        public int type;
        public int width;
        public int height;
        public int flags;
        public int format;
        public int privateFlags;

        public LayoutParams() {
            super(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT);
            throw new RuntimeException("Stub!");
        }

        public void setFitInsetsTypes(int types) {
            throw new RuntimeException("Stub!");
        }
    }
}
