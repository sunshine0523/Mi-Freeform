package com.sunshine.freeform.service.floating

import android.annotation.SuppressLint
import android.content.Context
import android.content.res.Configuration
import android.os.Handler
import android.os.Looper
import com.sunshine.freeform.callback.ServiceStateListener
import java.io.ObjectOutputStream
import java.net.Socket
import java.util.concurrent.TimeUnit

/**
 * @author sunshine
 * @date 2021/2/19
 * 一些小窗配置
 */
object FreeFormConfig {
    private const val IP_ADDRESS = "127.0.0.1"
    private const val TOUCH_PORT = 10259
    var touchSocket: Socket? = null
    var touchOos: ObjectOutputStream? = null

    var sendThread: Thread? = null

    var handler: Handler? = null

    const val MIME_TYPE = "video/hevc"    //h265
    const val FRAME_RATE = 60             //fps
    const val IFRAME_INTERVAL = 1         //关键帧间隔1s
    var dpi = 300                         //小窗默认分辨率为300dpi，可以自定义

    //所有小窗的集合，用于屏幕旋转时监听
    var freeFormViewSet = HashSet<FreeFormView>()
    //屏幕方向，1 竖屏 2横屏 0未定义
    var orientation = Configuration.ORIENTATION_UNDEFINED

    //最小化的边缘
    const val SMALL_FREEFORM_POSITION = 250
    //最小化的边缘留白
    const val SMALL_FREEFORM_DISTANCE = 250

    fun init(listener: ServiceStateListener?) {
        initSocket(listener)
        startSendEventThread()
    }

    /**
     * 初始化连接
     * 回调给活动
     */
    private fun initSocket(listener: ServiceStateListener?) {
        Thread {
            if (touchSocket == null) {
                try {
                    touchSocket = Socket(IP_ADDRESS, TOUCH_PORT)
                    touchOos = ObjectOutputStream(touchSocket!!.getOutputStream())
                    listener?.onStart()
                }catch (e: Exception) {
                    println("initSocket $e")
                    listener?.onStop()
                }
            }
        }.start()
    }

    /**
     * 初始化发送消息的线程
     */
    private fun startSendEventThread() {
        sendThread = Thread {
            Looper.prepare()
            handler = @SuppressLint("HandlerLeak")
            object : Handler(Looper.myLooper()!!) {}
            Looper.loop()
        }
        sendThread?.start()
    }

    /**
     * 屏幕旋转时，需要获取所有显示的小窗
     * 然后移除掉并重新创建
     * 屏幕方向通过无障碍直接设置，不需要传送参数，这样可以在第一次就知道屏幕的方向
     */
    fun orientationChanged() {
        freeFormViewSet.forEach {
            it.resize()
        }
    }

    /**
     * 当销毁服务时，因为这个是全局的，所以不会销毁，只有重启软件才会销毁
     * 而这样如果不重启软件而只是重启服务，这里就会有问题
     * 在服务的销毁中调用
     */
    fun onDelete(removeAllFreeForm: Boolean) {
        if (removeAllFreeForm) {
            freeFormViewSet.forEach {
                it.destroy()
            }
        }

        try {
            touchOos?.close()
            touchSocket?.shutdownOutput()
            touchSocket?.close()
            touchSocket = null
        }catch (e: Exception) {
            touchOos = null
            touchSocket = null
        }
        //停止发送消息的线程
        sendThread?.interrupt()
    }
}