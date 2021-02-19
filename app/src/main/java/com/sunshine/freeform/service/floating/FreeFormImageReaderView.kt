package com.sunshine.freeform.service.floating

import android.annotation.SuppressLint
import android.app.Service
import android.graphics.Bitmap
import android.graphics.PixelFormat
import android.graphics.SurfaceTexture
import android.hardware.display.DisplayManager
import android.media.Image
import android.media.ImageReader
import android.provider.Settings
import android.view.LayoutInflater
import android.view.Surface
import android.view.TextureView
import android.view.WindowManager
import android.widget.ImageView

import com.sunshine.freeform.R
import com.sunshine.freeform.service.floating.FreeFormConfig.dpi
import com.sunshine.freeform.service.floating.FreeFormConfig.freeFormViewSet
import com.sunshine.freeform.utils.ShellUtils

import java.nio.ByteBuffer


/**
 * @author sunshine
 * @date 2021/2/15
 * 小窗界面，采用ImageReader显示画面
 */
class FreeFormImageReaderView(
        private val service: FloatingService,
        command: String,
        packageName: String /*用于启动应用*/) : FreeFormView(service, packageName) {

    //主界面
    private var imageView: ImageView? = null

    override fun initDisplay() {
        initImageReader()

        val displayManager = service.getSystemService(DisplayManager::class.java) as DisplayManager
        virtualDisplay = displayManager.createVirtualDisplay("mi-freeform-display-$this", WIDTH, HEIGHT, dpi, imageReader?.surface, DisplayManager.VIRTUAL_DISPLAY_FLAG_PRESENTATION)
        displayId = virtualDisplay!!.display.displayId

        println("FreeFormView initEncode displayId $displayId")
    }

    @SuppressLint("WrongConstant")
    private fun initImageReader() {
        imageReader = ImageReader.newInstance(WIDTH, HEIGHT, PixelFormat.RGBA_8888, 1)

        var width: Int
        var height: Int
        var planes: Array<Image.Plane>
        var buffer: ByteBuffer
        var pixelStride: Int
        var rowStride: Int
        var rowPadding: Int
        var bitmap: Bitmap
        var image: Image

        imageReader!!.setOnImageAvailableListener({
            try {
                //获取下一个最新的可用Image，没有则返回null
                //建议对批处理/后台处理使用acquireNextImage（）
                //过多的调用acquireLatestImage()（大于getMaxImages()），而没有调用Image.close()的话，将会抛出IllegalStateException
                image = it.acquireNextImage()
                width = image.width
                height = image.height
                planes = image.planes
                buffer = planes[0].buffer
                pixelStride = planes[0].pixelStride
                rowStride = planes[0].rowStride
                rowPadding = rowStride - pixelStride * width
                bitmap = Bitmap.createBitmap(width + rowPadding / pixelStride, height, Bitmap.Config.ARGB_8888)
                bitmap.copyPixelsFromBuffer(buffer)
                bitmap = Bitmap.createBitmap(bitmap, 0, 0, width, height)
                imageView?.post { imageView?.setImageBitmap(bitmap) }
                image.close()
            } catch (e: Exception) {
                println(e)
            }
        }, null)
    }

    @SuppressLint("ClickableViewAccessibility", "InflateParams")
    override fun showFreeFormWindow() {
        if (Settings.canDrawOverlays(service)) {
            freeFormLayout = LayoutInflater.from(service).inflate(R.layout.view_freeform_imagereader, null, false)
            swingView = freeFormLayout!!.findViewById(R.id.view_swing)
            imageView = freeFormLayout!!.findViewById(R.id.imageView)
            closeView = freeFormLayout!!.findViewById(R.id.view_close)

            swingView!!.setOnTouchListener(TouchListener(R.id.imageView, imageView!!))
            imageView!!.setOnTouchListener(TouchListener(R.id.imageView, imageView!!))
            closeView!!.setOnTouchListener(TouchListener(R.id.imageView, imageView!!))

            addView()

        } else {
            service.stopSelf()
        }
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

        //更改imageReader配置
        //首先释放之前的对象
        imageReader?.close()

        //重新开启新的imageReader，并且不要忘记设置vd的surface!!!
        initImageReader()
        virtualDisplay!!.surface = imageReader?.surface
    }

    /**
     * 释放该小窗
     * 这个十分重要，经过排查，如果不释放mediaCodec，会导致cpu居高不下，会导致设备异常卡顿
     */
    override fun destroy() {
        windowManager?.removeView(freeFormLayout)
        virtualDisplay?.release()
        imageReader?.close()

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