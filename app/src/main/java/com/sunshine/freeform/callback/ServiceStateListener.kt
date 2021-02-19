package com.sunshine.freeform.callback

/**
 * @author sunshine
 * @date 2021/2/4
 * 服务状态监听
 */
interface ServiceStateListener {
    fun onStart()
    fun onStop()
}