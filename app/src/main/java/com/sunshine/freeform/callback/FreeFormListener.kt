package com.sunshine.freeform.callback

/**
 * @author sunshine
 * @date 2021/2/15
 * 监听服务端是否准备好
 */
interface FreeFormListener {
    fun onSuccess()
    fun onFail()
}