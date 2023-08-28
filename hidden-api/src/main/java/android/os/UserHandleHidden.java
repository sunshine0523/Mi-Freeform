package android.os;

import dev.rikka.tools.refine.RefineAs;

@RefineAs(UserHandle.class)
public class UserHandleHidden {

    public UserHandleHidden(int userId) {
        throw new RuntimeException("Stub!");
    }
}
