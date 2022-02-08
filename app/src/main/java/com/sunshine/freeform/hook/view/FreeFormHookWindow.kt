package com.sunshine.freeform.hook.view

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Matrix
import android.graphics.PixelFormat
import android.graphics.Point
import android.graphics.SurfaceTexture
import android.hardware.display.DisplayManager
import android.os.Handler
import android.os.SystemClock
import android.os.VibrationEffect
import android.os.Vibrator
import android.util.DisplayMetrics
import android.view.*
import android.widget.LinearLayout
import android.widget.Space

import com.sunshine.freeform.R
import com.sunshine.freeform.hook.utils.FreeFormHookUtils
import com.sunshine.freeform.hook.utils.HookFailException
import java.io.DataOutputStream
import kotlin.jvm.Throws

import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

/**
 * @author sunshine
 * @date 2021/8/1
 */
class FreeFormHookWindow(
    override val context: Context,
    override val packageName: String,
    override val command: String,
    override val userId: Int
) : FreeFormHookWindowAbs() {

    companion object {
        private const val TAG = "FreeFormHookWindow"

        private const val MIN_SCALE = 0.3f
        private const val MAX_SCALE = 0.9f
        private const val INITIAL_SCALE = 0.75f
        private const val FREEFORM_DEFAULT_WIDTH = 1080
        private const val FREEFORM_DEFAULT_HEIGHT = 1920
        private const val WIDTH_HEIGHT_RATIO = 9 / 16.0f
    }

    private lateinit var freeFormRootView: View
    //private lateinit var controlBarView: View

    private var freeformLayoutParams: WindowManager.LayoutParams = WindowManager.LayoutParams()
    //private var controlBarLayoutParams: WindowManager.LayoutParams = WindowManager.LayoutParams()

    private var dmsUiHandler: Handler? = null
    private var dmsContext: Context? = null

    private val displayName = "$packageName#$userId"

    private var textureRootView: LinearLayout? = null
    private var swingView: View? = null
    private var closeView: View? = null
    private var closeBarView: View? = null
    private var resizeLeftView: View? = null
    private var resizeRightView: View? = null
    private var textureView: TextureView? = null
    private var spaceLeft: Space? = null
    private var spaceRight: Space? = null

    //freeform`size - physical`size ratio
    private var scale = INITIAL_SCALE
    private var freeFormWidth = 0
    private var freeFormHeight = 0
    //屏幕大致宽高，请注意，这不是屏幕真正宽高，而是和小窗宽高比相同且和屏幕相近的一个宽高
    private var screenWidth = 0
    private var screenHeight = 0

    private val touchListener = TouchListener()

    private val displayManager: DisplayManager? = getDmsContext()?.getSystemService(Context.DISPLAY_SERVICE) as DisplayManager
    private val windowManager: WindowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager

    private val sp = context.getSharedPreferences("com.sunshine.freeform_preferences", Context.MODE_PRIVATE)

    private val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
    private val vibrationEffect: VibrationEffect? =
        if (sp.getBoolean("vibrator", true)) VibrationEffect.createOneShot(25, 255)
        else null

    private val controlBarHeight = 30f.dp2px()

    private val displayListener = object : DisplayManager.DisplayListener {
        override fun onDisplayChanged(displayId: Int) {

        }

        override fun onDisplayAdded(displayId: Int) {

        }

        override fun onDisplayRemoved(displayId: Int) {

        }
    }

    override var displayId: Int = -1

    @SuppressLint("ClickableViewAccessibility")
    @Throws(HookFailException::class)
    private fun initView() {
        freeFormRootView = LayoutInflater.from(context).inflate(R.layout.view_freeform_hook_window, null, false)
        //controlBarView = LayoutInflater.from(context).inflate(R.layout.view_freeform_control_bar, null, false)

        textureRootView = freeFormRootView.findViewById(R.id.texture_root)
        swingView = freeFormRootView.findViewById(R.id.view_swing)
        closeView = freeFormRootView.findViewById(R.id.view_close)
        closeBarView = freeFormRootView.findViewById(R.id.view_close_bar)
        //resizeLeftView = controlBarView.findViewById(R.id.view_resize_left)
        //resizeRightView = controlBarView.findViewById(R.id.view_resize_right)
        //spaceLeft = freeFormRootView.findViewById(R.id.space_left)
        //spaceRight = freeFormRootView.findViewById(R.id.space_right)
        swingView!!.setOnTouchListener(touchListener)
        closeView!!.setOnTouchListener(touchListener)
        //resizeLeftView!!.setOnTouchListener(touchListener)
        //resizeRightView!!.setOnTouchListener(touchListener)

        setWidthHeight()
        initTextureView()

        freeformLayoutParams.apply {
            type = WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            width = freeFormWidth
            height = freeFormHeight
            //设置这个在点击外部时响应外部操作，如果设置了FLAG_NOT_FOCUSABLE，悬浮窗不会躲避输入法，但是同时设置FLAG_ALT_FOCUSABLE_IM就都正常了，不设置FLAG_NOT_FOCUSABLE，会产生断触
            flags = WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_ALT_FOCUSABLE_IM// or WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED// or WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS//
            format = PixelFormat.RGBA_8888
            x = 0
            y = 0

//            if (FreeFormUtils.orientation == Configuration.ORIENTATION_LANDSCAPE) {
//                x = sp.getInt("freeform_window_x_landscape", 0)
//                y = sp.getInt("freeform_window_y_landscape", 0)
//            } else {
//                x = sp.getInt("freeform_window_x_portrait", 0)
//                y = sp.getInt("freeform_window_y_portrait", 0)
//            }
        }

//        controlBarLayoutParams.apply {
//            type = WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
//            width = freeFormWidth
//            height = WindowManager.LayoutParams.WRAP_CONTENT
//            flags = WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_ALT_FOCUSABLE_IM
//            format = PixelFormat.RGBA_8888
//            x = 0
//            y = freeFormHeight / 2 + controlBarHeight
//        }

        show()
    }

    @SuppressLint("ClickableViewAccessibility")
    @Throws(HookFailException::class)
    private fun initTextureView() {
        textureRootView?.removeView(textureView)
        textureView = null

        textureView = TextureView(context)
        textureView!!.id = R.id.texture_view
        val layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
        layoutParams.apply {
            width = screenWidth
            height = screenHeight
        }
        val matrix = Matrix()
        matrix.postScale(scale, scale, 0.0f, 0.0f)
        textureView!!.setTransform(matrix)
        textureView!!.setOnTouchListener(touchListener)

        textureRootView?.addView(textureView, layoutParams)

        textureView!!.surfaceTextureListener = object : TextureView.SurfaceTextureListener {
            override fun onSurfaceTextureSizeChanged(
                surface: SurfaceTexture,
                width: Int,
                height: Int
            ) {}

            override fun onSurfaceTextureUpdated(surface: SurfaceTexture) {}

            override fun onSurfaceTextureDestroyed(surface: SurfaceTexture): Boolean {
                surface.release()
                return true
            }

            //SurfaceTexture初始化完成后开始显示界面
            override fun onSurfaceTextureAvailable(
                surfaceTexture: SurfaceTexture,
                width: Int,
                height: Int
            ) {
                addOverlayDevice(
                    surfaceTexture,
                    FreeFormHookUtils.freeFormViewSet.size,
                    displayName,
                    screenWidth,
                    screenHeight,
                    FreeFormHookUtils.dpi
                )
            }
        }
    }

    @Throws(HookFailException::class)
    override fun show() {
        val handler = getDmsUiHandler()
        if (null == handler) {
            //throw HookFailException()
        } else {
            handler.post {
                windowManager.addView(freeFormRootView, freeformLayoutParams)
                //windowManager.addView(controlBarView, controlBarLayoutParams)
            }
        }
    }

    @Throws(HookFailException::class)
    override fun moveToTop(): Boolean {
        if (FreeFormHookUtils.displayIdStackSet.size() <= 0) return false
        if (FreeFormHookUtils.displayIdStackSet.peek() != displayId) {
            FreeFormHookUtils.displayIdStackSet.push(displayId)
            return true
        }
        return false
    }

    /**
     * 设置小窗和获取屏幕的宽高 dpi
     * 原理：屏幕宽高一定，让小窗的宽高比
     */
    @Throws(HookFailException::class)
    override fun setWidthHeight() {
        val point = Point()
        val dm = DisplayMetrics()
        windowManager.defaultDisplay.getSize(point)
        windowManager.defaultDisplay.getMetrics(dm)

        val realScreenWidth = min(point.x, point.y)
        val realScreenHeight = max(point.x, point.y)

        //choose smaller w&h
        if ((realScreenWidth / realScreenHeight).toFloat() >= WIDTH_HEIGHT_RATIO) {
            freeFormHeight = (realScreenHeight * INITIAL_SCALE).roundToInt()
            freeFormWidth = (freeFormHeight * WIDTH_HEIGHT_RATIO).roundToInt()
        } else {
            freeFormWidth = (realScreenWidth * INITIAL_SCALE).roundToInt()
            freeFormHeight = (freeFormWidth / WIDTH_HEIGHT_RATIO).roundToInt()
        }
        //freeFormHeight -= closebarHeight
        screenWidth = (freeFormWidth / scale).roundToInt()
        screenHeight = (freeFormHeight / scale).roundToInt()

        //old------------------
//
////        if (FreeFormHookUtils.orientation == Configuration.ORIENTATION_LANDSCAPE) {
////            realScreenWidth = max(point.x, point.y)
////            realScreenHeight = min(point.x, point.y)
////        } else {
//            realScreenWidth = min(point.x, point.y)
//            realScreenHeight = max(point.x, point.y)
////        }
//
////        if (smallModelGlobal) {
////            WIDTH = 108 * smallFreeFormSize / 10 + 32f.dp2px()
////            HEIGHT = 192 * smallFreeFormSize / 10 + 40f.dp2px()
////        }
////        else {
//            //横屏
//            if (FreeFormHookUtils.orientation == Configuration.ORIENTATION_LANDSCAPE) {
//                //横屏状态，默认宽为屏幕1/4，高为屏幕80%
//                val saveWidth = sp.getInt("width_land", -1)
//                val saveHeight = sp.getInt("height_land", -1)
//
//                freeFormWidth = if (saveWidth == -1) realScreenWidth / 3 else saveWidth
//                freeFormHeight = if (saveHeight == -1) (realScreenHeight * 0.7).roundToInt() else saveHeight
//            } else {
//                //竖屏状态，默认宽为屏幕60%，高为屏幕40%
//                val saveWidth = sp.getInt("width", -1)
//                val saveHeight = sp.getInt("height", -1)
//                freeFormWidth = if (saveWidth == -1) (realScreenWidth * 0.65).roundToInt() else saveWidth
//                freeFormHeight = if (saveHeight == -1) (realScreenHeight * 0.5).roundToInt() else saveHeight
//            }
////        }
//
//        //如果宽大于高，那么不允许
//        if (freeFormWidth > freeFormHeight) {
//            freeFormWidth = freeFormHeight
//        }
//
////        //横屏
////        if (smallRotation != -1 && smallRotation % 2 != 0) {
////            val temp = WIDTH
////            WIDTH = HEIGHT
////            HEIGHT = temp
////
////            //防止宽度比屏幕宽度高
////            if (WIDTH > realScreenWidth - 50) {
////                WIDTH = realScreenWidth - 50
////            }
////        }
//
//        //为什么减64dp和48dp，因为cardview有偏差，这个偏差导致实际显示的宽度小64dp，高度小48dp
//        //设置和小窗宽高比相同的屏幕大小
//        scale =  realScreenHeight.toFloat() / (freeFormHeight - 40f.dp2px())
//        scale = min(scale,  realScreenWidth.toFloat() / (freeFormWidth - 32f.dp2px()))
//
//        screenHeight = ((freeFormHeight - 40f.dp2px()) * scale).roundToInt()
//        screenWidth = ((freeFormWidth - 32f.dp2px()) * scale).roundToInt()

        //设置dpi
        FreeFormHookUtils.dpi = dm.densityDpi
    }

    @Throws(HookFailException::class)
    override fun resize() {
        //横竖屏切换时挂起的会恢复，同时也可以做正常挂起的恢复
//        if (smallModelGlobal) {
//            swingView!!.visibility = View.VISIBLE
//            closeView!!.visibility = View.VISIBLE
//            swingView!!.setOnTouchListener(touchListener)
//            closeView!!.setOnTouchListener(touchListener)
//            textureView!!.setOnTouchListener(touchListener)
//            resizeLeftView!!.setOnTouchListener(touchListener)
//            resizeRightView!!.setOnTouchListener(touchListener)
//
//            if (controlModel != "1") {
//                //恢复小横条
//                closeBarView?.visibility = View.VISIBLE
//            }
//            smallModelGlobal = false
//        }

        setWidthHeight()
        freeformLayoutParams.apply {
            width = freeFormWidth
            height = freeFormHeight
            x = 0
            y = 0
        }

        updateViewLayout()
        //updateTextureView()
    }

    @Throws(HookFailException::class)
    override fun addOverlayDevice(surfaceTexture: SurfaceTexture, freeFormCount: Int, displayName: String, screenWidth: Int, screenHeight: Int, dpi: Int) {
        FreeFormHookUtils.freeFormViewSet.add(this@FreeFormHookWindow)
        FreeFormHookUtils.displayIdStackSet.push(displayId)

        if (null == displayManager) {
            destroy()
        } else {
            Thread {
                while (true) {
                    for (i in displayManager.displays.size - 1 downTo 0) {
                        if (displayManager.displays[i].name == displayName) {
                            displayId = displayManager.displays[i].displayId
                            break
                        }
                    }
                    if (-1 != displayId) break
                    Thread.sleep(1000)
                }

                if (-1 == displayId) {
                    destroy()
                    //throw HookFailException()
                } else {
                    if (!startActivity("$command $displayId")) {
                        destroy()
                        //throw HookFailException()
                    }
                }
            }.start()
        }
    }

    @Throws(HookFailException::class)
    override fun resizeFreeForm(movedX: Float, movedY: Float, position: Int) {
        var tempWidth = if (position == 0) freeFormWidth - movedX else freeFormWidth + movedX
        var tempHeight = freeFormHeight + movedY

        val tempWidthScale = tempWidth / screenWidth
        val tempHeightScale = tempHeight / screenHeight
        if (tempWidthScale < MIN_SCALE || tempHeightScale < MIN_SCALE || tempWidthScale > MAX_SCALE || tempHeightScale > MAX_SCALE) return

        //keep equal width-height ratio
        if (tempWidth / tempHeight > WIDTH_HEIGHT_RATIO) tempHeight = tempWidth / WIDTH_HEIGHT_RATIO
        else tempWidth = tempHeight * WIDTH_HEIGHT_RATIO

        freeFormHeight = tempHeight.roundToInt()
        freeFormWidth = tempWidth.roundToInt()

        scale = freeFormHeight / screenHeight.toFloat()

        freeformLayoutParams.apply {
            width = freeFormWidth
            height = freeFormHeight
        }

//        controlBarLayoutParams.apply {
//            width = freeFormWidth
//            x = freeformLayoutParams.x
//            y = freeformLayoutParams.y / 2 + controlBarHeight
//        }

        val matrix = Matrix()
        matrix.postScale(scale, scale, 0.0f, 0.0f)
        textureView!!.setTransform(matrix)

        updateViewLayout()
    }

    fun getDmsUiHandler(): Handler? {
        return dmsUiHandler
    }

    fun getDmsContext(): Context? {
        return dmsContext
    }

    @Throws(HookFailException::class)
    override fun updateViewLayout() {
        val handler = getDmsUiHandler()
        if (null == handler) {
            //throw HookFailException()
        } else {
            handler.post {
                windowManager.updateViewLayout(freeFormRootView, freeformLayoutParams)
                //windowManager.updateViewLayout(controlBarView, controlBarLayoutParams)
            }
        }
    }

    override fun injectEvent(inputEvent: InputEvent, displayId: Int) {

    }

    override fun startActivity(command: String) : Boolean {
        return try {
            val process = Runtime.getRuntime().exec("sh")
            val os = DataOutputStream(process.outputStream)
            os.writeBytes(command + "\n")
            os.writeBytes("exit\n")
            os.flush()

            true
        }catch (e: Exception) {
            false
        }
    }

    override fun exitSmall() {

    }

    @Throws(HookFailException::class)
    override fun removeView() {
        val handler = getDmsUiHandler()
        if (null == handler) {
            //throw HookFailException()
        } else {
            handler.post {
                windowManager.removeView(freeFormRootView)
                //windowManager.removeView(controlBarView)
            }
        }
    }

    @Throws(HookFailException::class)
    override fun killApp(packageName: String) {
        destroy()
    }

    @Throws(HookFailException::class)
    override fun destroy() {
        //view在屏幕中的位置，用于记录
        //val locations = IntArray(2)

        //freeFormRootView.getLocationOnScreen(locations)

//        // 21/3/25 为了防止下移，只有更新时才需要重新设置
//        if (isMoved) {
//            // 21/3/22 为了防止下移，要减去35fdp2px，为什么是这个...
//            sp.edit().apply {
//                if (FreeFormUtils.orientation == Configuration.ORIENTATION_LANDSCAPE) {
//                    putInt(
//                        "freeform_window_x_landscape",
//                        locations[0] - realScreenWidth / 2 + WIDTH / 2
//                    )
//                    putInt(
//                        "freeform_window_y_landscape",
//                        locations[1] - realScreenHeight / 2 + HEIGHT / 2 - 35f.dp2px()
//                    )
//                } else {
//                    putInt(
//                        "freeform_window_x_portrait",
//                        locations[0] - realScreenWidth / 2 + WIDTH / 2
//                    )
//                    putInt(
//                        "freeform_window_y_portrait",
//                        locations[1] - realScreenHeight / 2 + HEIGHT / 2 - 35f.dp2px()
//                    )
//                }
//                apply()
//            }
//        }

        //displayManager.unregisterDisplayListener(displayListener)
        removeView()

        FreeFormHookUtils.freeFormViewSet.remove(this)
        FreeFormHookUtils.displayIdStackSet.pop()
    }

    init {
        initView()
    }

    inner class TouchListener : View.OnTouchListener {
        //小窗滑动和关闭
        private var x = 0.0f
        private var y = 0.0f

        //用于改变小窗大小记录的坐标信息
        private var resizeX = 0.0f
        private var resizeY = 0.0f

        //小窗关闭用
        private var closeMoveY = 0
        //第一次小于-400则代表关闭，振动提示
        private var firstSmaller400 = true
        //第一次大于250则代表全屏
        private var firstBigger250 = true

        //第一次进行小窗振动
        private var smallFreeForm = true

        @SuppressLint("ClickableViewAccessibility")
        override fun onTouch(v: View?, event: MotionEvent?): Boolean {
            if (moveToTop()) return false
            when (v!!.id) {
                //滑动改变位置
                R.id.view_swing -> {
                    when (event!!.action) {
                        MotionEvent.ACTION_DOWN -> {
                            //按下时就对x,y初始化位置
                            x = event.rawX
                            y = event.rawY
                        }
                        //移动
                        MotionEvent.ACTION_MOVE -> {
                            val nowX = event.rawX
                            val nowY = event.rawY

                            val movedX = nowX - x
                            val movedY = nowY - y
                            x = nowX
                            y = nowY
                            freeformLayoutParams.x = freeformLayoutParams.x + movedX.roundToInt()
                            freeformLayoutParams.y = freeformLayoutParams.y + movedY.roundToInt()

                            //controlBarLayoutParams.x = controlBarLayoutParams.x + movedX.roundToInt()
                            //controlBarLayoutParams.y = controlBarLayoutParams.y + movedY.roundToInt()

                            updateViewLayout()

                            //toSmallFreeForm(nowX.roundToInt(), nowY.roundToInt())

                            //isMoved = true
                        }
                    }
                    return true
                }

                //触控
                R.id.texture_view -> {
//                    val count = event!!.pointerCount
//                    val pointerProperties: Array<MotionEvent.PointerProperties?> = arrayOfNulls(count)
//                    val pointerCoords: Array<MotionEvent.PointerCoords?> = arrayOfNulls(count)
//                    for (i in 0 until count) {
//                        pointerProperties[i] = MotionEvent.PointerProperties()
//                        event.getPointerProperties(i, pointerProperties[i])
//
//                        pointerCoords[i] = MotionEvent.PointerCoords()
//                        event.getPointerCoords(i, pointerCoords[i])
//                        pointerCoords[i]!!.apply {
//                            x /= scale
//                            y /= scale
//                        }
//                    }
//
//                    val mMotionEvent = MotionEvent.obtain(
//                        SystemClock.uptimeMillis(),
//                        SystemClock.uptimeMillis(),
//                        event.action,
//                        count,
//                        pointerProperties,
//                        pointerCoords,
//                        0,
//                        0,
//                        1.0f,
//                        1.0f,
//                        -1,
//                        0,
//                        InputDevice.SOURCE_TOUCHSCREEN,
//                        0
//                    )

                    val mMotionEvent = MotionEvent.obtain(
                        SystemClock.uptimeMillis(),
                        SystemClock.uptimeMillis(),
                        event!!.action,
                        event.x,
                        event.y,
                        0
                    )
                    injectEvent(mMotionEvent, displayId)
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
                                freeFormRootView.alpha = 1 - (closeMoveY * -1.0f / 1000)
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
                                freeFormRootView.alpha = 1.0f
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
                                    killApp(packageName)
                                }
                                closeMoveY <= 0 -> {
                                    //如果没有达到可以关闭的高度，就恢复不透明
                                    freeFormRootView.alpha = 1.0f
                                }
                                closeMoveY >= 250 -> {
                                    destroy()
                                }
                            }
                        }
                    }
                    //设置为false才能响应clickListener
                    //return false
                }

                R.id.view_resize_left, R.id.view_resize_right -> {
                    when (event?.action) {
                        MotionEvent.ACTION_DOWN -> {
                            resizeX = event.rawX
                            resizeY = event.rawY
                        }
                        MotionEvent.ACTION_MOVE -> {
                            val nowX = event.rawX
                            val nowY = event.rawY
                            val movedX = nowX - resizeX
                            val movedY = nowY - resizeY
                            resizeFreeForm(movedX, movedY, if (v.id == R.id.view_resize_left) 0 else 1)
                            resizeX = nowX
                            resizeY = nowY
                        }
                        MotionEvent.ACTION_UP -> {
                            //updateTextureView()
//                            sp.edit().apply {
//                                if (FreeFormUtils.orientation == Configuration.ORIENTATION_LANDSCAPE) {
//                                    putInt("width_land", WIDTH)
//                                    putInt("height_land", HEIGHT)
//                                } else {
//                                    putInt("width", WIDTH)
//                                    putInt("height", HEIGHT)
//                                }
//                                apply()
//                            }
                        }
                    }
                }
            }
            return true
        }

    }

}