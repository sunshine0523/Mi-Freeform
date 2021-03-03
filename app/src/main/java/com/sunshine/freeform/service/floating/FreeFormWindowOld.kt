//package com.sunshine.freeform.service.floating
//
//import android.annotation.SuppressLint
//import android.app.Instrumentation
//import android.app.Service
//import android.content.Context
//import android.content.res.Configuration
//import android.graphics.Bitmap
//import android.graphics.PixelFormat
//import android.graphics.Point
//import android.hardware.display.VirtualDisplay
//import android.media.Image
//import android.media.ImageReader
//import android.os.SystemClock
//import android.os.VibrationEffect
//import android.os.Vibrator
//import android.view.*
//import android.view.inputmethod.InputMethodManager
//import android.widget.ImageView
//import android.widget.LinearLayout
//
//import com.sunshine.freeform.R
//import com.sunshine.freeform.callback.BackClickListener
//import com.sunshine.freeform.utils.InputEventUtils
//import com.sunshine.freeform.utils.ShellUtils
//import de.robv.android.xposed.XC_MethodHook
//import java.nio.ByteBuffer
//import java.util.concurrent.TimeUnit
//import kotlin.Exception
//
//import kotlin.math.max
//import kotlin.math.min
//import kotlin.math.roundToInt
//
//
///**
// * @author sunshine
// * @date 2021/2/19
// * 小窗抽象类，负责小窗的一些公共代码
// */
//abstract class FreeFormWindowOld(private val service: Service, private val packageName: String) {
//
//    //小窗宽高
//    protected var WIDTH = 0
//    protected var HEIGHT = 0
//
//    //屏幕宽高
//    private var screenWidth = 0
//    private var screenHeight = 0
//
//    var virtualDisplay: VirtualDisplay? = null
//    //小窗模式的surface
//    protected var surface: Surface? = null
//    protected var displayId = -1
//    //挂起时用的imageReader
//    protected var smallImageReader: ImageReader? = null
//    //挂起时用得imageView
//    protected var smallImageView: ImageView? = null
//    //FreeFormView，监听返回键
//    protected var freeFormView: FreeFormView? = null
//    //主界面
//    protected var textureView: TextureView? = null
//
//    protected var layoutParams: WindowManager.LayoutParams? = null
//    var windowManager: WindowManager? = null
//
//    //整体布局
//    var freeFormLayout: View? = null
//
//
//    //子布局，分别位拖动，关闭
//    protected var swingView: LinearLayout? = null
//    protected var closeView: LinearLayout? = null
//
//    //振动
//    protected val vibrator = service.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
//    protected val vibrationEffect: VibrationEffect = VibrationEffect.createOneShot(50, 255)
//
//    //第一次进行小窗振动
//    private var smallFreeForm = true
//
//    var inputEventUtils = InputEventUtils()
//
//    //返回键监听
//    protected var backKeyListener: BackClickListener? = null
//
//    /**
//     * 将dp转换成px
//     * @param context
//     * @param dpValue
//     * @return
//     */
//    protected fun dip2px(dpValue: Float): Int {
//        val scale = service.resources.displayMetrics.density
//        return (dpValue * scale + 0.5f).toInt()
//    }
//
//    /**
//     * 设置小窗和获取屏幕的宽高 dpi
//     */
//    protected fun setWidthHeight() {
//        val point = Point()
//        windowManager!!.defaultDisplay.getSize(point)
//
//        //设置小窗宽高
//        val sp = service.getSharedPreferences("com.sunshine.freeform_preferences", Context.MODE_PRIVATE)
//
//        if (FreeFormConfig.orientation == Configuration.ORIENTATION_LANDSCAPE) {
//            screenWidth = max(point.x, point.y)
//            screenHeight = min(point.x, point.y)
//        } else {
//            screenWidth = min(point.x, point.y)
//            screenHeight = max(point.x, point.y)
//        }
//
//        //横屏
//        if (FreeFormConfig.orientation == Configuration.ORIENTATION_LANDSCAPE) {
//            //横屏状态，默认宽为屏幕1/4，高为屏幕80%
//            val saveWidth = sp.getInt("width_land", -1)
//            val saveHeight = sp.getInt("height_land", -1)
//            //横屏宽高反过来了
//            WIDTH = if (saveWidth == -1) screenWidth / 3 else saveWidth
//            HEIGHT = if (saveHeight == -1) (screenHeight * 0.7).roundToInt() else saveHeight
//        } else {
//            //竖屏状态，默认宽为屏幕60%，高为屏幕40%
//            val saveWidth = sp.getInt("width", -1)
//            val saveHeight = sp.getInt("height", -1)
//            WIDTH = if (saveWidth == -1) (screenWidth * 0.65).roundToInt() else saveWidth
//            HEIGHT = if (saveHeight == -1) (screenHeight * 0.5).roundToInt() else saveHeight
//        }
//
//        //设置dpi
//        FreeFormConfig.dpi = sp.getInt("dpi", 300)
//    }
//
//    /**
//     * 将小窗界面添加到wm中
//     */
//    protected fun addView() {
//        layoutParams = WindowManager.LayoutParams()
//
//        // 设置LayoutParam
//        layoutParams!!.apply {
//            type = WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
//            width = WIDTH
//            height = HEIGHT + dip2px(50f)
//            //设置这个在点击外部时响应外部操作，如果设置了FLAG_NOT_FOCUSABLE，悬浮窗不会躲避输入法，所以不能设置
//            flags = WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH or WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED or WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS// or WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
//            format = PixelFormat.RGBA_8888
//            gravity = Gravity.CENTER_VERTICAL
//            x = WindowManager.LayoutParams.WRAP_CONTENT
//            y = WindowManager.LayoutParams.WRAP_CONTENT
//        }
//
//        // 将悬浮窗控件添加到WindowManager
//        windowManager!!.addView(freeFormLayout, layoutParams)
//    }
//
//    /**
//     * 设置最小化
//     * @param mainId 主界面id
//     * @param mainView 显示小窗的主view，在mediaCodec中是textureView，在imageReader中是imageView
//     */
//    @SuppressLint("ClickableViewAccessibility")
//    protected fun toSmallFreeForm(nowX: Int, nowY: Int, mainView: View) {
//        //移动到边缘需要缩小
//        if ((nowX <= FreeFormConfig.SMALL_FREEFORM_POSITION || nowX >= screenWidth - FreeFormConfig.SMALL_FREEFORM_POSITION) && nowY <= FreeFormConfig.SMALL_FREEFORM_POSITION) {
//            //第一次缩小
//            if (smallFreeForm) {
//                vibrator.vibrate(vibrationEffect)
//
//                layoutParams!!.apply {
//                    width = 108 * 2//WIDTH / 2
//                    height = 192 * 2//(HEIGHT + dip2px(50f)) / 2
////                    x = if (nowX <= FreeFormConfig.SMALL_FREEFORM_POSITION) screenWidth / -2 else screenWidth / 2
////                    y = screenHeight / -2
//                }
//                windowManager!!.updateViewLayout(freeFormLayout, layoutParams)
//
//                //最小化模式隐藏拖动和最大最小化界面
//                swingView!!.visibility = View.INVISIBLE
//                closeView!!.visibility = View.INVISIBLE
//                swingView!!.setOnTouchListener(null)
//                closeView!!.setOnTouchListener(null)
//
////                //开始显示imageView，非常迷，只有设置了150才可以，正常50就行啊
////                virtualDisplay!!.resize(WIDTH, HEIGHT + dip2px(150f), FreeFormConfig.dpi)
////                virtualDisplay!!.surface = smallImageReader?.surface
////                smallImageView?.visibility = View.VISIBLE
////                mainView.visibility = View.INVISIBLE
//
//                smallImageView?.visibility = View.VISIBLE
//                smallImageView?.setImageBitmap(textureView!!.bitmap)
//                //最小化只响应恢复点击
//                smallImageView?.setOnTouchListener { _, event ->
//                    when (event?.action) {
//                        MotionEvent.ACTION_DOWN -> {
//                            layoutParams!!.apply {
//                                width = WIDTH
//                                height = HEIGHT + dip2px(50f)
//                                x = WindowManager.LayoutParams.WRAP_CONTENT
//                                y = WindowManager.LayoutParams.WRAP_CONTENT
//                            }
//                            windowManager!!.updateViewLayout(freeFormLayout, layoutParams)
//                        }
//                        //松手时恢复点击事件
//                        MotionEvent.ACTION_UP -> {
////                            virtualDisplay!!.resize(WIDTH, HEIGHT, FreeFormConfig.dpi)
////                            virtualDisplay!!.surface = surface
////                            mainView.visibility = View.VISIBLE
////                            smallImageView?.visibility = View.INVISIBLE
//
//
//
//                            //非最小化模式显示拖动和最大最小化界面
//                            swingView!!.visibility = View.VISIBLE
//                            closeView!!.visibility = View.VISIBLE
//                            swingView!!.setOnTouchListener(TouchListener(mainView))
//                            closeView!!.setOnTouchListener(TouchListener(mainView))
//                            mainView.setOnTouchListener(TouchListener(mainView))
//                        }
//                    }
//                    true
//                }
//                //第一次点击振动提示
//                smallFreeForm = false
//            }
//        } else {
//            smallFreeForm = true
//        }
//    }
//
//    /**
//     * 初始化虚拟屏幕
//     */
//    protected abstract fun initDisplay()
//
//    /**
//     * 设置小窗界面
//     */
//    protected abstract fun showFreeFormWindow()
//
//    /**
//     * 当小窗大小改变时调用此，主要时屏幕翻转
//     */
//    abstract fun resize()
//
//    /**
//     * 挂起时小窗用得imageReader
//     */
//    protected abstract fun initSmallFreeFormImageReader()
//
//    /**
//     * 销毁小窗对象
//     */
//    abstract fun destroy()
//
//    inner class ClickListener : View.OnClickListener {
//        override fun onClick(v: View?) {
//            if (v?.id == R.id.view_close) {
//                //xposed模式
//                if (FreeFormConfig.controlModel == 2) {
//                    val down = KeyEvent(SystemClock.uptimeMillis(), SystemClock.uptimeMillis(), KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_BACK, 0)
//                    val up = KeyEvent(SystemClock.uptimeMillis(), SystemClock.uptimeMillis(), KeyEvent.ACTION_UP, KeyEvent.KEYCODE_BACK, 0)
//
//                    KeyEvent::class.java.getMethod("setSource", Int::class.javaPrimitiveType).invoke(down, InputDevice.SOURCE_KEYBOARD)
//                    //KeyEvent::class.java.getMethod("setFlags", Int::class.javaPrimitiveType).invoke(down, 0x8)
//                    inputEventUtils.xposedInjectKeyEvent(down, displayId)
//
//                    KeyEvent::class.java.getMethod("setSource", Int::class.javaPrimitiveType).invoke(up, InputDevice.SOURCE_KEYBOARD)
//                    //KeyEvent::class.java.getMethod("setFlags", Int::class.javaPrimitiveType).invoke(up, 0x8)
//                    inputEventUtils.xposedInjectKeyEvent(up, displayId)
//                }
//                //root模式
//                else {
//                    inputEventUtils.rootInjectKeyEvent(displayId)
//                }
//            }
//        }
//    }
//
//    /**
//     * 触摸监听类
//     * @param mainView 主界面view
//     */
//    inner class TouchListener(private val mainView: View) : View.OnTouchListener {
//        //小窗滑动和关闭
//        private var x = 0
//        private var y = 0
//
//        //小窗关闭用
//        private var closeMoveY = 0
//        //第一次小于-500则代表关闭，振动提示
//        private var firstSmaller500 = true
//        //第一次大于250则代表全屏
//        private var firstBigger250 = true
//
//        @SuppressLint("ClickableViewAccessibility")
//        override fun onTouch(v: View?, event: MotionEvent?): Boolean {
//            //点击了小窗外部
//            if (event!!.action == MotionEvent.ACTION_OUTSIDE) {
//                //点击外部就处理主屏幕返回
//                //设置输入法可以弹出
//                try {
//                    layoutParams?.flags = WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH or WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED or WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS
//                    windowManager?.updateViewLayout(freeFormView, layoutParams)
//                    return false
//                }catch (e: Exception) {}
//
//            }
//
//            try {
//                layoutParams?.flags = WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH or WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED or WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS
//                windowManager?.updateViewLayout(freeFormView, layoutParams)
//            }catch (e: Exception) {}
//
//
//            when (v!!.id) {
//                //滑动改变位置
//                R.id.view_swing -> {
//                    when (event.action) {
//                        MotionEvent.ACTION_DOWN -> {
//                            //按下时就对x,y初始化位置
//                            x = event.rawX.toInt()
//                            y = event.rawY.toInt()
//                        }
//                        //移动
//                        MotionEvent.ACTION_MOVE -> {
//                            val nowX = event.rawX.toInt()
//                            val nowY = event.rawY.toInt()
//                            val movedX = nowX - x
//                            val movedY = nowY - y
//                            x = nowX
//                            y = nowY
//                            layoutParams!!.x = layoutParams!!.x + movedX
//                            layoutParams!!.y = layoutParams!!.y + movedY
//                            windowManager!!.updateViewLayout(freeFormLayout, layoutParams)
//
//                            toSmallFreeForm(nowX, nowY, mainView)
//                        }
//                        MotionEvent.ACTION_UP -> {
////                        //这个记录拖动后的位置
////                        movedX = event.rawX.toInt()
////                        movedY = event.rawY.toInt()
//                        }
//                    }
//                }
//
//                //root模式的界面
//                R.id.imageView_root, R.id.textureView_root -> {
//                    inputEventUtils.rootInjectMotionEvent(event, displayId)
//                }
//                //xposed的界面
//                R.id.imageView_xposed, R.id.textureView_xposed -> {
//                    inputEventUtils.xposedInjectMotionEvent(event, displayId)
//                }
//
//                R.id.view_close -> {
//                    when (event.action) {
//                        MotionEvent.ACTION_DOWN -> {
//                            y = event.rawY.toInt()
//                            vibrator.vibrate(vibrationEffect)
//                        }
//                        MotionEvent.ACTION_MOVE -> {
//                            //如果移动范围大于一定范围，就关闭窗口
//                            val nowY = event.rawY.toInt()
//                            closeMoveY = nowY - y
//                            //向上移动，最小化
//                            if (closeMoveY < 0) {
//                                freeFormLayout!!.alpha = 1 - (closeMoveY * -1.0f / 1000)
//                                //到达可以关闭的点，就振动提示，但是没有达到的话，就设置为假，以便可以再次振动
//                                if (closeMoveY <= -500) {
//                                    if (firstSmaller500) {
//                                        firstSmaller500 = false
//                                        vibrator.vibrate(vibrationEffect)
//                                    }
//                                } else {
//                                    firstSmaller500 = true
//                                } //end 判断振动
//                            } else {
//                                freeFormLayout!!.alpha = 1.0f
//                                if (closeMoveY >= 250) {
//                                    if (firstBigger250) {
//                                        firstBigger250 = false
//                                        vibrator.vibrate(vibrationEffect)
//                                    }
//                                } else {
//                                    firstBigger250 = true
//                                }
//                            } //end判断移动
//                        }
//                        MotionEvent.ACTION_UP -> {
//                            //松手后判断是应该全屏打开还是关闭应用
//                            when {
//                                closeMoveY <= -500 -> {
//                                    destroy()
//                                }
//                                closeMoveY <= 0 -> {
//                                    //如果没有达到可以关闭的高度，就恢复不透明
//                                    freeFormLayout!!.alpha = 1.0f
//                                }
//                                closeMoveY >= 250 -> {
//                                    destroy()
//                                    val intent = service.packageManager.getLaunchIntentForPackage(packageName)
//                                    service.startActivity(intent)
//                                }
//                            }
//                        }
//                    }
//                    //设置为false才能响应clickListener
//                    return false
//                }
//            }
//            return true
//        }
//    }
//}