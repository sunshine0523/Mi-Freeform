package com.sunshine.freeform.utils

import android.annotation.SuppressLint
import android.content.Context
import android.hardware.SensorManager
import android.util.Log
import android.view.OrientationEventListener
import android.view.Surface

import java.lang.reflect.Field


/**
 * @author sunshine
 * @date 2021/2/19
 */
class ScreenOrientationListener(context: Context) : OrientationEventListener(context) {
    private var mOrientation = 0
    private var mOnOrientationChangedListener: OnOrientationChangedListener? = null
    private val mContext: Context
    private var mFieldRotation: Field? = null
    private var mOLegacy: Any? = null
    fun setOnOrientationChangedListener(listener: OnOrientationChangedListener?) {
        mOnOrientationChangedListener = listener
    }

    @SuppressLint("PrivateApi")
    fun getOrientation(): Int {
        var rotation = -1
        try {
            if (null == mFieldRotation) {
                val sensorManager = mContext.getSystemService(Context.SENSOR_SERVICE) as SensorManager
                val clazzLegacy = Class.forName("android.hardware.LegacySensorManager")
                val constructor = clazzLegacy.getConstructor(SensorManager::class.java)
                constructor.isAccessible = true
                mOLegacy = constructor.newInstance(sensorManager)
                mFieldRotation = clazzLegacy.getDeclaredField("sRotation")
                mFieldRotation!!.isAccessible = true
            }
            rotation = mFieldRotation!!.getInt(mOLegacy)
        } catch (e: Exception) {
            Log.e(TAG, "getRotation e=" + e.message)
            e.printStackTrace()
        }
        //        Log.d(TAG, "getRotation rotation=" + rotation);
        var orientation = -1
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

    override fun onOrientationChanged(orientation: Int) {
        var orientation = orientation
        if (orientation == ORIENTATION_UNKNOWN) {
            return  // 手机平放时，检测不到有效的角度
        }
        orientation = getOrientation()
        if (mOrientation != orientation) {
            mOrientation = orientation
            if (null != mOnOrientationChangedListener) {
                mOnOrientationChangedListener!!.onOrientationChanged(mOrientation)
                Log.d(TAG, "ScreenOrientationListener onOrientationChanged orientation=$mOrientation")
            }
        }
    }

    interface OnOrientationChangedListener {
        fun onOrientationChanged(orientation: Int)
    }

    companion object {
        private val TAG = ScreenOrientationListener::class.java.simpleName
    }

    init {
        mContext = context
    }
}