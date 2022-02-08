package com.sunshine.freeform.utils

import android.view.MotionEvent
import com.sunshine.freeform.MiFreeForm
import com.sunshine.freeform.bean.MotionEventBean
import rikka.shizuku.Shizuku
import rikka.shizuku.ShizukuRemoteProcess

/**
 * @author sunshine
 * @date 2022/2/6
 */
object RemoteServiceUtils {
    private var count: Int = 0
    private var xArray: FloatArray? = null
    private var yArray: FloatArray? = null

    fun remotePressBack(displayId: Int) {
        MiFreeForm.baseViewModel.getControlService()?.pressBack(displayId)
    }

    fun remoteInjectMotionEvent(
        motionEvent: MotionEvent?,
        displayId: Int,
        scale: Float
    ) {
        if (motionEvent == null) return

        count = motionEvent.pointerCount
        xArray = FloatArray(count)
        yArray = FloatArray(count)

        for (i in 0 until count) {
            val coords = MotionEvent.PointerCoords()
            motionEvent.getPointerCoords(i, coords)
            xArray!![i] = coords.x / scale
            yArray!![i] = coords.y / scale
        }

        MiFreeForm.baseViewModel.getControlService()?.touch(MotionEventBean(motionEvent.action, xArray!!, yArray!!, displayId))
    }
}