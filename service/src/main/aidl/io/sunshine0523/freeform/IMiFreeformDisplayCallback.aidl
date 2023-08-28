// IMiFreeformDisplayCallback.aidl
package io.sunshine0523.freeform;

oneway interface IMiFreeformDisplayCallback {
    void onDisplayPaused();
    void onDisplayResumed();
    void onDisplayStopped();
    void onDisplayAdd(int displayId);
}