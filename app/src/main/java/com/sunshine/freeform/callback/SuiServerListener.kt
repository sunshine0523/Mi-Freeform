package com.sunshine.freeform.callback

/**
 * @author sunshine
 * @date 2021/3/19
 */
abstract class SuiServerListener {
    abstract fun onStart()
    abstract fun onStop()
    open fun onFailBind() {}
}