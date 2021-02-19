package com.sunshine.freeform

/**
 * @author sunshine
 * @date 2021/2/10
 * 服务类
 * 用于监听触摸事件
 */
object Server {
    @JvmStatic
    fun main(args: Array<String>) {
        SocketServer().startService()
    }
}