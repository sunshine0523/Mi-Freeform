package com.sunshine.freeform.service.floating

import android.annotation.SuppressLint
import android.app.Service
import android.graphics.SurfaceTexture
import android.hardware.display.DisplayManager
import android.media.MediaCodec
import android.media.MediaCodecInfo
import android.media.MediaFormat
import android.provider.Settings
import android.view.*

import com.sunshine.freeform.R
import com.sunshine.freeform.service.floating.FreeFormConfig.FRAME_RATE
import com.sunshine.freeform.service.floating.FreeFormConfig.IFRAME_INTERVAL
import com.sunshine.freeform.service.floating.FreeFormConfig.MIME_TYPE
import com.sunshine.freeform.service.floating.FreeFormConfig.dpi
import com.sunshine.freeform.service.floating.FreeFormConfig.freeFormViewSet
import com.sunshine.freeform.utils.ShellUtils

import java.io.IOException
import java.nio.ByteBuffer

/**
 * @author sunshine
 * @date 2021/2/15
 * 小窗界面，采用MediaCodec显示画面
 */
class FreeFormMediaCodecView(
        private val service: FloatingService,
        command: String,
        packageName: String /*用于启动应用*/) : FreeFormView(service, packageName) {

    private var bufferInfo: MediaCodec.BufferInfo? = null
    private var encodeMediaCodec: MediaCodec? = null
    private var decodeMediaCodec: MediaCodec? = null
    //编码线程，最后释放
    private var encodeThread: Thread? = null

    //主界面
    private var textureView: TextureView? = null

    override fun initDisplay() {
        initEncode()

        val displayManager = service.getSystemService(DisplayManager::class.java) as DisplayManager
        virtualDisplay = displayManager.createVirtualDisplay("mi-freeform-display-$this", WIDTH, HEIGHT, dpi, encodeMediaCodec!!.createInputSurface(), DisplayManager.VIRTUAL_DISPLAY_FLAG_PRESENTATION)
        displayId = virtualDisplay!!.display.displayId

        println("FreeFormView initEncode displayId $displayId")
    }

    /**
     * 初始化编码器
     */
    private fun initEncode() {
        try {
            bufferInfo = MediaCodec.BufferInfo()
            val format = MediaFormat.createVideoFormat(MIME_TYPE, WIDTH, HEIGHT)
            format.setInteger(MediaFormat.KEY_COLOR_FORMAT, MediaCodecInfo.CodecCapabilities.COLOR_FormatSurface)
            format.setInteger(MediaFormat.KEY_FRAME_RATE, FRAME_RATE)
            format.setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, IFRAME_INTERVAL)
            format.setInteger(MediaFormat.KEY_BIT_RATE, WIDTH * HEIGHT * 60)
            encodeMediaCodec = MediaCodec.createEncoderByType(MIME_TYPE)
            encodeMediaCodec!!.configure(format, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE)

        } catch (e: IOException) {
            println("initEncode $e")
        }
    }

    @SuppressLint("ClickableViewAccessibility", "InflateParams")
    override fun showFreeFormWindow() {
        if (Settings.canDrawOverlays(service)) {
            freeFormLayout = LayoutInflater.from(service).inflate(R.layout.view_freeform_mediacodec, null, false)
            swingView = freeFormLayout!!.findViewById(R.id.view_swing)
            textureView = freeFormLayout!!.findViewById(R.id.textureView)
            closeView = freeFormLayout!!.findViewById(R.id.view_close)

            swingView!!.setOnTouchListener(TouchListener(R.id.textureView, textureView!!))
            textureView!!.setOnTouchListener(TouchListener(R.id.textureView, textureView!!))
            closeView!!.setOnTouchListener(TouchListener(R.id.textureView, textureView!!))

            textureView!!.surfaceTextureListener = object : TextureView.SurfaceTextureListener {
                override fun onSurfaceTextureAvailable(surface: SurfaceTexture, width: Int, height: Int) {
                    val s = Surface(surface)
                    initDecode(s)
                    encode()
                }

                override fun onSurfaceTextureSizeChanged(surface: SurfaceTexture, width: Int, height: Int) {

                }

                override fun onSurfaceTextureDestroyed(surface: SurfaceTexture): Boolean {
                    return true
                }

                override fun onSurfaceTextureUpdated(surface: SurfaceTexture) {

                }

            }

            addView()

        } else {
            service.stopSelf()
        }
    }

    /**
     * 初始化解码器
     */
    private fun initDecode(surface: Surface) {
        try {
            decodeMediaCodec = MediaCodec.createDecoderByType(MIME_TYPE)
            val format = MediaFormat.createVideoFormat(MIME_TYPE, WIDTH, HEIGHT)
            format.setInteger(MediaFormat.KEY_FRAME_RATE, FRAME_RATE)
            format.setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, IFRAME_INTERVAL)
            format.setInteger(MediaFormat.KEY_BIT_RATE, WIDTH * HEIGHT * 60)
            format.setInteger(MediaFormat.KEY_BITRATE_MODE, MediaCodecInfo.EncoderCapabilities.BITRATE_MODE_CQ)
            decodeMediaCodec!!.configure(format, surface, null, 0)
            decodeMediaCodec!!.start()
        } catch (e: IOException) {
            println("initDecode $e")
        }
    }

    /**
     *  开始编码
     */
    private fun encode() {
        encodeMediaCodec!!.start()
        encodeThread = Thread {
            while (true) {
                try {
                    val outputBuffers: Array<ByteBuffer> = encodeMediaCodec!!.outputBuffers
                    val outputBufferId: Int = encodeMediaCodec!!.dequeueOutputBuffer(bufferInfo!!, 0)
                    if (outputBufferId >= 0) {
                        val bb = outputBuffers[outputBufferId]
                        val byteArray = ByteArray(bufferInfo!!.size)
                        bb.get(byteArray)
                        onFrame(byteArray, byteArray.size)

                        encodeMediaCodec!!.releaseOutputBuffer(outputBufferId, false)
                    }
                }catch (e: Exception) {
                    println("encode $e")
                    break
                }
            }
        }
        encodeThread?.start()
    }

    /**
     * 逐帧解码
     */
    private fun onFrame(buf: ByteArray, length: Int): Boolean {
        val inputBuffers: Array<ByteBuffer> = decodeMediaCodec!!.inputBuffers
        val inputBufferIndex: Int = decodeMediaCodec!!.dequeueInputBuffer(100)
        if (inputBufferIndex >= 0) {
            val inputBuffer = inputBuffers[inputBufferIndex]
            inputBuffer.clear()
            inputBuffer.put(buf, 0, length)
            decodeMediaCodec!!.queueInputBuffer(inputBufferIndex, 0, length, System.currentTimeMillis(), 0)
        } else {
            return false
        }
        val bufferInfo = MediaCodec.BufferInfo()
        //这个的第二个参数不应该过小，否则可能会产生绿屏
        var outputBufferIndex: Int = decodeMediaCodec!!.dequeueOutputBuffer(bufferInfo, 20000)
        while (outputBufferIndex >= 0) {
            decodeMediaCodec!!.releaseOutputBuffer(outputBufferIndex, true)
            outputBufferIndex = decodeMediaCodec!!.dequeueOutputBuffer(bufferInfo, 0)
        }
        return true
    }

    //屏幕翻转时，应该重新设置小窗大小
    override fun resize() {
        //更改宽高
        setWidthHeight()

        //重新设置vd
        virtualDisplay!!.resize(WIDTH, HEIGHT, dpi)

        //重新设置layout
        layoutParams!!.apply {
            width = WIDTH
            height = HEIGHT + dip2px(50f)
        }
        windowManager!!.updateViewLayout(freeFormLayout, layoutParams)

        //更改mediaCodec配置
        //首先释放之前的对象
        encodeMediaCodec?.release()
        decodeMediaCodec?.release()
        encodeThread?.interrupt()

        //重新开启新的编码解码，并且不要忘记设置vd的surface!!!
        initEncode()
        virtualDisplay!!.surface = encodeMediaCodec!!.createInputSurface()
        //其实直接用windowsShow那个listener就行，但是为了看清楚又加一个
        textureView!!.surfaceTextureListener = object : TextureView.SurfaceTextureListener {
            override fun onSurfaceTextureAvailable(surface: SurfaceTexture, width: Int, height: Int) {

            }

            override fun onSurfaceTextureSizeChanged(surface: SurfaceTexture, width: Int, height: Int) {
                val s = Surface(surface)
                initDecode(s)
                encode()
            }

            override fun onSurfaceTextureDestroyed(surface: SurfaceTexture): Boolean {
                return true
            }

            override fun onSurfaceTextureUpdated(surface: SurfaceTexture) {

            }

        }
    }

    /**
     * 释放该小窗
     * 这个十分重要，经过排查，如果不释放mediaCodec，会导致cpu居高不下，会导致设备异常卡顿
     */
    override fun destroy() {
        windowManager!!.removeView(freeFormLayout)
        virtualDisplay!!.release()
        decodeMediaCodec?.release()
        encodeMediaCodec?.release()
        encodeThread?.interrupt()

        freeFormViewSet.remove(this)
    }

    init {
        //注册
        freeFormViewSet.add(this)
        //初始化windowManager，稍后悬浮窗也需要，这里需要获取屏幕宽高，用来确认默认宽高
        windowManager = service.getSystemService(Service.WINDOW_SERVICE) as WindowManager

        setWidthHeight()
        initDisplay()
        showFreeFormWindow()

        //启动程序
        ShellUtils.execCommand(command + displayId, true)
    }
}