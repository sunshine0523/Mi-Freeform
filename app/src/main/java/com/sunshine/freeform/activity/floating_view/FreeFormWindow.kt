package com.sunshine.freeform.activity.floating_view

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.content.res.Resources
import android.graphics.Matrix
import android.graphics.PixelFormat
import android.graphics.Point
import android.graphics.SurfaceTexture
import android.hardware.display.DisplayManager
import android.hardware.display.VirtualDisplay
import android.os.VibrationEffect
import android.os.Vibrator
import android.util.DisplayMetrics
import android.view.*
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.Space
import android.widget.Toast
import androidx.constraintlayout.widget.ConstraintLayout
import com.sunshine.freeform.R
import com.sunshine.freeform.utils.FreeFormUtils
import com.sunshine.freeform.utils.InputEventUtils
import com.sunshine.freeform.utils.PermissionUtils
import com.sunshine.freeform.view.floating.FreeFormHelper
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

/**
 * @author sunshine
 * @date 2021/2/26
 * 全新设计的小窗界面
 * tips:怎么解决updateView但是textureView却不能更新的问题？突然想起来的，可以new啊
 */
class FreeFormWindow(
    private val context: Context,
    override val command: String,
    override val packageName: String
) : FreeFormWindowAbs() {
    companion object {
        private const val TAG = "FreeFormWindow"

        private const val MIN_WIDTH = 400
        private const val MIN_HEIGHT = 600
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
    private var swingView: ConstraintLayout? = null
    private var closeView: ConstraintLayout? = null
    //关闭的小横条
    private var closeBarView: View? = null
    //改变小窗大小控制view
    private var resizeLeftView: View? = null
    private var resizeRightView: View? = null
    //textureView的父布局
    private var textureRootView: LinearLayout? = null
    private var textureView: TextureView? = null
    private var spaceLeft: Space? = null
    private var spaceRight: Space? = null

    private val sp = context.getSharedPreferences("com.sunshine.freeform_preferences", Context.MODE_PRIVATE)

    //真实屏幕和小窗的大小比例
    private var scale = 1.0f

    private var virtualDisplay: VirtualDisplay? = null
    override var displayId = -1

    //是否是第一次初始化textureView，如果是的话，需要启动应用，否则就不启动了，因为会弹出root允许
    private var firstStart = true

    //注入输入类
    private val inputEventUtils = InputEventUtils()

    //触摸
    private val touchListener = TouchListener()

    //振动
    private val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
    //20210711 新增 是否进行振动提示
    private val vibrationEffect: VibrationEffect? =
        if (sp.getBoolean("vibrator", true)) VibrationEffect.createOneShot(25, 255)
        else null

    //小窗内应用的方向
    private var smallRotation = -1
    //是否已经旋转了，如果不判断会一直转
    private var hasRotation = false

    //小窗显示的模式 1 User 2 System
    private val showModel = sp.getInt("freeform_show_model", 1)

    //小窗控制模式 0 底部栏 1 顶部按钮 2 都显示
    private val controlModel = sp.getString("freeform_control_mode", "0")

    //挂起时小窗大小
    private val smallFreeFormSize = sp.getInt("small_freeform_size", 20)

    private var smallModelGlobal = false

    //判断本次打开是否移动了小窗，如果移动了就在关闭时更新位置
    private var isMoved = false

    /**
     * @author sunshine
     * @date 2021/2/26
     * virtualDisplay方向监听器
     * tips:怎么解决updateView但是textureView却不能更新的问题？可以new啊
     */
    @Deprecated("This field is deprecate because it`s so bad...")
    private val displayListener = object : DisplayManager.DisplayListener {
        override fun onDisplayChanged(displayId: Int) {
            var nowRotation = virtualDisplay?.display?.rotation?:-1
            if (smallRotation == -1) {
                smallRotation = nowRotation
            } else {
                //当前小窗内容为横屏时，nowRotation会错误，需要更改
                if (smallRotation % 2 != 0) {
                    nowRotation = (nowRotation + 1) % 4
                }
                //比如，0到90度需要重新设置，但是0到180不需要设置
                if (abs(smallRotation - nowRotation) % 2 != 0) {
                    //如果本次没有旋转，那么开始旋转
                    if (!hasRotation) {
                        smallRotation = nowRotation
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
        setWidthHeight()

        val displayManager = context.getSystemService(DisplayManager::class.java) as DisplayManager
        virtualDisplay = displayManager.createVirtualDisplay(
            "mi-freeform-display-$this",
            screenWidth,
            screenHeight,
            FreeFormUtils.dpi,
            null,
            DisplayManager.VIRTUAL_DISPLAY_FLAG_PRESENTATION
        )

        displayId = virtualDisplay!!.display.displayId
        displayManager.registerDisplayListener(displayListener, null)

        freeFormLayout = LayoutInflater.from(context).inflate(R.layout.view_freeform_window, null, false)
        textureRootView = freeFormLayout!!.findViewById(R.id.texture_root)

        swingView = freeFormLayout!!.findViewById(R.id.view_swing)
        closeView = freeFormLayout!!.findViewById(R.id.view_close)
        closeBarView = freeFormLayout!!.findViewById(R.id.view_close_bar)
        resizeLeftView = freeFormLayout!!.findViewById(R.id.view_resize_left)
        resizeRightView = freeFormLayout!!.findViewById(R.id.view_resize_right)
        spaceLeft = freeFormLayout!!.findViewById(R.id.space_left)
        spaceRight = freeFormLayout!!.findViewById(R.id.space_right)
        swingView!!.setOnTouchListener(touchListener)
        closeView!!.setOnTouchListener(touchListener)
        resizeLeftView!!.setOnTouchListener(touchListener)
        resizeRightView!!.setOnTouchListener(touchListener)
        closeView!!.setOnClickListener(ClickListener())
        closeBarView!!.setOnClickListener(ClickListener())

        updateTextureView()

        //显示右上角或者都显示模式下显示经典控制按钮
        if (controlModel == "1" || controlModel == "2") {
            val closeButton: ImageView = freeFormLayout!!.findViewById(R.id.close_button)
            closeButton.visibility = View.VISIBLE
            closeButton.setOnClickListener {
                destroy()
            }

            val fullButton: ImageView = freeFormLayout!!.findViewById(R.id.full_button)
            fullButton.visibility = View.VISIBLE
            fullButton.setOnClickListener {
                destroy()
                val intent = context.packageManager.getLaunchIntentForPackage(packageName)
                intent!!.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_REORDER_TO_FRONT
                context.startActivity(intent)
//                if (FreeFormUtils.getControlService().moveStack(displayId)) {
//                    destroy()
//                } else {
//                    destroy()
//                    val intent = context.packageManager.getLaunchIntentForPackage(packageName)
//                    intent!!.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_REORDER_TO_FRONT
//                    context.startActivity(intent)
//                }
            }
        }

        //只显示右上角按钮模式或都不显示模式下就把closeBarView隐藏
        if (controlModel == "1" || controlModel == "3") {
            closeBarView?.visibility = View.GONE
        }
        
        // 设置LayoutParam
        layoutParams!!.apply {
            //启用系统层级的对话框
            type = if (showModel == 1) WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY else 2026
            width = WIDTH
            height = HEIGHT// + if (compatibleMode) 50f.dp2px() else 25f.dp2px()
            //设置这个在点击外部时响应外部操作，如果设置了FLAG_NOT_FOCUSABLE，悬浮窗不会躲避输入法，但是同时设置FLAG_ALT_FOCUSABLE_IM就都正常了，不设置FLAG_NOT_FOCUSABLE，会产生断触
            flags = WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_ALT_FOCUSABLE_IM// or WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED// or WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS//
            format = PixelFormat.RGBA_8888
            //获取记录中的位置
            if (FreeFormUtils.orientation == Configuration.ORIENTATION_LANDSCAPE) {
                x = sp.getInt("freeform_window_x_landscape", 0)
                y = sp.getInt("freeform_window_y_landscape", 0)
            } else {
                x = sp.getInt("freeform_window_x_portrait", 0)
                y = sp.getInt("freeform_window_y_portrait", 0)
            }
        }

        show()
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun updateTextureView() {
        //移除之前的view
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
        matrix.postScale(1 / scale, 1 / scale, 0.0f, 0.0f)
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
                surface: SurfaceTexture,
                width: Int,
                height: Int
            ) {
                virtualDisplay?.resize(screenWidth, screenHeight, FreeFormUtils.dpi)
                virtualDisplay?.surface = Surface(surface)

                if (firstStart) {
                    // 21/3/22 如果这两个放在了if中，那么如果执行失败，将会出现错误
                    FreeFormUtils.freeFormViewSet.add(this@FreeFormWindow)
                    FreeFormUtils.displayIdStackSet.push(displayId)
                    //启动成功
                    if (FreeFormHelper.getControlService() != null && FreeFormHelper.getControlService()!!.startActivity(command + displayId)) {
                        firstStart = false
                    } else {
                        Toast.makeText(context, "命令执行失败，可能的原因：远程服务没有启动、打开的程序不存在或已经停用", Toast.LENGTH_SHORT).show()
                        destroy()
                    }
                }
            }
        }
    }

    private fun show() {
        try {
            windowManager.addView(freeFormLayout, layoutParams)
        } catch (e: Exception) {
            Toast.makeText(context, context.getString(R.string.show_overlay_fail), Toast.LENGTH_SHORT).show()
        }
    }

    /**
     * 设置小窗和获取屏幕的宽高 dpi
     */
    fun setWidthHeight() {
        val point = Point()
        val dm = DisplayMetrics()
        windowManager.defaultDisplay.getSize(point)
        windowManager.defaultDisplay.getMetrics(dm)

        if (FreeFormUtils.orientation == Configuration.ORIENTATION_LANDSCAPE) {
            realScreenWidth = max(point.x, point.y)
            realScreenHeight = min(point.x, point.y)
        } else {
            realScreenWidth = min(point.x, point.y)
            realScreenHeight = max(point.x, point.y)
        }

        if (smallModelGlobal) {
            WIDTH = 108 * smallFreeFormSize / 10
            HEIGHT = 192 * smallFreeFormSize / 10
        }
        else {
            //横屏
            if (FreeFormUtils.orientation == Configuration.ORIENTATION_LANDSCAPE) {
                //横屏状态，默认宽为屏幕1/4，高为屏幕80%
                val saveWidth = sp.getInt("width_land", -1)
                val saveHeight = sp.getInt("height_land", -1)

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

        //如果宽大于高，那么不允许
        if (WIDTH > HEIGHT) {
            WIDTH = HEIGHT
        }

        //横屏
        if (smallRotation != -1 && smallRotation % 2 != 0) {
            val temp = WIDTH
            WIDTH = HEIGHT
            HEIGHT = temp

            //防止宽度比屏幕宽度高
            if (WIDTH > realScreenWidth - 50) {
                WIDTH = realScreenWidth - 50
            }
        }

        //为什么减64dp和48dp，因为cardview有偏差，这个偏差导致实际显示的宽度小64dp，高度小48dp
        //设置和小窗宽高比相同的屏幕大小
        scale =  realScreenHeight.toFloat() / (HEIGHT )
        scale = min(scale,  realScreenWidth.toFloat() / (WIDTH))

        screenHeight = ((HEIGHT) * scale).roundToInt()
        screenWidth = ((WIDTH) * scale).roundToInt()

        tempWidth = WIDTH
        tempHeight = HEIGHT

        //设置dpi
        FreeFormUtils.dpi = dm.densityDpi
    }

    /**
     * 改变小窗大小，拖动大小用
     * @param position 响应的位置，0 左边 1 右边 因为左右边响应的加减是不同的
     */
    private fun resizeFreeForm(movedX: Int, movedY: Int, position: Int) {
        //设置新的小窗宽高
        val tempHeight = HEIGHT + movedY
        val tempWidth = if (position == 0) WIDTH - movedX else WIDTH + movedX

        //小窗过大过小不允许
        if (tempWidth < 400 || tempWidth > realScreenWidth * 0.9 || tempHeight > realScreenHeight * 0.9) return

        //小窗中横屏会导致宽大于高，此时允许
        if (tempHeight <= tempWidth) return

        HEIGHT = tempHeight
        WIDTH = tempWidth

        //设置和小窗宽高比相同的屏幕大小
        scale =  realScreenHeight.toFloat() / (HEIGHT - 40f.dp2px())
        scale = min(scale,  realScreenWidth.toFloat() / (WIDTH - 32f.dp2px()))

        //横屏
        if (smallRotation != -1 && smallRotation % 2 != 0) {
            val temp = WIDTH
            WIDTH = HEIGHT
            HEIGHT = temp
        }

        screenHeight = ((HEIGHT - 40f.dp2px()) * scale).roundToInt()
        screenWidth = ((WIDTH - 32f.dp2px()) * scale).roundToInt()

        layoutParams!!.apply {
            width = WIDTH
            height = HEIGHT
        }
        windowManager.updateViewLayout(freeFormLayout, layoutParams)
    }

    private fun resizeFreeForm(widthChanged: Int, heightChanged: Int) {
        val tempHeight = HEIGHT + heightChanged
        val tempWidth = WIDTH + widthChanged
        if (tempWidth < MIN_WIDTH || tempWidth > realScreenWidth * 0.9 || tempHeight < MIN_HEIGHT || tempHeight > realScreenHeight * 0.9) return

        WIDTH = tempWidth
        HEIGHT = tempHeight

        scale =  realScreenHeight.toFloat() / (HEIGHT)
        scale = min(scale,  realScreenWidth.toFloat() / (WIDTH))

        //横屏
        if (smallRotation != -1 && smallRotation % 2 != 0) {
            val temp = WIDTH
            WIDTH = HEIGHT
            HEIGHT = temp
        }

        screenHeight = ((HEIGHT) * scale).roundToInt()
        screenWidth = ((WIDTH ) * scale).roundToInt()

        layoutParams!!.apply {
            width = WIDTH
            height = HEIGHT
        }
        windowManager.updateViewLayout(freeFormLayout, layoutParams)
    }

    /**
     * 重新设置布局，用于横竖屏切换用
     */
    @SuppressLint("ClickableViewAccessibility")
    override fun resize() {
        //横竖屏切换时挂起的会恢复，同时也可以做正常挂起的恢复
        if (smallModelGlobal) {
            swingView!!.visibility = View.VISIBLE
            closeView!!.visibility = View.VISIBLE
            swingView!!.setOnTouchListener(touchListener)
            closeView!!.setOnTouchListener(touchListener)
            textureView!!.setOnTouchListener(touchListener)
            resizeLeftView!!.setOnTouchListener(touchListener)
            resizeRightView!!.setOnTouchListener(touchListener)

            if (controlModel != "1") {
                //恢复小横条
                closeBarView?.visibility = View.VISIBLE
            }
            smallModelGlobal = false
        }

        setWidthHeight()
        layoutParams!!.apply {
            width = WIDTH
            height = HEIGHT
            x = 0
            y = 0
        }

        windowManager.updateViewLayout(freeFormLayout, layoutParams)
        updateTextureView()
    }

    override fun exitSmall() {

    }

    override fun destroy() {
        //view在屏幕中的位置，用于记录
        val locations = IntArray(2)

        freeFormLayout?.getLocationOnScreen(locations)

        // 21/3/25 为了防止下移，只有更新时才需要重新设置
        if (isMoved) {
            // 21/3/22 为了防止下移，要减去35fdp2px，为什么是这个...
            sp.edit().apply {
                if (FreeFormUtils.orientation == Configuration.ORIENTATION_LANDSCAPE) {
                    putInt(
                        "freeform_window_x_landscape",
                        locations[0] - realScreenWidth / 2 + WIDTH / 2
                    )
                    putInt(
                        "freeform_window_y_landscape",
                        locations[1] - realScreenHeight / 2 + HEIGHT / 2 - 35f.dp2px()
                    )
                } else {
                    putInt(
                        "freeform_window_x_portrait",
                        locations[0] - realScreenWidth / 2 + WIDTH / 2
                    )
                    putInt(
                        "freeform_window_y_portrait",
                        locations[1] - realScreenHeight / 2 + HEIGHT / 2 - 35f.dp2px()
                    )
                }
                apply()
            }
        }

        displayManager.unregisterDisplayListener(displayListener)
        windowManager.removeView(freeFormLayout)
        virtualDisplay?.surface?.release()
        virtualDisplay?.release()

        FreeFormUtils.freeFormViewSet.remove(this)
        FreeFormUtils.displayIdStackSet.pop()
    }

    /**
     * kotlin扩展类
     */
    private fun Float.dp2px(): Int {
        val scale = Resources.getSystem().displayMetrics.density
        return (this * scale + 0.5f).toInt()
    }

    /**
     * 当前小窗不为顶部却需要点击或触摸，则移动到顶部
     */
    private fun moveToTop(): Boolean {
        if (FreeFormUtils.displayIdStackSet.peek() != displayId) {
            windowManager.removeView(freeFormLayout)
            windowManager.addView(freeFormLayout, layoutParams)
            FreeFormUtils.displayIdStackSet.push(displayId)
            return true
        }
        return false
    }

    init {
        if (FreeFormUtils.hasFreeFormWindow(packageName)) {
            Toast.makeText(context, context.getString(R.string.already_show), Toast.LENGTH_SHORT).show()
        } else {
            if (PermissionUtils.checkPermission(context)  && FreeFormUtils.serviceInitSuccess()) {
                //initView()
            } else {
                //Toast.makeText(context, context.getString(R.string.sui_not_running), Toast.LENGTH_SHORT).show()
            }
            initView()
        }
    }

    inner class ClickListener : View.OnClickListener {
        override fun onClick(v: View?) {
            when (v?.id) {
                R.id.view_close, R.id.view_close_bar -> {
                    moveToTop()
                    vibrator.vibrate(vibrationEffect)
                    FreeFormUtils.getControlService()?.pressBack(displayId)
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

        private val gestureDetector = GestureDetector(context, GestureListener())

        private var needToUpdateTexture = false

        /**
         * 挂起模式
         */
        @SuppressLint("ClickableViewAccessibility")
        private fun toSmallFreeForm(nowX: Int, nowY: Int) {
            //移动到边缘需要缩小
            if ((nowX <= FreeFormUtils.SMALL_FREEFORM_POSITION || nowX >= realScreenWidth - FreeFormUtils.SMALL_FREEFORM_POSITION) && nowY <= FreeFormUtils.SMALL_FREEFORM_POSITION) {
                //第一次缩小
                if (smallFreeForm) {
                    //设置挂起模式标记
                    smallModelGlobal = true

                    //振动
                    vibrator.vibrate(vibrationEffect)

                    //最小化模式隐藏拖动和最大最小化界面
                    swingView!!.visibility = View.INVISIBLE
                    closeView!!.visibility = View.INVISIBLE
                    swingView!!.setOnTouchListener(null)
                    closeView!!.setOnTouchListener(null)
                    resizeLeftView!!.setOnTouchListener(null)
                    resizeRightView!!.setOnTouchListener(null)

                    //重新设置宽高参数
                    setWidthHeight()

                    //重新设置layoutParams
                    layoutParams!!.apply {
                        width = WIDTH
                        height = HEIGHT
//                        //竖屏
//                        if (smallRotation % 2 == 0) {
//                            width = WIDTH
//                            height = HEIGHT
//                        } else {
//                            width = HEIGHT
//                            height = WIDTH
//                        }
                        x = if (nowX <= FreeFormUtils.SMALL_FREEFORM_POSITION) (realScreenWidth - 108 * smallFreeFormSize / 10) / -2 else (realScreenWidth - 108 * smallFreeFormSize / 10) / 2
                        y = (realScreenHeight - 192 * smallFreeFormSize / 10) / -2
                    }

                    //挂起时隐藏小横条
                    closeBarView?.visibility = View.GONE

                    //更新悬浮窗
                    windowManager.updateViewLayout(freeFormLayout, layoutParams)

                    //更新屏幕
                    updateTextureView()

                    //最小化只响应恢复点击
                    textureView!!.setOnTouchListener { _, event ->
                        when (event?.action) {
                            //松手时恢复点击事件
                            MotionEvent.ACTION_UP -> {
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
            if (moveToTop()) {
                return false
            }
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
                            layoutParams!!.x = layoutParams!!.x + movedX.roundToInt()
                            layoutParams!!.y = layoutParams!!.y + movedY.roundToInt()
                            windowManager.updateViewLayout(freeFormLayout, layoutParams)

                            toSmallFreeForm(nowX.roundToInt(), nowY.roundToInt())

                            isMoved = true
                        }
                    }
                    return true
                }

                //触控
                R.id.texture_view -> {
                    inputEventUtils.rootInjectMotionEvent(event, displayId, scale)
//                    if (gestureDetector.onTouchEvent(event)) {
//                        needToUpdateTexture = true
//                    } else {
//                        if (needToUpdateTexture) {
//                            if (event!!.action == MotionEvent.ACTION_UP) {
//                                updateTextureView()
//                                needToUpdateTexture = false
//                                tempWidth = WIDTH
//                                tempHeight = HEIGHT
//                            }
//                        } else {
//                            inputEventUtils.rootInjectMotionEvent(event, displayId, scale)
//                        }
//                    }
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
                                    intent!!.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_REORDER_TO_FRONT
                                    context.startActivity(intent)
//                                    if (FreeFormUtils.getControlService().moveStack(displayId)) {
//                                        destroy()
//                                    } else {
//                                        destroy()
//                                        val intent = context.packageManager.getLaunchIntentForPackage(packageName)
//                                        intent!!.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_REORDER_TO_FRONT
//                                        context.startActivity(intent)
//                                    }
                                }
                            }
                        }
                    }
                    //设置为false才能响应clickListener
                    return false
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
                            resizeFreeForm(movedX.roundToInt(), movedY.roundToInt(), if (v.id == R.id.view_resize_left) 0 else 1)
                            resizeX = nowX
                            resizeY = nowY
                        }
                        MotionEvent.ACTION_UP -> {
                            updateTextureView()
                            sp.edit().apply {
                                if (FreeFormUtils.orientation == Configuration.ORIENTATION_LANDSCAPE) {
                                    putInt("width_land", WIDTH)
                                    putInt("height_land", HEIGHT)
                                } else {
                                    putInt("width", WIDTH)
                                    putInt("height", HEIGHT)
                                }
                                apply()
                            }
                        }
                    }
                }
            }
            return true
        }

        private inner class GestureListener : GestureDetector.SimpleOnGestureListener() {
            override fun onScroll(
                e1: MotionEvent?,
                e2: MotionEvent?,
                distanceX: Float,
                distanceY: Float
            ): Boolean {
                if (null == e1 || null == e2) return false

                when {
                    //left
                    e1.x <= 75 -> {
                        //divided 5 for follow figure
                        resizeFreeForm(((e1.x - e2.x) / 10).roundToInt(), 0)
                        return true
                    }
                    //right
                    e1.x >= tempWidth - 75 -> {
                        resizeFreeForm(((e2.x - e1.x) / 10).roundToInt(), 0)
                        return true
                    }
                    //bottom
                    e1.y >= tempHeight - 75 -> {
                        resizeFreeForm(0, ((e2.y - e1.y) / 20).roundToInt())
                        return true
                    }
                    else -> return false
                }
            }
        }
    }

    private var tempWidth = WIDTH
    private var tempHeight = HEIGHT
}