package com.sunshine.freeform.activity.floating_view

import android.content.Context
import android.view.View

/**
 * @author sunshine
 * @date 2021/7/15
 */
abstract class FreeFormWindowAbs() {
    abstract val packageName: String
    abstract val command: String
    abstract var displayId: Int
    abstract fun resize()
    //退出最小化
    abstract fun exitSmall()
    abstract fun destroy()
}