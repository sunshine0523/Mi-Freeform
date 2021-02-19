package com.sunshine.freeform.utils

/**
 * @author sunshine
 * @date 2021/2/19
 */
class ScreenOrientationListener constructor(context: Context?) : OrientationEventListener(context) {
    private var mOrientation: kotlin.Int = 0
    private var mOnOrientationChangedListener: ScreenOrientationListener.OnOrientationChangedListener? = null
    private var mContext: Context?
    private var mFieldRotation: Field? = null
    private var mOLegacy: Object? = null
    fun setOnOrientationChangedListener(listener: ScreenOrientationListener.OnOrientationChangedListener?) {
        this.mOnOrientationChangedListener = listener9
    }

    fun getOrientation(): kotlin.Int {
        var rotation: kotlin.Int = -1
        try {
            if (null == mFieldRotation) {
                var sensorManager: SensorManager? = mContext.getSystemService(Context.SENSOR_SERVICE) as SensorManager?
                var clazzLegacy: Class? = Class.forName("android.hardware.LegacySensorManager")
                var constructor: Constructor? = clazzLegacy.getConstructor(SensorManager::class.java)
                constructor.setAccessible(true)
                mOLegacy = constructor.newInstance(sensorManager)
                mFieldRotation = clazzLegacy.getDeclaredField("sRotation")
                mFieldRotation.setAccessible(true)
            }
            rotation = mFieldRotation.getInt(mOLegacy)
        } catch (e: Exception) {
            Log.e(ScreenOrientationListener.Companion.TAG, "getRotation e=" + e.getMessage())
            e.printStackTrace()
        }
        //        Log.d(TAG, "getRotation rotation=" + rotation);
        var orientation: kotlin.Int = -1
        when (rotation) {
            Surface.ROTATION_0 -> orientation = 0
            Surface.ROTATION_90 -> orientation = 90
            Surface.ROTATION_180 -> orientation = 180
            Surface.ROTATION_270 -> orientation = 270
            else -> {
            }
        }
        //        Log.d(TAG, "getRotation orientation=" + orientation);
        return orientation
    }

    @Override
    fun onOrientationChanged(orientation: kotlin.Int) {
        var orientation: kotlin.Int = orientation
        if (orientation == OrientationEventListener.ORIENTATION_UNKNOWN) {
            return  // 手机平放时，检测不到有效的角度
        }
        orientation = getOrientation()
        if (mOrientation != orientation) {
            mOrientation = orientation
            if (null != mOnOrientationChangedListener) {
                mOnOrientationChangedListener.onOrientationChanged(mOrientation)
                Log.d(ScreenOrientationListener.Companion.TAG, "ScreenOrientationListener onOrientationChanged orientation=" + mOrientation)
            }
        }
    }

    open interface OnOrientationChangedListener {
        open fun onOrientationChanged(orientation: kotlin.Int)
    }

    companion object {
        private val TAG: String? = ScreenOrientationListener::class.java.getSimpleName()
    }

    init {
        mContext = context
    }
}