package com.sunshine.freeform.bean

import java.io.Serializable

/**
 * @author sunshine
 * @date 2021/2/20
 */
data class EventData(
    var type: Int,              //事件类型，1 MotionEvent 2 KeyEvent
    var action: Int,            //MotionEvent必要，KeyEvent固定为down -> up
    var xArray: FloatArray?,     //MotionEvent专属
    var yArray: FloatArray?,     //MotionEvent专属
    var flags: Int,             //MotionEvent专属，KeyEvent可不需要
    var source: Int,            //MotionEvent必要
    var displayId: Int,         //必要
) : Serializable {

    companion object {
        private const val serialVersionUID = 957264038542547537L
    }

}