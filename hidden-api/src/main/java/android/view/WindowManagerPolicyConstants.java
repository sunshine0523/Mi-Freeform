package android.view;

public interface WindowManagerPolicyConstants {
    interface PointerEventListener {
        /**
         * 1. onPointerEvent will be called on the service.UiThread.
         * 2. motionEvent will be recycled after onPointerEvent returns so if it is needed later a
         * copy() must be made and the copy must be recycled.
         **/
        void onPointerEvent(MotionEvent motionEvent);
    }
}
