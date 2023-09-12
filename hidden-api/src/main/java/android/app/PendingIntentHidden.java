package android.app;

import android.content.IIntentSender;
import android.os.IBinder;

import dev.rikka.tools.refine.RefineAs;

@RefineAs(PendingIntent.class)
public class PendingIntentHidden {
    public IIntentSender getTarget() {
        throw new RuntimeException("Stub!");
    }

    public IBinder getWhitelistToken() {
        throw new RuntimeException("Stub!");
    }

}