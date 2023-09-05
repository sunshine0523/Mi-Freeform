package io.sunshine0523.freeform.ui.freeform

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.PixelFormat
import android.graphics.SurfaceTexture
import android.os.Handler
import android.util.Log
import android.view.Display
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.Surface
import android.view.TextureView
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.FrameLayout
import io.sunshine0523.freeform.IMiFreeformDisplayCallback
import io.sunshine0523.freeform.service.FreeformWindowManager
import io.sunshine0523.freeform.service.MiFreeformServiceHolder
import io.sunshine0523.freeform.service.SystemServiceHolder
import io.sunshine0523.freeform.util.MLog
import kotlin.math.min
import kotlin.math.roundToInt

class FreeformWindow(
    val uiHandler: Handler,
    val context: Context,
    private val appConfig: AppConfig,
    val freeformConfig: FreeformConfig,
    private val uiConfig: UIConfig,
): TextureView.SurfaceTextureListener, IMiFreeformDisplayCallback.Stub(), View.OnTouchListener {

    var freeformTaskStackListener: FreeformTaskStackListener ?= null
    val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
    val windowParams = WindowManager.LayoutParams()
    private val resourceHolder = RemoteResourceHolder(context, uiConfig.resPkg)
    lateinit var freeformLayout: ViewGroup
    lateinit var freeformRootView: ViewGroup
    private lateinit var topBarView: View
    private lateinit var bottomBarView: View
    private var displayId = Display.INVALID_DISPLAY
    var defaultDisplayWidth = context.resources.displayMetrics.widthPixels
    var defaultDisplayHeight = context.resources.displayMetrics.heightPixels
    private val rotationWatcher = RotationWatcher(this)
    private val hangUpGestureListener = HangUpGestureListener(this)

    companion object {
        private const val TAG = "Mi-Freeform/FreeformWindow"
    }

    init {
        if (MiFreeformServiceHolder.ping()) {
            MLog.i(TAG, "FreeformWindow init")
            uiHandler.post { if (!addFreeformView()) destroy() }
        } else {
            destroy()
            // NOT RUNNING !!!
        }
    }

    override fun onDisplayPaused() {
        //NOT USED
    }

    override fun onDisplayResumed() {
        //NOT USED
    }

    override fun onDisplayStopped() {
        //NOT USED
    }

    override fun onSurfaceTextureAvailable(surfaceTexture: SurfaceTexture, width: Int, height: Int) {
        MLog.i(TAG, "onSurfaceTextureAvailable width:$width height:$height")
        MiFreeformServiceHolder.createDisplay(freeformConfig, appConfig, Surface(surfaceTexture), this)
    }

    override fun onSurfaceTextureSizeChanged(surfaceTexture: SurfaceTexture, width: Int, height: Int) {
        if (freeformConfig.isHangUp) {
            surfaceTexture.setDefaultBufferSize(freeformConfig.width, freeformConfig.height)
        } else {
            freeformConfig.width = width
            freeformConfig.height = height
        }
        if (!freeformConfig.isScaling) {
            MLog.i(TAG, "onSurfaceTextureSizeChanged $width $height")
            uiHandler.post { makeSureFreeformInScreen() }
            MiFreeformServiceHolder.resizeFreeform(this, freeformConfig.width, freeformConfig.height, freeformConfig.densityDpi)
        }
    }

    override fun onSurfaceTextureDestroyed(surfaceTexture: SurfaceTexture): Boolean {
        return true
    }

    override fun onSurfaceTextureUpdated(surfaceTexture: SurfaceTexture) {
        //NOT USED
    }

    override fun onDisplayAdd(displayId: Int) {
        MLog.i(TAG, "onDisplayAdd displayId $displayId")
        uiHandler.post {
            this.displayId = displayId
            freeformTaskStackListener = FreeformTaskStackListener(displayId, this)
            SystemServiceHolder.activityTaskManager.registerTaskStackListener(freeformTaskStackListener)
            MiFreeformServiceHolder.startApp(context, appConfig, displayId)

            val rightView = resourceHolder.getLayoutChildViewByTag<View>(freeformLayout, "rightView")
            if (null == rightView) {
                MLog.e(TAG, "left&leftScale&rightScale view is null")
                destroy()
                return@post
            }
            rightView.setOnClickListener(RightViewClickListener(displayId))
            rightView.setOnLongClickListener(RightViewLongClickListener(this))
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouch(view: View, event: MotionEvent): Boolean {
        MiFreeformServiceHolder.touch(event, displayId)
        return true
    }

    /**
     * Called in uiHandler
     */
    @SuppressLint("WrongConstant")
    private fun addFreeformView(): Boolean {
        MLog.i(TAG, "addFreeformView")
        val tmpFreeformLayout = resourceHolder.getLayout(uiConfig.layoutName) ?: return false
        freeformLayout = tmpFreeformLayout
        freeformRootView = resourceHolder.getLayoutChildViewByTag<FrameLayout>(freeformLayout, "freeform_root") ?: return false
        topBarView = resourceHolder.getLayoutChildViewByTag(freeformLayout, "topBarView") ?: return false
        bottomBarView = resourceHolder.getLayoutChildViewByTag(freeformLayout, "bottomBarView") ?: return false
        val middleView = resourceHolder.getLayoutChildViewByTag<View>(freeformLayout, "middleView") ?: return false
        val moveTouchListener = MoveTouchListener(this)
        topBarView.setOnTouchListener(moveTouchListener)
        middleView.setOnTouchListener(moveTouchListener)
        val leftView = resourceHolder.getLayoutChildViewByTag<View>(freeformLayout, "leftView")
        val leftScaleView = resourceHolder.getLayoutChildViewByTag<View>(freeformLayout, "leftScaleView")
        val rightScaleView = resourceHolder.getLayoutChildViewByTag<View>(freeformLayout, "rightScaleView")
        if (null == leftView || null == leftScaleView || null == rightScaleView) {
            MLog.e(TAG, "left&leftScale&rightScale view is null")
            destroy()
            return false
        }
        leftView.setOnClickListener(LeftViewClickListener(this))
        leftView.setOnLongClickListener(LeftViewLongClickListener(this))
        leftScaleView.setOnTouchListener(RightScaleTouchListener(this))
        rightScaleView.setOnTouchListener(RightScaleTouchListener(this))

        val freeformView = TextureView(context).apply {
            setOnTouchListener(this@FreeformWindow)
            surfaceTextureListener = this@FreeformWindow
        }
        freeformRootView.layoutParams = freeformRootView.layoutParams.apply {
            width = freeformConfig.width
            height = freeformConfig.height
        }
        freeformRootView.addView(freeformView, ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
        windowParams.apply {
            type = WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            width = WindowManager.LayoutParams.WRAP_CONTENT
            height = WindowManager.LayoutParams.WRAP_CONTENT
            flags = WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS or
                    WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
                    WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED or
                    WindowManager.LayoutParams.FLAG_ALT_FOCUSABLE_IM
            format = PixelFormat.RGBA_8888
        }
        SystemServiceHolder.windowManager.watchRotation(rotationWatcher, Display.DEFAULT_DISPLAY)
        runCatching {
            windowManager.addView(freeformLayout, windowParams)
        }.onFailure {
            MLog.e(TAG, "addView failed: $it")
            return false
        }
        return true
    }

    /**
     * Called in uiHandler
     */
    @SuppressLint("ClickableViewAccessibility")
    fun handleHangUp() {
        if (freeformConfig.isHangUp) {
            windowParams.apply {
                x = freeformConfig.notInHangUpX
                y = freeformConfig.notInHangUpY
                flags = flags or WindowManager.LayoutParams.FLAG_ALT_FOCUSABLE_IM
            }
            freeformRootView.layoutParams.apply {
                width = freeformConfig.width
                height = freeformConfig.height
            }
            windowManager.updateViewLayout(freeformLayout, windowParams)
            topBarView.visibility = View.VISIBLE
            bottomBarView.visibility = View.VISIBLE
            freeformConfig.isHangUp = false
            freeformRootView.setOnTouchListener(this)
        } else {
            freeformConfig.notInHangUpX = windowParams.x
            freeformConfig.notInHangUpY = windowParams.y
            toHangUp()
            topBarView.visibility = View.GONE
            bottomBarView.visibility = View.GONE
            freeformConfig.isHangUp = true
            val gestureDetector = GestureDetector(context, hangUpGestureListener)
            freeformRootView.setOnTouchListener { _, event ->
                gestureDetector.onTouchEvent(event)
                if (event.action == MotionEvent.ACTION_UP) makeSureFreeformInScreen()
                true
            }
        }
    }

    /**
     * Called in uiHandler
     */
    fun toHangUp() {
        windowParams.apply {
            x = (defaultDisplayWidth / 2 - freeformConfig.hangUpWidth / 2)
            y = -(defaultDisplayHeight / 2 - freeformConfig.hangUpHeight / 2)
            flags = flags xor WindowManager.LayoutParams.FLAG_ALT_FOCUSABLE_IM
        }
        freeformRootView.layoutParams = freeformRootView.layoutParams.apply {
            width = freeformConfig.hangUpWidth
            height = freeformConfig.hangUpHeight
        }
        runCatching { windowManager.updateViewLayout(freeformLayout, windowParams) }.onFailure { MLog.e(TAG, "$it") }
    }

    /**
     * Called in uiHandler
     */
    fun makeSureFreeformInScreen() {
        if (!freeformConfig.isHangUp) {
            val maxWidth = defaultDisplayWidth
            val maxHeight = (defaultDisplayHeight * 0.9).roundToInt()
            if (freeformRootView.layoutParams.width > maxWidth || freeformRootView.layoutParams.height > maxHeight) {
                freeformRootView.layoutParams = freeformRootView.layoutParams.apply {
                    width = min(freeformRootView.width, maxWidth)
                    height = min(freeformRootView.height, maxHeight)
                }
            }
        }
        if (windowParams.x < -(defaultDisplayWidth / 2)) FreeformAnimation.moveInScreenAnimator(windowParams.x, -(defaultDisplayWidth / 2), 300, true, this)
        else if (windowParams.x > (defaultDisplayWidth / 2)) FreeformAnimation.moveInScreenAnimator(windowParams.x, (defaultDisplayWidth / 2), 300, true, this)
        if (windowParams.y < -(defaultDisplayHeight / 2)) FreeformAnimation.moveInScreenAnimator(windowParams.y, -(defaultDisplayHeight / 2), 300, false, this)
        else if (windowParams.y > (defaultDisplayHeight / 2)) FreeformAnimation.moveInScreenAnimator(windowParams.y, (defaultDisplayHeight / 2), 300, false, this)
    }

    /**
     * Called in uiHandler
     */
    fun changeOrientation() {
        freeformRootView.layoutParams = freeformRootView.layoutParams.apply {
            width = if (freeformConfig.isHangUp) freeformConfig.hangUpWidth else freeformConfig.width
            height = if (freeformConfig.isHangUp) freeformConfig.hangUpHeight else freeformConfig.height
        }
    }

    fun destroy(removeTask: Boolean = true) {
        MLog.i(TAG, "destroy $this")
        uiHandler.post {
            runCatching { windowManager.removeViewImmediate(freeformLayout) }.onFailure { exception -> Log.e(TAG, "removeView failed $exception") }
            SystemServiceHolder.activityTaskManager.unregisterTaskStackListener(freeformTaskStackListener)
            SystemServiceHolder.windowManager.removeRotationWatcher(rotationWatcher)
            MiFreeformServiceHolder.releaseFreeform(this)
            FreeformWindowManager.removeWindow("${appConfig.componentName.packageName},${appConfig.componentName.className},${appConfig.userId}")
            if (removeTask) runCatching { SystemServiceHolder.activityTaskManager.removeTask(freeformTaskStackListener!!.taskId) }.onFailure { exception -> MLog.e(TAG, "removeTask failed $exception") }
        }
    }
}