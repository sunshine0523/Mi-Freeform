package com.sunshine.freeform.service.floating

import android.annotation.SuppressLint
import android.content.Context
import android.content.res.Configuration
import android.graphics.PixelFormat
import android.graphics.Point
import android.hardware.display.VirtualDisplay
import android.media.ImageReader
import android.os.VibrationEffect
import android.os.Vibrator
import android.view.*
import android.widget.LinearLayout
import com.sunshine.freeform.EventData
import com.sunshine.freeform.R
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

/**
 * @author sunshine
 * @date 2021/2/19
 * 小窗抽象类，负责小窗的一些公共代码
 */
abstract class FreeFormView(private val service: FloatingService, private val packageName: String) {

    //小窗宽高
    protected var WIDTH = 0
    protected var HEIGHT = 0

    //屏幕宽高
    private var screenWidth = 0
    private var screenHeight = 0

    protected var imageReader: ImageReader? = null
    var virtualDisplay: VirtualDisplay? = null
    protected var displayId = -1

    protected var layoutParams: WindowManager.LayoutParams? = null
    var windowManager: WindowManager? = null

    //整体布局
    var freeFormLayout: View? = null

    //子布局，分别位拖动，关闭
    protected var swingView: LinearLayout? = null
    protected var closeView: LinearLayout? = null

    //振动
    protected val vibrator = service.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
    protected val vibrationEffect: VibrationEffect = VibrationEffect.createOneShot(25, 255)

    //第一次进行小窗振动
    private var smallFreeForm = true

    /**
     * 初始化虚拟屏幕
     */
    protected abstract fun initDisplay()

    /**
     * 将dp转换成px
     * @param context
     * @param dpValue
     * @return
     */
    protected fun dip2px(dpValue: Float): Int {
        val scale = service.resources.displayMetrics.density
        return (dpValue * scale + 0.5f).toInt()
    }

    //顶部有白条，应该减去
    private val topPx = dip2px(25f)

    /**
     * 转换成对象传输
     */
    protected fun getEventData(motionEvent: MotionEvent): EventData {
        val count = motionEvent.pointerCount
        val xArray = FloatArray(count)
        val yArray = FloatArray(count)

        for (i in 0 until count) {
            val coords = MotionEvent.PointerCoords()
            motionEvent.getPointerCoords(i, coords)
            xArray[i] = coords.x
            yArray[i] = coords.y - topPx
        }
        return EventData(motionEvent.action, xArray, yArray, motionEvent.deviceId, motionEvent.source, motionEvent.flags, displayId)
    }

    /**
     * 设置小窗和获取屏幕的宽高 dpi
     */
    protected fun setWidthHeight() {
        val point = Point()
        windowManager!!.defaultDisplay.getSize(point)

        //设置小窗宽高
        val sp = service.getSharedPreferences("com.sunshine.freeform_preferences", Context.MODE_PRIVATE)

        if (FreeFormConfig.orientation == Configuration.ORIENTATION_LANDSCAPE) {
            screenWidth = max(point.x, point.y)
            screenHeight = min(point.x, point.y)
        } else {
            screenWidth = min(point.x, point.y)
            screenHeight = max(point.x, point.y)
        }

        //横屏
        if (FreeFormConfig.orientation == Configuration.ORIENTATION_LANDSCAPE) {
            //横屏状态，默认宽为屏幕1/4，高为屏幕80%
            val saveWidth = sp.getInt("width_land", -1)
            val saveHeight = sp.getInt("height_land", -1)
            //横屏宽高反过来了
            WIDTH = if (saveWidth == -1) screenWidth / 3 else saveWidth
            HEIGHT = if (saveHeight == -1) (screenHeight * 0.7).roundToInt() else saveHeight
        } else {
            //竖屏状态，默认宽为屏幕60%，高为屏幕40%
            val saveWidth = sp.getInt("width", -1)
            val saveHeight = sp.getInt("height", -1)
            WIDTH = if (saveWidth == -1) (screenWidth * 0.65).roundToInt() else saveWidth
            HEIGHT = if (saveHeight == -1) (screenHeight * 0.5).roundToInt() else saveHeight
        }

        //设置dpi
        FreeFormConfig.dpi = sp.getInt("dpi", 300)
    }

    /**
     * 将小窗界面添加到wm中
     */
    protected fun addView() {
        layoutParams = WindowManager.LayoutParams()

        // 设置LayoutParam
        layoutParams!!.apply {
            type = WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            width = WIDTH
            height = HEIGHT + dip2px(50f)
            //设置这个在点击外部时响应外部操作，如果设置了FLAG_NOT_FOCUSABLE，悬浮窗不会躲避输入法，所以不能设置
            flags = WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
            format = PixelFormat.RGBA_8888
            gravity = Gravity.CENTER_VERTICAL
            x = WindowManager.LayoutParams.WRAP_CONTENT
            y = WindowManager.LayoutParams.WRAP_CONTENT
        }
        // 将悬浮窗控件添加到WindowManager
        windowManager!!.addView(freeFormLayout, layoutParams)
    }

    /**
     * 设置小窗界面
     */
    protected abstract fun showFreeFormWindow()

    /**
     * 当小窗大小改变时调用此，主要时屏幕翻转
     */
    abstract fun resize()

    /**
     * 设置最小化
     * @param mainId 主界面id
     * @param mainView 显示小窗的主view，在mediaCodec中是textureView，在imageReader中是imageView
     */
    @SuppressLint("ClickableViewAccessibility")
    protected fun toSmallFreeForm(nowX: Int, nowY: Int, mainId: Int, mainView: View) {
        //移动到边缘需要缩小
        if ((nowX <= FreeFormConfig.SMALL_FREEFORM_POSITION || nowX >= screenWidth - FreeFormConfig.SMALL_FREEFORM_POSITION) && nowY <= FreeFormConfig.SMALL_FREEFORM_POSITION) {
            //第一次缩小
            if (smallFreeForm) {
                vibrator.vibrate(vibrationEffect)

                layoutParams!!.apply {
                    width = WIDTH / 2
                    height = (HEIGHT + dip2px(50f)) / 2
                    x = if (nowX <= FreeFormConfig.SMALL_FREEFORM_POSITION) screenWidth / -2 else screenWidth / 2
                    y = screenHeight / -2
                }
                windowManager!!.updateViewLayout(freeFormLayout, layoutParams)

                //最小化模式隐藏拖动和最大最小化界面
                swingView!!.visibility = View.GONE
                closeView!!.visibility = View.GONE
                swingView!!.setOnTouchListener(null)
                closeView!!.setOnTouchListener(null)

                //最小化只响应恢复点击
                mainView.setOnTouchListener { _, event ->
                    when (event?.action) {
                        MotionEvent.ACTION_DOWN -> {
                            layoutParams!!.apply {
                                width = WIDTH
                                height = HEIGHT + dip2px(50f)
//                                x = movedX
//                                y = movedY
                                x = WindowManager.LayoutParams.WRAP_CONTENT
                                y = WindowManager.LayoutParams.WRAP_CONTENT
                            }
                            windowManager!!.updateViewLayout(freeFormLayout, layoutParams)
                        }
                        //松手时恢复点击事件
                        MotionEvent.ACTION_UP -> {
                            //非最小化模式显示拖动和最大最小化界面
                            swingView!!.visibility = View.VISIBLE
                            closeView!!.visibility = View.VISIBLE
                            swingView!!.setOnTouchListener(TouchListener(mainId, mainView))
                            closeView!!.setOnTouchListener(TouchListener(mainId, mainView))
                            mainView.setOnTouchListener(TouchListener(mainId, mainView))
                        }
                    }
                    true
                }
                //第一次点击振动提示
                smallFreeForm = false
            }
        } else {
            smallFreeForm = true
        }
    }

    /**
     * 销毁小窗对象
     */
    abstract fun destroy()

    /**
     * 触摸监听类
     * @param mainId 主界面id
     * @param mainView 主界面view
     */
    inner class TouchListener(private val mainId: Int, private val mainView: View) : View.OnTouchListener{
        //小窗滑动和关闭
        private var x = 0
        private var y = 0

        //小窗关闭用
        private var closeMoveY = 0
        //第一次小于-500则代表关闭，振动提示
        private var firstSmaller500 = true
        //第一次大于250则代表全屏
        private var firstBigger250 = true

        @SuppressLint("ClickableViewAccessibility")
        override fun onTouch(v: View?, event: MotionEvent?): Boolean {
            when (v!!.id) {
                //滑动改变位置
                R.id.view_swing -> {
                    when (event?.action) {
                        MotionEvent.ACTION_DOWN -> {
                            //按下时就对x,y初始化位置
                            x = event.rawX.toInt()
                            y = event.rawY.toInt()
                        }
                        //移动
                        MotionEvent.ACTION_MOVE -> {
                            val nowX = event.rawX.toInt()
                            val nowY = event.rawY.toInt()
                            val movedX = nowX - x
                            val movedY = nowY - y
                            x = nowX
                            y = nowY
                            layoutParams!!.x = layoutParams!!.x + movedX
                            layoutParams!!.y = layoutParams!!.y + movedY
                            windowManager!!.updateViewLayout(freeFormLayout, layoutParams)

                            toSmallFreeForm(nowX, nowY, mainId, mainView)
                        }
                        MotionEvent.ACTION_UP -> {
//                        //这个记录拖动后的位置
//                        movedX = event.rawX.toInt()
//                        movedY = event.rawY.toInt()
                        }
                    }
                }
                mainId -> {
                    FreeFormConfig.handler?.post {
                        try {
                            FreeFormConfig.touchOos!!.writeObject(getEventData(event!!))
                            FreeFormConfig.touchOos!!.flush()
                        } catch (e: Exception) {
                            println("onTouch $e")
                            //仅重启连接不移除所有小窗
                            FreeFormConfig.onDelete(removeAllFreeForm = false)
                            FreeFormConfig.init(null)
                        }
                    }
                }
                R.id.view_close -> {
                    when (event?.action) {
                        MotionEvent.ACTION_DOWN -> {
                            y = event.rawY.toInt()
                            vibrator.vibrate(vibrationEffect)
                        }
                        MotionEvent.ACTION_MOVE -> {
                            //如果移动范围大于一定范围，就关闭窗口
                            val nowY = event.rawY.toInt()
                            closeMoveY = nowY - y
                            //向上移动，最小化
                            if (closeMoveY < 0) {
                                freeFormLayout!!.alpha = 1 - (closeMoveY * -1.0f / 500)
                                //到达可以关闭的点，就振动提示，但是没有达到的话，就设置为假，以便可以再次振动
                                if (closeMoveY <= -500) {
                                    if (firstSmaller500) {
                                        firstSmaller500 = false
                                        vibrator.vibrate(vibrationEffect)
                                    }
                                } else {
                                    firstSmaller500 = true
                                } //end 判断振动
                            } else {
                                freeFormLayout!!.alpha = 1.0f
                                if (closeMoveY >= 250) {
                                    if (firstBigger250) {
                                        firstBigger250 = false
                                        vibrator.vibrate(vibrationEffect)
                                    }
                                } else {
                                    firstBigger250 = true
                                }
                            } //end判断移动
                        }
                        MotionEvent.ACTION_UP -> {
                            //松手后判断是应该全屏打开还是关闭应用
                            if (closeMoveY <= -500) {
                                destroy()
                            } else if (closeMoveY >= 250) {
                                destroy()
                                val intent = service.packageManager.getLaunchIntentForPackage(packageName)
                                service.startActivity(intent)
                            }
                        }
                    }
                }
            }
            return true
        }
    }
}