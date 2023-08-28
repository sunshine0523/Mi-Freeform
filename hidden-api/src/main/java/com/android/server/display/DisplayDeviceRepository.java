package com.android.server.display;

class DisplayDeviceRepository implements DisplayAdapter.Listener{
    public void addListener(Listener listener) {
        throw new RuntimeException("Stub!");
    }

    @Override
    public void onDisplayDeviceEvent(DisplayDevice device, int event) {
        throw new RuntimeException("Stub!");
    }

    @Override
    public void onTraversalRequested() {
        throw new RuntimeException("Stub!");
    }

    /**
     * Listens to {@link DisplayDevice} events from {@link DisplayDeviceRepository}.
     */
    public interface Listener {
        void onDisplayDeviceEventLocked(DisplayDevice device, int event);

        // TODO: multi-display - Try to remove the need for requestTraversal...it feels like
        // a shoe-horned method for a shoe-horned feature.
        void onTraversalRequested();
    };
}
