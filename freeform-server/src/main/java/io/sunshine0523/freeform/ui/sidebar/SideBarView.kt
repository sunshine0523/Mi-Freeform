package io.sunshine0523.freeform.ui.sidebar

import android.content.Context
import android.util.AttributeSet
import android.view.KeyEvent
import android.view.View

/**
 * @author KindBrave
 * @since 2023/8/22
 */
class SideBarView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {
    init {
        isFocusableInTouchMode = true
    }
    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        if (event.keyCode == KeyEvent.KEYCODE_BACK) return true
        return super.dispatchKeyEvent(event)
    }
}