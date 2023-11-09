package android.view;

import dev.rikka.tools.refine.RefineAs;

@RefineAs(DisplayShape.class)
public class DisplayShapeHidden {

    public static DisplayShape createDefaultDisplayShape(
            int displayWidth, int displayHeight, boolean isScreenRound) {
        throw new RuntimeException("Stub!");
    }

}
