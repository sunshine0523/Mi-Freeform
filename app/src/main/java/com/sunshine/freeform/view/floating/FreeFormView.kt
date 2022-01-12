package com.sunshine.freeform.view.floating

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.graphics.*
import android.hardware.display.DisplayManager
import android.hardware.display.VirtualDisplay
import android.util.DisplayMetrics
import android.util.Log
import android.view.*
import android.widget.LinearLayout
import android.widget.Toast
import androidx.cardview.widget.CardView
import com.sunshine.freeform.R
import com.sunshine.freeform.callback.OrientationChangedListener
import com.sunshine.freeform.callback.SuiServerListener
import com.sunshine.freeform.utils.FreeFormUtils
import com.sunshine.freeform.utils.InputEventUtils
import kotlin.math.*


/**
 * @author sunshine
 * @date 2022/1/6
 */
class FreeFormView(
    private val context: Context,
    val command: String,
    private val packageName: String
) {

    companion object {
        private const val TAG = "FreeFormView"

        private const val FREEFORM_DEFAULT_PROPORTION = 0.7f
        private const val FREEFORM_DEFAULT_PROPORTION_LANDSCAPE = 0.9f
        private const val FREEFORM_SUSPEND_HEIGHT = 192 * 3

        private const val BAR_WIDTH = 128
        private const val BAR_HEIGHT = 8
        private const val BAR_VIEW_HEIGHT = 96
        //控制栏与小窗的距离
        private const val BAR_DISTANCE = 48

        val orientationChangedListener = object : OrientationChangedListener {
            override fun onChanged(orientation: Int) {
                FreeFormHelper.onOrientationChanged()
            }
        }

        //注入输入类
        private val inputEventUtils = InputEventUtils()

        //控制栏滑动方向
        const val CHANGE_LEFT = 0
        const val CHANGE_RIGHT = 1
        const val BACK = 2
        const val CLOSE = 3
        const val MAX = 4
        const val MOVE = 5
        const val DOUBLE = 6
        private var curEvent = -1

        const val DPI = 300

        //小窗可以关闭和最大化滑动距离
        const val CAN_CLOSE = 300
        const val CAN_MAX = 200

        const val SUSPEND_DPI = 200
        const val SUSPEND_DISTANCE = 50
    }

    private val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
    private lateinit var freeFormRootView: CardView
    private lateinit var virtualDisplay: VirtualDisplay
    private var displayId: Int = -1
    private var textureView: TextureView? = null
    private lateinit var displayManager: DisplayManager
    private val touchListener = TouchListener()

    private var freeFormWidth = 0
    private var freeFormHeight = 0
    //屏幕大致宽高，请注意，这不是屏幕真正宽高，而是和小窗宽高比相同且和屏幕相近的一个宽高
    private var screenWidth = 0
    private var screenHeight = 0

    //屏幕真正的高
    private var realScreenHeight = 0
    private var realScreenWidth = 0

    private val windowLayoutParams = WindowManager.LayoutParams()

    //是否是第一次初始化textureView，如果是的话，需要启动应用，否则就不启动了，因为会弹出root允许
    private var firstStart = true

    private var scale = 1.0f

    //小窗中显示方向：0竖屏 1横屏
    private var freeFormRotate = 0

    private var hasChanged = false

    //小窗是否为边角挂起状态
    private var isSuspend = false

    private val displayListener = object : DisplayManager.DisplayListener {
        override fun onDisplayChanged(displayId: Int) {
            //原理：小窗内方向旋转有两次回调：1.小窗内变为横屏，此时执行else，让小窗也跟着旋转
            //2.小窗和小窗内方向保持一致后，会再一次回调，此时不需要做任何操作
            if (freeFormRotate != virtualDisplay.display.rotation) {
                 freeFormRotate = virtualDisplay.display.rotation
                hasChanged = if (hasChanged) {
                    false
                } else {
                    onFreeFormRotationChanged()
                    true
                }
            }
        }

        override fun onDisplayAdded(displayId: Int) {

        }

        override fun onDisplayRemoved(displayId: Int) {

        }

    }

    //---------------bar--------------
    private lateinit var barLayout: LinearLayout
    private lateinit var barView: View
    private val barWindowLayoutParams = WindowManager.LayoutParams()
    private val myGestureListener = MyGestureListener(context.resources.configuration.orientation)
    private val gestureDetector = GestureDetector(context, myGestureListener)

    init {
        if (FreeFormHelper.hasFreeFormWindow(packageName)) {
            Toast.makeText(context, context.getString(R.string.already_show), Toast.LENGTH_SHORT).show()
        }else{
            FreeFormHelper.init(
                context,
                object : SuiServerListener() {
                    override fun onStart() {
                        initView()
                        initBar()
                    }

                    override fun onStop() {

                    }

                    override fun onFailBind() {
                        Toast.makeText(context, "服务没有运行：发生在FreeFormView", Toast.LENGTH_SHORT).show()
                    }

                }
            )
        }
    }

    //务必做到一点：简洁！
    private fun initView() {
        val rootLayout = LayoutInflater.from(context).inflate(R.layout.view_freeform_view, null, false)
        freeFormRootView = rootLayout.findViewById(R.id.freeform_root_view)

        initSize()
        initTextureView()

        windowLayoutParams.apply {
            type = WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            width = freeFormWidth
            height = freeFormHeight
            //设置这个在点击外部时响应外部操作，如果设置了FLAG_NOT_FOCUSABLE，悬浮窗不会躲避输入法，但是同时设置FLAG_ALT_FOCUSABLE_IM就都正常了，不设置FLAG_NOT_FOCUSABLE，会产生断触
            flags = WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_ALT_FOCUSABLE_IM or WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS// or WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED// or WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS//
            format = PixelFormat.RGBA_8888
            x =
                if (context.resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE) (screenWidth - realScreenWidth) / 2
                else 0
        }

        try {
            windowManager.addView(freeFormRootView, windowLayoutParams)
        } catch (e: Exception) {
            Toast.makeText(context, context.getString(R.string.show_overlay_fail), Toast.LENGTH_SHORT).show()
        }
    }

    private fun initSize() {
        val point = Point()
        val dm = DisplayMetrics()
        windowManager.defaultDisplay.getSize(point)
        windowManager.defaultDisplay.getMetrics(dm)

        var realWidth = 0
        var realHeight = 0

        if (context.resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE) {
            realWidth = max(point.x, point.y)
            realHeight = min(point.x, point.y)
            realScreenWidth = realWidth
            realScreenHeight = realHeight

            screenHeight = min(realHeight, realWidth / 9 * 16)
            screenWidth = screenHeight / 16 * 9
            freeFormHeight = (screenHeight * FREEFORM_DEFAULT_PROPORTION_LANDSCAPE).roundToInt()
            freeFormWidth = freeFormHeight / 16 * 9
        } else {
            realWidth = min(point.x, point.y)
            realHeight = max(point.x, point.y)
            realScreenWidth = realWidth
            realScreenHeight = realHeight

            screenHeight = min(realHeight, realWidth / 9 * 16)
            screenWidth = screenHeight / 16 * 9
            freeFormHeight = (screenHeight * FREEFORM_DEFAULT_PROPORTION).roundToInt()
            freeFormWidth = freeFormHeight / 16 * 9
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun initTextureView() {
        displayManager = context.getSystemService(DisplayManager::class.java) as DisplayManager
        displayManager.registerDisplayListener(displayListener, null)
        virtualDisplay = displayManager.createVirtualDisplay(
            "mi-freeform-display-$this",
            freeFormWidth,
            freeFormHeight,
            DPI,
            null,
            DisplayManager.VIRTUAL_DISPLAY_FLAG_PRESENTATION
        )
        displayId = virtualDisplay.display.displayId

        if (textureView == null) textureView = TextureView(context)
        textureView?.id = R.id.texture_view

        textureView?.setOnTouchListener(touchListener)

        textureView?.surfaceTextureListener = object : TextureView.SurfaceTextureListener {
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
                virtualDisplay.surface = Surface(surface)

                if (firstStart) {
                    FreeFormHelper.freeFormViewSet.add(this@FreeFormView)
                    FreeFormHelper.displayIdStackSet.push(displayId)

                    if (FreeFormHelper.getControlService() != null && FreeFormHelper.getControlService()!!.startActivity(command + displayId)) {
                        firstStart = false
                    } else {
                        Log.e(TAG, "${FreeFormHelper.getControlService()}")
                        Toast.makeText(context, "命令执行失败，可能的原因：远程服务没有启动、打开的程序不存在或已经停用", Toast.LENGTH_SHORT).show()
                        destroy()
                    }
                }
            }
        }

        freeFormRootView.addView(textureView)
    }

    @SuppressLint("UseCompatLoadingForDrawables", "ClickableViewAccessibility")
    private fun initBar(){
        barView = View(context).apply {
            layoutParams = if (context.resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE)
                ViewGroup.LayoutParams(BAR_HEIGHT, BAR_WIDTH)
            else ViewGroup.LayoutParams(BAR_WIDTH, BAR_HEIGHT)
            alpha = 0.5f
            background = context.getDrawable(R.drawable.corners_bg)
        }
        barLayout = LinearLayout(context).apply {
            setBackgroundColor(Color.TRANSPARENT)
            gravity = Gravity.CENTER
            addView(barView)
            id = R.id.bar_layout
            setOnTouchListener(touchListener)
            setOnLongClickListener {
                it.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
                true
            }
        }
        barWindowLayoutParams.apply {
            //启用系统层级的对话框
            //type = if (showModel == 1) WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            type = WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY

            //设置这个在点击外部时响应外部操作，如果设置了FLAG_NOT_FOCUSABLE，悬浮窗不会躲避输入法，但是同时设置FLAG_ALT_FOCUSABLE_IM就都正常了，不设置FLAG_NOT_FOCUSABLE，会产生断触
            flags = WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_ALT_FOCUSABLE_IM or WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS// or WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED// or WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS//
            format = PixelFormat.RGBA_8888
            if (context.resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE) {
                width = BAR_VIEW_HEIGHT
                height = freeFormHeight
                x = windowLayoutParams.x + freeFormWidth / 2 + BAR_DISTANCE
                y = 0
            } else {
                width = freeFormWidth
                height = BAR_VIEW_HEIGHT
                x = 0
                y = windowLayoutParams.y + freeFormHeight / 2 + BAR_DISTANCE
            }
        }
        windowManager.addView(barLayout, barWindowLayoutParams)
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun updateTextureView() {
        //TODO 无奈之举，不重新new的话没有办法更改尺寸
        freeFormRootView.removeView(textureView)

        textureView = TextureView(context)
        textureView?.id = R.id.texture_view
        textureView?.setOnTouchListener(touchListener)

        textureView?.surfaceTextureListener = object : TextureView.SurfaceTextureListener {
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

            override fun onSurfaceTextureAvailable(
                surface: SurfaceTexture,
                width: Int,
                height: Int
            ) {
                virtualDisplay.resize(freeFormWidth, freeFormHeight, if (isSuspend) SUSPEND_DPI else DPI)
                virtualDisplay.surface = Surface(surface)
            }
        }

        freeFormRootView.addView(textureView)
    }

    @SuppressLint("UseCompatLoadingForDrawables")
    fun onOrientationChanged(){
        initSize()

        windowLayoutParams.apply {
            width = freeFormWidth
            height = freeFormHeight
            x =
                if (context.resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE) (screenWidth - realScreenWidth) / 2
                else 0
            y = 0
        }
        updateTextureView()
        windowManager.updateViewLayout(freeFormRootView, windowLayoutParams)
        //----------------Bar----------------
        myGestureListener.setOrientation(context.resources.configuration.orientation)

        barView.layoutParams.apply {
            if (context.resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE) {
                width = BAR_HEIGHT
                height = BAR_WIDTH
            }
            else {
                width = BAR_WIDTH
                height = BAR_HEIGHT
            }
        }
        barWindowLayoutParams.apply {
            if (context.resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE) {
                width = BAR_VIEW_HEIGHT
                height = freeFormHeight
                x = windowLayoutParams.x + freeFormWidth / 2 + BAR_DISTANCE
                y = 0
            } else {
                width = freeFormWidth
                height = BAR_VIEW_HEIGHT
                x = 0
                y = windowLayoutParams.y + freeFormHeight / 2 + BAR_DISTANCE
            }
        }
        windowManager.updateViewLayout(barLayout, barWindowLayoutParams)

        if (isSuspend) toSuspend()
    }

    private fun onFreeFormRotationChanged() {
        //交换宽高
        val temp = freeFormWidth
        freeFormWidth = freeFormHeight
        freeFormHeight = temp

        virtualDisplay.resize(freeFormWidth, freeFormHeight, if (isSuspend) SUSPEND_DPI else DPI)

        windowLayoutParams.apply {
            width = freeFormWidth
            height = freeFormHeight
            x =
                if (context.resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE) (screenWidth - realScreenWidth) / 2
                else 0
            y = 0
        }
        windowManager.updateViewLayout(freeFormRootView, windowLayoutParams)
        //----------------Bar----------------
        barView.layoutParams.apply {
            if (context.resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE) {
                width = BAR_HEIGHT
                height = BAR_WIDTH
            }
            else {
                width = BAR_WIDTH
                height = BAR_HEIGHT
            }
        }
        barWindowLayoutParams.apply {
            if (context.resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE) {
                width = BAR_VIEW_HEIGHT
                height = freeFormHeight
                x = windowLayoutParams.x + freeFormWidth / 2 + BAR_DISTANCE
                y = 0
            } else {
                width = freeFormWidth
                height = BAR_VIEW_HEIGHT
                x = 0
                y = windowLayoutParams.y + freeFormHeight / 2 + BAR_DISTANCE
            }
        }
        windowManager.updateViewLayout(barLayout, barWindowLayoutParams)
    }

    private fun toSuspend() {
        val freeFormRotation = if (freeFormWidth > freeFormHeight) 1 else 0
        if (freeFormRotation == 0) {
            freeFormHeight = FREEFORM_SUSPEND_HEIGHT
            freeFormWidth = freeFormHeight / 16 * 9
        } else {
            freeFormWidth = FREEFORM_SUSPEND_HEIGHT
            freeFormHeight = freeFormWidth / 16 * 9
        }

        updateTextureView()
        windowManager.updateViewLayout(
            freeFormRootView,
            windowLayoutParams.apply {
                width = freeFormWidth
                height = freeFormHeight
                x = (realScreenWidth - freeFormWidth) / 2 - SUSPEND_DISTANCE
                y = (freeFormHeight - realScreenHeight) / 2 + SUSPEND_DISTANCE
            })
        barLayout.visibility = View.GONE
    }

    private fun destroy() {
        windowManager.removeViewImmediate(freeFormRootView)
        virtualDisplay.surface?.release()
        virtualDisplay.release()

        FreeFormHelper.freeFormViewSet.remove(this)
        FreeFormHelper.displayIdStackSet.pop()

        //----------------Bar----------------
        windowManager.removeViewImmediate(barLayout)
    }

    inner class TouchListener : View.OnTouchListener {
        private var touchX = 0.0f
        private var touchY = 0.0f
        private var downX = 0.0f
        private var downY = 0.0f
        //滑动到一定范围可以关闭、最大化
        private var canClose = false
        private var canMax = false

        //为了防止在三者在移动过程中相互干扰，做一个判断
        private var nowStatus = -1

        @SuppressLint("ClickableViewAccessibility")
        override fun onTouch(v: View?, event: MotionEvent?): Boolean {
            //挂起状态不响应正常事件
            if (isSuspend) {
                isSuspend = false
                barLayout.visibility = View.VISIBLE
                //复用
                onOrientationChanged()
            } else {
                when(v?.id) {
                    R.id.texture_view -> {
                        inputEventUtils.rootInjectMotionEvent(event, displayId, scale)
                    }
                    //上滑：关闭；下滑：最大化；左右角滑：缩放
                    R.id.bar_layout-> {
                        gestureDetector.onTouchEvent(event)
                        when(event?.action) {
                            MotionEvent.ACTION_DOWN -> {
                                touchX = event.rawX
                                touchY = event.rawY
                                downX = event.rawX
                                downY = event.rawY
                            }
                            MotionEvent.ACTION_MOVE -> {
                                val nowX = event.rawX
                                val nowY = event.rawY
                                val movedX = nowX - touchX
                                val movedY = nowY - touchY
                                touchX = nowX
                                touchY = nowY

                                //如果上一次动作还没有完成，不响应其他动作
                                if (nowStatus != -1) curEvent = nowStatus
                                //TODO 横屏调节大小
                                when(curEvent) {
                                    CLOSE -> {
                                        nowStatus = CLOSE

                                        if (context.resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE) {
                                            canClose = if (downX - nowX > CAN_CLOSE) {
                                                if (!canClose) v.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
                                                true
                                            } else {
                                                //回滑时取消关闭
                                                false
                                            }
                                            windowManager.updateViewLayout(
                                                freeFormRootView.apply {
                                                    alpha = max(1.0f - (downX - nowX) *1.0f / CAN_CLOSE, 0.5f)
                                                },
                                                windowLayoutParams)
                                        } else {
                                            canClose = if (downY - nowY > CAN_CLOSE) {
                                                if (!canClose) v.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
                                                true
                                            } else {
                                                false
                                            }
                                            windowManager.updateViewLayout(
                                                freeFormRootView.apply {
                                                    alpha = max(1.0f - (downY - nowY) *1.0f / CAN_CLOSE, 0.5f)
                                                },
                                                windowLayoutParams)
                                        }
                                    }
                                    MAX -> {
                                        nowStatus = MAX

                                        canMax = if (context.resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE) {
                                            if (nowX - downX > CAN_MAX) {
                                                if (!canMax) v.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
                                                true
                                            } else {
                                                false
                                            }
                                        } else {
                                            if (nowY - downY > CAN_MAX) {
                                                if (!canMax) v.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
                                                true
                                            } else {
                                                false
                                            }
                                        }
                                    }
                                    CHANGE_LEFT, CHANGE_RIGHT -> {
                                        nowStatus = curEvent

                                        //小窗内横竖屏状态
                                        val freeFormRotation = if (freeFormWidth > freeFormHeight) 1 else 0

                                        if (context.resources.configuration.orientation == Configuration.ORIENTATION_PORTRAIT) {
                                            if (curEvent == CHANGE_LEFT) freeFormWidth += movedX.roundToInt()
                                            else freeFormWidth -= movedX.roundToInt()
                                            //小窗内为竖屏
                                            if (freeFormRotation == 0) {
                                                freeFormWidth = min(max(screenWidth / 2, freeFormWidth), screenWidth / 4 * 3)
                                                freeFormHeight = freeFormWidth / 9 * 16
                                            } else {
                                                freeFormWidth = min(max(screenWidth / 2, freeFormWidth), screenWidth)
                                                freeFormHeight = freeFormWidth / 16 * 9
                                            }

                                            windowManager.updateViewLayout(
                                                freeFormRootView,
                                                windowLayoutParams.apply {
                                                    width = freeFormWidth
                                                    height = freeFormHeight
                                                }
                                            )

                                            barWindowLayoutParams.apply {
                                                if (context.resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE) {
                                                    width = BAR_VIEW_HEIGHT
                                                    height = freeFormHeight
                                                    x = windowLayoutParams.x + freeFormWidth / 2 + BAR_DISTANCE
                                                    y = windowLayoutParams.y
                                                } else {
                                                    width = freeFormWidth
                                                    height = BAR_VIEW_HEIGHT
                                                    x = windowLayoutParams.x
                                                    y = windowLayoutParams.y + freeFormHeight / 2 + BAR_DISTANCE
                                                }
                                            }
                                            //卡顿
                                            //virtualDisplay.resize(freeFormWidth, freeFormHeight, DPI)
                                            windowManager.updateViewLayout(barLayout, barWindowLayoutParams)
                                        }
                                    }
                                    MOVE -> {
                                        windowLayoutParams.apply {
                                            x = windowLayoutParams.x + movedX.roundToInt()
                                            y = windowLayoutParams.y + movedY.roundToInt()
                                        }
                                        barWindowLayoutParams.apply {
                                            x = barWindowLayoutParams.x + movedX.roundToInt()
                                            y = barWindowLayoutParams.y + movedY.roundToInt()
                                        }
                                        windowManager.updateViewLayout(freeFormRootView, windowLayoutParams)
                                        windowManager.updateViewLayout(barLayout, barWindowLayoutParams)
                                    }
                                }
                            }
                            MotionEvent.ACTION_UP -> {
                                when(curEvent) {
                                    CLOSE -> {
                                        if (canClose) destroy()
                                        else {
                                            windowManager.updateViewLayout(
                                                freeFormRootView.apply {
                                                    alpha = 1.0f
                                                },
                                                windowLayoutParams)
                                        }
                                    }
                                    MAX -> {
                                        if (canMax) {
                                            destroy()
                                            val intent = context.packageManager.getLaunchIntentForPackage(packageName)
                                            intent!!.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_REORDER_TO_FRONT
                                            context.startActivity(intent)
                                        }
                                    }
                                    CHANGE_LEFT, CHANGE_RIGHT -> {
                                        if (context.resources.configuration.orientation == Configuration.ORIENTATION_PORTRAIT) {
                                            //这里有需要注意的点：updateTextureView()中不能加入这个，因为该处不需要加x y
                                            updateTextureView()
                                            windowManager.updateViewLayout(
                                                freeFormRootView,
                                                windowLayoutParams.apply {
                                                    width = freeFormWidth
                                                    height = freeFormHeight
                                                })
                                        }
                                    }
                                    BACK -> {
                                        inputEventUtils.shizukuPressBack(displayId)
                                    }
                                    MOVE -> {

                                    }
                                    DOUBLE -> {
                                        //双击切换成挂起状态
                                        isSuspend = true
                                        toSuspend()
                                    }
                                }
                                nowStatus = -1
                                curEvent = -1
                            }

                        }
                        //响应长按
                        return false
                    }
                }
            }
            return true
        }
    }

    inner class MyGestureListener(o: Int) : GestureDetector.SimpleOnGestureListener() {

        private var orientation = o

        fun setOrientation(orientation: Int) {
            this.orientation = orientation
        }

        override fun onSingleTapUp(e: MotionEvent): Boolean {
            return super.onSingleTapUp(e)
        }

        override fun onLongPress(e: MotionEvent) {
            curEvent = MOVE
        }

        override fun onScroll(
            e1: MotionEvent,
            e2: MotionEvent,
            velocityX: Float,
            velocityY: Float
        ): Boolean {
            val velocity =
                sqrt(velocityX.toDouble().pow(2.0) + velocityY.toDouble().pow(2.0))
                    .toFloat()
            val x1 = e1.x
            val y1 = e1.y
            val x2 = e2.x
            val y2 = e2.y
            val distanceX = x2 - x1
            val distanceY = y2 - y1
            val distance =
                sqrt((y2 - y1).toDouble().pow(2.0) + (x2 - x1).toDouble().pow(2.0))
                    .toInt()
            if (distance < 80) { //小于一定距离则忽略掉
                return true
            }
            if (distanceX > 0) { //向右
                val y3 = distanceX * tan(22.5 * Math.PI / 180)
                val y4 = distanceX * tan((45 + 22.5) * Math.PI / 180)
                val absY = abs(distanceY).toDouble()
                when {
                    absY < y3 -> { //向右滑
                        Log.i(TAG, "向右滑,速率：$velocity")
                        curEvent = if (orientation == Configuration.ORIENTATION_LANDSCAPE) MAX
                        else BACK
                    }
                    absY < y4 -> {
                        curEvent = if (distanceY > 0) { //向右下滑
                            Log.i(TAG, "向右下滑,速率：$velocity")
                            CHANGE_LEFT
                        } else { //向右上滑
                            Log.i(TAG, "向右上滑,速率：$velocity")
                            CHANGE_RIGHT
                        }
                    }
                    else -> {
                        curEvent = if (distanceY > 0) { //向下滑
                            Log.i(TAG, "向下滑,速率：$velocity")
                            if (orientation == Configuration.ORIENTATION_LANDSCAPE) BACK
                            else MAX
                        } else { //向上滑
                            Log.i(TAG, "向上滑,速率：$velocity")
                            if (orientation == Configuration.ORIENTATION_LANDSCAPE) BACK
                            else CLOSE
                        }
                    }
                }
            } else { //向左
                val y3 = abs(distanceX * tan(22.5 * Math.PI / 180))
                val y4 = abs(distanceX * tan((45 + 22.5) * Math.PI / 180))
                val absY = abs(distanceY).toDouble()
                when {
                    absY < y3 -> { //向左滑
                        Log.i(TAG, "向左滑,速率：$velocity")
                        curEvent = if (orientation == Configuration.ORIENTATION_LANDSCAPE) CLOSE
                        else BACK
                    }
                    absY < y4 -> {
                        curEvent = if (distanceY > 0) { //向左下滑
                            Log.i(TAG, "向左下滑,速率：$velocity")
                            CHANGE_RIGHT
                        } else { //向左上滑
                            Log.i(TAG, "向左上滑,速率：$velocity")
                            CHANGE_LEFT
                        }
                    }
                    else -> {
                        curEvent = if (distanceY > 0) { //向下滑
                            Log.i(TAG, "向下滑,速率：$velocity")
                            if (orientation == Configuration.ORIENTATION_LANDSCAPE) BACK
                            else MAX
                        } else { //向上滑
                            Log.i(TAG, "向上滑,速率：$velocity")
                            if (orientation == Configuration.ORIENTATION_LANDSCAPE) BACK
                            else CLOSE
                        }
                    }
                }
            }
            return true
        }

        override fun onFling(
            e1: MotionEvent,
            e2: MotionEvent,
            velocityX: Float,
            velocityY: Float
        ): Boolean {
            return super.onFling(e1, e2, velocityX, velocityY)
        }

        override fun onShowPress(e: MotionEvent) {
            super.onShowPress(e)
        }

        override fun onDown(e: MotionEvent): Boolean {
            return e.pointerCount == 1
        }

        override fun onDoubleTap(e: MotionEvent): Boolean {
            curEvent = DOUBLE
            return super.onDoubleTap(e)
        }

        override fun onDoubleTapEvent(e: MotionEvent): Boolean {
            return super.onDoubleTapEvent(e)
        }

        override fun onSingleTapConfirmed(e: MotionEvent): Boolean {
            return super.onSingleTapConfirmed(e)
        }

        override fun onContextClick(e: MotionEvent): Boolean {
            return super.onContextClick(e)
        }
    }

}