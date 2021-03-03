package com.sunshine.freeform.service.floating

import android.annotation.SuppressLint
import android.content.Context
import android.content.res.Configuration
import android.content.res.Resources
import android.graphics.*
import android.hardware.camera2.CameraManager
import android.hardware.display.DisplayManager
import android.hardware.display.VirtualDisplay
import android.os.*
import android.util.DisplayMetrics
import android.util.Log
import android.view.*
import android.widget.LinearLayout
import android.widget.Toast
import com.sunshine.freeform.R
import com.sunshine.freeform.utils.InputEventUtils
import com.sunshine.freeform.utils.ShellUtils
import kotlin.math.abs

import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

/**
 * @author sunshine
 * @date 2021/2/26
 * 全新设计的小窗界面
 * tips:怎么解决updateView但是textureView却不能更新的问题？起床时突然想起来的，可以new啊
 */
class FreeFormWindow(
    private val context: Context,
    val command: String,
    val packageName: String /*用于启动应用*/
) {
    companion object {
        private const val TAG = "FreeFormWindow"
    }

    private var windowManager: WindowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
    private var displayManager: DisplayManager = context.getSystemService(Context.DISPLAY_SERVICE) as DisplayManager
    private var layoutParams: WindowManager.LayoutParams? = WindowManager.LayoutParams()

    //小窗宽高
    private var WIDTH = 0
    private var HEIGHT = 0

    //屏幕真正宽高，用于小窗挂起显示位置
    private var realScreenWidth = 0
    private var realScreenHeight = 0

    //屏幕大致宽高，请注意，这不是屏幕真正宽高，而是和小窗宽高比相同且和屏幕相近的一个宽高
    private var screenWidth = 0
    private var screenHeight = 0

    //整体布局
    private var freeFormLayout: View? = null
    //子布局，分别位拖动，关闭
    private var swingView: LinearLayout? = null
    private var closeView: LinearLayout? = null
    private var mainId: Int = -1
    //textureView的父布局
    private var textureRootView: LinearLayout? = null
    private var textureView: TextureView? = null
    //真实屏幕和小窗的大小比例
    private var scale = 1.0f

    private var virtualDisplay: VirtualDisplay? = null
    var displayId = -1

    //是否是第一次初始化textureView，如果是的话，需要启动应用，否则就不启动了，因为会弹出root允许
    private var firstStart = true

    //注入输入类
    private val inputEventUtils = InputEventUtils()

    //触摸
    private val touchListener = TouchListener()

    //振动
    private val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
    private val vibrationEffect: VibrationEffect = VibrationEffect.createOneShot(25, 255)

    //小窗内应用的方向
    private var rotation = -1
    //是否已经旋转了，如果不判断会一直转
    private var hasRotation = false

    private val displayListener = object : DisplayManager.DisplayListener {
        override fun onDisplayChanged(displayId: Int) {
            var nowRotation = virtualDisplay?.display?.rotation?:-1
            if (rotation == -1) {
                rotation = nowRotation
            } else {
                //当前小窗内容为横屏时，nowRotation会错误，需要更改
                if (rotation % 2 != 0) {
                    nowRotation = (nowRotation + 1) % 4
                }
                //比如，0到90度需要重新设置，但是0到180不需要设置
                if (abs(rotation - nowRotation) % 2 != 0) {
                    //如果本次没有旋转，那么开始旋转
                    if (!hasRotation) {
                        rotation = nowRotation
                        resize()
                        //旋转完成
                        hasRotation = true
                    }
                } else {
                    //如果rotation和nowRotation是同一方向了，说明旋转完成了
                    hasRotation = false
                }
            }
        }

        override fun onDisplayAdded(displayId: Int) {

        }

        override fun onDisplayRemoved(displayId: Int) {

        }

    }

    @SuppressLint("ClickableViewAccessibility", "InflateParams")
    private fun initView() {
        setWidthHeight(false)

        val displayManager = context.getSystemService(DisplayManager::class.java) as DisplayManager
        virtualDisplay = displayManager.createVirtualDisplay(
            "mi-freeform-display-$this",
            screenWidth,
            screenHeight,
            FreeFormConfig.dpi,
            null,
            DisplayManager.VIRTUAL_DISPLAY_FLAG_PRESENTATION
        )
        displayId = virtualDisplay!!.display.displayId
        displayManager.registerDisplayListener(displayListener, null)

        freeFormLayout = LayoutInflater.from(context).inflate(R.layout.view_freeform_window, null, false)
        textureRootView = freeFormLayout!!.findViewById(R.id.texture_root)
        mainId = if (FreeFormConfig.controlModel == 1) R.id.textureView_root else R.id.textureView_xposed

        updateTextureView()

        swingView = freeFormLayout!!.findViewById(R.id.view_swing)
        closeView = freeFormLayout!!.findViewById(R.id.view_close)

        swingView!!.setOnTouchListener(touchListener)
        closeView!!.setOnTouchListener(touchListener)

        //在android 10上有打开闪光灯问题，暂时不支持返回
        if (Build.VERSION.SDK_INT > 29) {
            closeView!!.setOnClickListener(ClickListener())
        }

        // 设置LayoutParam
        layoutParams!!.apply {
            type = WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            width = WIDTH
            height = HEIGHT + 25f.dp2px()
            //设置这个在点击外部时响应外部操作，如果设置了FLAG_NOT_FOCUSABLE，悬浮窗不会躲避输入法，但是同时设置FLAG_ALT_FOCUSABLE_IM就都正常了，不设置FLAG_NOT_FOCUSABLE，会产生断触
            flags = WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_ALT_FOCUSABLE_IM// or WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED// or WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS//
            format = PixelFormat.RGBA_8888
            gravity = Gravity.CENTER_VERTICAL
            x = WindowManager.LayoutParams.WRAP_CONTENT
            y = WindowManager.LayoutParams.WRAP_CONTENT
        }

        show()
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun updateTextureView() {
        //移除之前的view
        textureRootView?.removeView(textureView)
        textureView = null

        textureView = TextureView(context)
        textureView!!.id = mainId
        val layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
        layoutParams.apply {
            width = screenWidth
            height = screenHeight
        }
        val matrix = Matrix()
        matrix.postScale(1 / scale, 1 / scale, 0.0f, 0.0f)
        textureView!!.setTransform(matrix)
        textureView!!.setOnTouchListener(touchListener)

        textureRootView?.addView(textureView, layoutParams)

        textureView!!.surfaceTextureListener = object : TextureView.SurfaceTextureListener {
            override fun onSurfaceTextureSizeChanged(
                surface: SurfaceTexture,
                width: Int,
                height: Int
            ) {

            }

            override fun onSurfaceTextureUpdated(surface: SurfaceTexture) {

            }

            override fun onSurfaceTextureDestroyed(surface: SurfaceTexture): Boolean {
                surface.release()
                return true
            }

            //SurfaceTexture初始化完成后开始显示界面
            override fun onSurfaceTextureAvailable(
                surface: SurfaceTexture,
                width: Int,
                height: Int
            ) {
                virtualDisplay?.resize(screenWidth, screenHeight, FreeFormConfig.dpi)
                virtualDisplay?.surface = Surface(surface)

                if (firstStart) {
//                    val activityOptions = ActivityOptions.makeBasic()
//                    activityOptions.launchDisplayId = displayId
//                    val packageManager = context.packageManager
//                    val intent = packageManager.getLaunchIntentForPackage(packageName)
//                    if (intent != null) {
//                        context.startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK), activityOptions.toBundle())
//                    }
                    ShellUtils.execCommand(command + displayId, true)
                    firstStart = false
                }
            }
        }
    }

    private fun show() {
        windowManager.addView(freeFormLayout, layoutParams)
    }

    /**
     * 设置小窗和获取屏幕的宽高 dpi
     * @param smallModel 挂起模式
     */
    private fun setWidthHeight(smallModel: Boolean) {
        val point = Point()
        val dm = DisplayMetrics()
        windowManager.defaultDisplay.getSize(point)
        windowManager.defaultDisplay.getMetrics(dm)

        if (FreeFormConfig.orientation == Configuration.ORIENTATION_LANDSCAPE) {
            realScreenWidth = max(point.x, point.y)
            realScreenHeight = min(point.x, point.y)
        } else {
            realScreenWidth = min(point.x, point.y)
            realScreenHeight = max(point.x, point.y)
        }

        if (smallModel) {
            WIDTH = 108 * 2
            HEIGHT = 192 * 2
        }
        else {
            //设置小窗宽高
            val sp = context.getSharedPreferences("com.sunshine.freeform_preferences", Context.MODE_PRIVATE)

            //横屏
            if (FreeFormConfig.orientation == Configuration.ORIENTATION_LANDSCAPE) {
                //横屏状态，默认宽为屏幕1/4，高为屏幕80%
                val saveWidth = sp.getInt("width_land", -1)
                val saveHeight = sp.getInt("height_land", -1)
                //横屏宽高反过来了
                WIDTH = if (saveWidth == -1) realScreenWidth / 3 else saveWidth
                HEIGHT = if (saveHeight == -1) (realScreenHeight * 0.7).roundToInt() else saveHeight
            } else {
                //竖屏状态，默认宽为屏幕60%，高为屏幕40%
                val saveWidth = sp.getInt("width", -1)
                val saveHeight = sp.getInt("height", -1)
                WIDTH = if (saveWidth == -1) (realScreenWidth * 0.65).roundToInt() else saveWidth
                HEIGHT = if (saveHeight == -1) (realScreenHeight * 0.5).roundToInt() else saveHeight
            }
        }
        //设置和小窗宽高比相同的屏幕大小
        scale =  realScreenHeight.toFloat() / HEIGHT
        scale = min(scale,  realScreenWidth.toFloat() / WIDTH)

        //横屏
        if (rotation != -1 && rotation % 2 != 0) {
            val temp = WIDTH
            WIDTH = HEIGHT
            HEIGHT = temp
        }
        screenHeight = (HEIGHT * scale).roundToInt()
        screenWidth = (WIDTH * scale).roundToInt()

        //设置dpi
        FreeFormConfig.dpi = dm.densityDpi
    }

    /**
     * kotlin扩展类
     */
    private fun Float.dp2px(): Int {
        val scale = Resources.getSystem().displayMetrics.density
        return (this * scale + 0.5f).toInt()
    }

    fun resize() {
        setWidthHeight(false)
        layoutParams!!.apply {
            width = WIDTH
            height = HEIGHT + 25f.dp2px()
        }
        windowManager.updateViewLayout(freeFormLayout, layoutParams)
        updateTextureView()
    }

    fun destroy() {
        displayManager.unregisterDisplayListener(displayListener)
        windowManager.removeView(freeFormLayout)
        virtualDisplay?.surface?.release()
        virtualDisplay?.release()

        FreeFormConfig.freeFormViewSet.remove(this)
    }

    init {
        if (FreeFormConfig.hasFreeFormWindow(packageName)) {
            Toast.makeText(context, context.getString(R.string.already_show), Toast.LENGTH_SHORT).show()
        } else {
            FreeFormConfig.freeFormViewSet.add(this)
            initView()
        }
    }

    inner class ClickListener : View.OnClickListener {
        override fun onClick(v: View?) {
            if (v?.id == R.id.view_close) {
                vibrator.vibrate(vibrationEffect)
                //xposed模式
                if (FreeFormConfig.controlModel == 2) {
                    val down = KeyEvent(SystemClock.uptimeMillis(), SystemClock.uptimeMillis(), KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_BACK, 0)
                    val up = KeyEvent(SystemClock.uptimeMillis(), SystemClock.uptimeMillis(), KeyEvent.ACTION_UP, KeyEvent.KEYCODE_BACK, 0)

                    val setSourceMethod = KeyEvent::class.java.getMethod("setSource", Int::class.javaPrimitiveType)
                    val setFlagsMethod = KeyEvent::class.java.getMethod("setFlags", Int::class.javaPrimitiveType)
                    setSourceMethod.invoke(down, InputDevice.SOURCE_KEYBOARD)
                    setSourceMethod.invoke(up, InputDevice.SOURCE_KEYBOARD)
                    setFlagsMethod.invoke(down, 0x48)
                    setFlagsMethod.invoke(up, 0x48)
                    inputEventUtils.xposedInjectKeyEvent(down, displayId)
                    inputEventUtils.xposedInjectKeyEvent(up, displayId)
                }
                //root模式
                else {
                    inputEventUtils.rootInjectKeyEvent(displayId)
                }
            }
        }
    }

    /**
     * 触摸监听类
     */
    inner class TouchListener : View.OnTouchListener {
        //小窗滑动和关闭
        private var x = 0.0f
        private var y = 0.0f

        //小窗关闭用
        private var closeMoveY = 0
        //第一次小于-400则代表关闭，振动提示
        private var firstSmaller400 = true
        //第一次大于250则代表全屏
        private var firstBigger250 = true

        //第一次进行小窗振动
        private var smallFreeForm = true

        //是否是移动模式
        private var moveFreeForm = false
        //记录down手势，后期如果不是移动模式就注入
        private var downEvent: MotionEvent? = null

        /**
         * 挂起模式
         */
        @SuppressLint("ClickableViewAccessibility")
        private fun toSmallFreeForm(nowX: Int, nowY: Int) {
            //移动到边缘需要缩小
            if ((nowX <= FreeFormConfig.SMALL_FREEFORM_POSITION || nowX >= realScreenWidth - FreeFormConfig.SMALL_FREEFORM_POSITION) && nowY <= FreeFormConfig.SMALL_FREEFORM_POSITION) {
                //第一次缩小
                if (smallFreeForm) {
                    vibrator.vibrate(vibrationEffect)

                    layoutParams!!.apply {
                        //竖屏
                        if (rotation % 2 == 0) {
                            width = 108 * 2
                            height = 192 * 2
                        } else {
                            width = 192 * 2
                            height = 108 * 2
                        }
                        x = if (nowX <= FreeFormConfig.SMALL_FREEFORM_POSITION) realScreenWidth / -2 else realScreenWidth / 2
                        y = realScreenHeight / -2
                    }
                    setWidthHeight(true)
                    windowManager.updateViewLayout(freeFormLayout, layoutParams)
                    updateTextureView()

                    //最小化模式隐藏拖动和最大最小化界面
                    swingView!!.layoutParams.height = 0
                    closeView!!.layoutParams.height = 0
                    swingView!!.setOnTouchListener(null)
                    closeView!!.setOnTouchListener(null)

                    //最小化只响应恢复点击
                    textureView!!.setOnTouchListener { _, event ->
                        when (event?.action) {
                            MotionEvent.ACTION_DOWN -> {
                                layoutParams!!.apply {
                                    width = WIDTH
                                    height = HEIGHT + 25f.dp2px()
                                    x = WindowManager.LayoutParams.WRAP_CONTENT
                                    y = WindowManager.LayoutParams.WRAP_CONTENT
                                }
                            }
                            //松手时恢复点击事件
                            MotionEvent.ACTION_UP -> {
                                //非最小化模式显示拖动和最大最小化界面
                                swingView!!.layoutParams.height = 25f.dp2px()
                                closeView!!.layoutParams.height = 25f.dp2px()
                                swingView!!.setOnTouchListener(touchListener)
                                closeView!!.setOnTouchListener(touchListener)
                                textureView!!.setOnTouchListener(touchListener)

                                resize()
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

        @SuppressLint("ClickableViewAccessibility")
        override fun onTouch(v: View?, event: MotionEvent?): Boolean {
            when (v!!.id) {
                //滑动改变位置
                R.id.view_swing -> {
                    when (event!!.action) {
                        MotionEvent.ACTION_DOWN -> {
                            //按下时就对x,y初始化位置
                            x = event.rawX
                            y = event.rawY

                            //开启新一轮判断
                            moveFreeForm = false
                            downEvent = MotionEvent.obtain(event)
                        }
                        //移动
                        MotionEvent.ACTION_MOVE -> {
                            val nowX = event.rawX
                            val nowY = event.rawY
                            val movedX = nowX - x
                            val movedY = nowY - y
                            x = nowX
                            y = nowY
                            layoutParams!!.x = layoutParams!!.x + movedX.roundToInt()
                            layoutParams!!.y = layoutParams!!.y + movedY.roundToInt()
                            windowManager.updateViewLayout(freeFormLayout, layoutParams)

                            toSmallFreeForm(nowX.roundToInt(), nowY.roundToInt())

                            //Log.e(TAG, "$movedX $movedY")
                            moveFreeForm = true
                        }
                        MotionEvent.ACTION_UP -> {

                        }
                    }
                    //如果已经抬起了，就可以判断是不是移动模式了
                    if (!moveFreeForm && event.action == MotionEvent.ACTION_UP) {
                        if (mainId == R.id.textureView_root) {
                            inputEventUtils.rootInjectMotionEvent(downEvent, displayId, scale)
                            inputEventUtils.rootInjectMotionEvent(event, displayId, scale)
                        } else {
                            inputEventUtils.xposedInjectMotionEvent(downEvent, displayId, scale)
                            inputEventUtils.xposedInjectMotionEvent(event, displayId, scale)
                        }
                    }
                    return true
                }

                //root模式的界面
                R.id.textureView_root -> {
                    inputEventUtils.rootInjectMotionEvent(event, displayId, scale)
                }
                //xposed的界面
                R.id.textureView_xposed -> {
                    inputEventUtils.xposedInjectMotionEvent(event, displayId, scale)
                }

                R.id.view_close -> {
                    when (event!!.action) {
                        MotionEvent.ACTION_DOWN -> {
                            y = event.rawY
                        }
                        MotionEvent.ACTION_MOVE -> {
                            //如果移动范围大于一定范围，就关闭窗口
                            val nowY = event.rawY
                            closeMoveY = (nowY - y).roundToInt()
                            //向上移动，最小化
                            if (closeMoveY < 0) {
                                freeFormLayout!!.alpha = 1 - (closeMoveY * -1.0f / 1000)
                                //到达可以关闭的点，就振动提示，但是没有达到的话，就设置为假，以便可以再次振动
                                if (closeMoveY <= -400) {
                                    if (firstSmaller400) {
                                        firstSmaller400 = false
                                        vibrator.vibrate(vibrationEffect)
                                    }
                                } else {
                                    firstSmaller400 = true
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
                            when {
                                closeMoveY <= -400 -> {
                                    destroy()
                                }
                                closeMoveY <= 0 -> {
                                    //如果没有达到可以关闭的高度，就恢复不透明
                                    freeFormLayout!!.alpha = 1.0f
                                }
                                closeMoveY >= 250 -> {
                                    destroy()
                                    val intent = context.packageManager.getLaunchIntentForPackage(packageName)
                                    context.startActivity(intent)
                                }
                            }
                        }
                    }
                    //设置为false才能响应clickListener
                    return Build.VERSION.SDK_INT <= 29
                }
            }
            return true
        }
    }
}