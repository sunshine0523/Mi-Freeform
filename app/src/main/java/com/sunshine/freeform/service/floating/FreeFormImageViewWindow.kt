//package com.sunshine.freeform.service.floating
//
//import android.annotation.SuppressLint
//import android.app.ActivityManager
//import android.app.Service
//import android.content.Context
//import android.graphics.Bitmap
//import android.graphics.PixelFormat
//import android.graphics.SurfaceTexture
//import android.hardware.display.DisplayManager
//import android.media.Image
//import android.media.ImageReader
//import android.provider.Settings
//import android.util.DisplayMetrics
//import android.view.*
//import android.widget.ImageView
//import com.sunshine.freeform.R
//import com.sunshine.freeform.service.floating.FreeFormConfig.dpi
//import com.sunshine.freeform.service.floating.FreeFormConfig.freeFormViewSet
//import com.sunshine.freeform.utils.ShellUtils
//import java.nio.ByteBuffer
//
//
///**
// * @author sunshine
// * @date 2021/2/15
// * 小窗界面，采用ImageReader显示画面
// */
//class FreeFormImageViewWindow(
//        private val service: Service,
//        private val command: String,
//        packageName: String /*用于启动应用*/) : FreeFormWindow(service, packageName) {
//
//    //主界面
//    private var imageView: ImageView? = null
//
//    //画面接收
//    private var imageReader: ImageReader? = null
//
//    override fun initDisplay() {
//        initImageReader()
//
//
//
//        println("FreeFormWindow initEncode displayId $virtualDisplay")
//    }
//
//    @SuppressLint("WrongConstant")
//    private fun initImageReader() {
//        imageReader = ImageReader.newInstance(WIDTH, HEIGHT, PixelFormat.RGBA_8888, 1)
//
//        var width: Int
//        var height: Int
//        var planes: Array<Image.Plane>
//        var buffer: ByteBuffer
//        var pixelStride: Int
//        var rowStride: Int
//        var rowPadding: Int
//        var bitmap: Bitmap
//        var image: Image
//
//        imageReader!!.setOnImageAvailableListener({
//            try {
//                //获取下一个最新的可用Image，没有则返回null
//                //建议对批处理/后台处理使用acquireNextImage（）
//                //过多的调用acquireLatestImage()（大于getMaxImages()），而没有调用Image.close()的话，将会抛出IllegalStateException
//                image = it.acquireNextImage()
//                width = image.width
//                height = image.height
//                planes = image.planes
//                buffer = planes[0].buffer
//                pixelStride = planes[0].pixelStride
//                rowStride = planes[0].rowStride
//                rowPadding = rowStride - pixelStride * width
//                bitmap = Bitmap.createBitmap(width + rowPadding / pixelStride, height, Bitmap.Config.ARGB_8888)
//                bitmap.copyPixelsFromBuffer(buffer)
//                bitmap = Bitmap.createBitmap(bitmap, 0, 0, width, height)
//
//                //沉浸
//                swingView?.setBackgroundColor(bitmap.getPixel(width / 2, 0))
//                closeView?.setBackgroundColor(bitmap.getPixel(width / 2, height - 1))
//
//                imageView?.post { imageView?.setImageBitmap(bitmap) }
//                image.close()
//            } catch (e: Exception) {
//                println(e)
//            }
//        }, null)
//    }
//
//    @SuppressLint("ClickableViewAccessibility", "InflateParams")
//    override fun showFreeFormWindow() {
//        if (Settings.canDrawOverlays(service)) {
//            freeFormLayout = LayoutInflater.from(service).inflate(R.layout.view_freeform_imageview, null, false)
//
//            //获取主界面布局，其实布局完全相同，只是为了减少控制时的if判断
//            val mainId = if (FreeFormConfig.controlModel == 1) R.id.imageView_root else R.id.imageView_xposed
//
//            swingView = freeFormLayout!!.findViewById(R.id.view_swing)
//            imageView = freeFormLayout!!.findViewById(mainId)
//            closeView = freeFormLayout!!.findViewById(R.id.view_close)
//
//            imageView?.visibility = View.VISIBLE
//
//            val textureView = freeFormLayout!!.findViewById<TextureView>(R.id.textureView)
//            textureView.surfaceTextureListener = object : TextureView.SurfaceTextureListener {
//                override fun onSurfaceTextureSizeChanged(
//                    surface: SurfaceTexture,
//                    width: Int,
//                    height: Int
//                ) {
//
//                }
//
//                override fun onSurfaceTextureUpdated(surface: SurfaceTexture) {
//
//                }
//
//                override fun onSurfaceTextureDestroyed(surface: SurfaceTexture): Boolean {
//                    surface.release()
//                    return true
//                }
//
//                override fun onSurfaceTextureAvailable(
//                    surface: SurfaceTexture,
//                    width: Int,
//                    height: Int
//                ) {
//                    val dm = DisplayMetrics()
//                    windowManager!!.defaultDisplay.getMetrics(dm)
//                    println()
//                    this@FreeFormImageViewWindow.surface = Surface(surface)
//                    val displayManager = service.getSystemService(DisplayManager::class.java) as DisplayManager
//                    virtualDisplay = displayManager.createVirtualDisplay("mi-freeform-display-$this", dm.widthPixels, dm.heightPixels, dm.densityDpi, this@FreeFormImageViewWindow.surface, DisplayManager.VIRTUAL_DISPLAY_FLAG_PRESENTATION)
//                    displayId = virtualDisplay!!.display.displayId
//                    imageView!!.setImageBitmap(textureView.bitmap)
//                    //启动程序
//                    ShellUtils.execCommand(command + displayId, true)
//                }
//
//            }
//
//            //监听小窗外部事件，如果点击外部，那么返回键应该响应外部
//            freeFormLayout!!.setOnTouchListener(TouchListener(freeFormLayout!!))
//
//            swingView!!.setOnTouchListener(TouchListener(imageView!!))
//            imageView!!.setOnTouchListener(TouchListener(imageView!!))
//            closeView!!.setOnTouchListener(TouchListener(imageView!!))
//            //点击为模拟返回
//            closeView!!.setOnClickListener(ClickListener())
//
//            addView()
//
//        } else {
//            service.stopSelf()
//        }
//    }
//
//    //屏幕翻转时，应该重新设置小窗大小
//    override fun resize() {
//        //更改宽高
//        setWidthHeight()
//
//        //重新设置vd
//        virtualDisplay!!.resize(WIDTH, HEIGHT, dpi)
//
//        //重新设置layout
//        layoutParams!!.apply {
//            width = WIDTH
//            height = HEIGHT + dip2px(50f)
//        }
//        windowManager!!.updateViewLayout(freeFormLayout, layoutParams)
//
//        //更改imageReader配置
//        //首先释放之前的对象
//        imageReader?.close()
//
//        //重新开启新的imageReader，并且不要忘记设置vd的surface!!!
//        initImageReader()
//        virtualDisplay!!.surface = imageReader?.surface
//    }
//
//    override fun initSmallFreeFormImageReader() {
//
//    }
//
//    /**
//     * 释放该小窗
//     * 这个十分重要，经过排查，如果不释放mediaCodec，会导致cpu居高不下，会导致设备异常卡顿
//     */
//    override fun destroy() {
//        windowManager?.removeView(freeFormLayout)
//        virtualDisplay?.surface?.release()
//        virtualDisplay?.release()
//        imageReader?.close()
//
//        freeFormViewSet.remove(this)
//
//        System.gc()
//    }
//
//    init {
//        //注册
//        freeFormViewSet.add(this)
//        //初始化windowManager，稍后悬浮窗也需要，这里需要获取屏幕宽高，用来确认默认宽高
//        windowManager = service.getSystemService(Service.WINDOW_SERVICE) as WindowManager
//
//        setWidthHeight()
//        showFreeFormWindow()
//
//
//    }
//}