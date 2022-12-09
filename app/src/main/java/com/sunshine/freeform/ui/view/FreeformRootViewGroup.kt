package com.sunshine.freeform.ui.view

import android.content.Context
import android.util.AttributeSet
import android.util.Log
import android.view.KeyEvent
import androidx.constraintlayout.widget.ConstraintLayout

//TODO
class FreeformRootViewGroup @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null
) : ConstraintLayout(context, attrs) {

    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        if (event.keyCode == KeyEvent.KEYCODE_BACK) {
            Log.e("根目录", "返回")
        }
        return super.dispatchKeyEvent(event)
    }
}