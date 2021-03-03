package com.sunshine.freeform.service.floating

import android.app.Instrumentation
import android.content.Context
import android.hardware.input.InputManager
import android.os.SystemClock
import android.util.AttributeSet
import android.util.Log
import android.view.InputDevice
import android.view.KeyEvent
import androidx.cardview.widget.CardView
import com.sunshine.freeform.callback.BackClickListener
import com.sunshine.freeform.utils.InputEventUtils
import java.lang.reflect.Method

/**
 * @author sunshine
 * @date 2021/2/20
 * 继承cardView，用于接收处理返回键
 * 处理方式：在接收到返回键时，将layoutParams的flags设置为可以接收
 */
class FreeFormView @JvmOverloads constructor(
        context: Context,
        attrs: AttributeSet? = null,
        defStyleAttr: Int = 0
) : CardView(context, attrs, defStyleAttr){

    companion object {
        private const val TAG = "FreeFormView"
    }

    private var callBack: BackClickListener? = null
    fun setCallBack(callBack: BackClickListener?) {
        this.callBack = callBack
    }

    override fun dispatchKeyEvent(event: KeyEvent?): Boolean {
//        Log.e(TAG, event.toString())
//        //是返回键就处理，0x48才是原本的key
//        if (event?.keyCode == KeyEvent.KEYCODE_BACK && event.action == KeyEvent.ACTION_DOWN) {
//            callBack?.onClick()
//        }
        return super.dispatchKeyEvent(event)
    }

}