package io.sunshine0523.freeform.ui.freeform

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.PixelFormat
import android.graphics.SurfaceTexture
import android.os.Build
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
import io.sunshine0523.freeform.service.MiFreeformServiceHolder
import io.sunshine0523.freeform.service.SystemServiceHolder
import io.sunshine0523.freeform.util.MLog
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

class FreeformWindow(
    val handler: Handler,
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
    lateinit var freeformView: TextureView
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
        measureScale()
        if (MiFreeformServiceHolder.ping()) {
            MLog.i(TAG, "FreeformWindow init")
            handler.post { if (!addFreeformView()) destroy("init:addFreeform failed") }
        } else {
            destroy("init:service not running")
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
        if (displayId < 0) {
            MiFreeformServiceHolder.createDisplay(freeformConfig, appConfig, Surface(surfaceTexture), this)
        }
        surfaceTexture.setDefaultBufferSize(freeformConfig.freeformWidth, freeformConfig.freeformHeight)
    }

    override fun onSurfaceTextureSizeChanged(surfaceTexture: SurfaceTexture, width: Int, height: Int) {
        surfaceTexture.setDefaultBufferSize(freeformConfig.freeformWidth, freeformConfig.freeformHeight)
    }

    override fun onSurfaceTextureDestroyed(surfaceTexture: SurfaceTexture): Boolean {
        return true
    }

    override fun onSurfaceTextureUpdated(surfaceTexture: SurfaceTexture) {
        //NOT USED
    }

    override fun onDisplayAdd(displayId: Int) {
        MLog.i(TAG, "onDisplayAdd displayId $displayId")
        handler.post {
            this.displayId = displayId
            freeformTaskStackListener = FreeformTaskStackListener(displayId, this)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                SystemServiceHolder.activityTaskManager.registerTaskStackListener(freeformTaskStackListener)
            } else {
                SystemServiceHolder.activityManager.registerTaskStackListener(freeformTaskStackListener)
            }
            // pendingIntent
            if (appConfig.userId == -100) {
                if (appConfig.pendingIntent == null) destroy("onDisplayAdd:userId=-100, but pendingIntent is null", false)
                else {
                    MiFreeformServiceHolder.startPendingIntent(appConfig.pendingIntent, displayId)
                }
            } else {
                if (MiFreeformServiceHolder.startApp(context, appConfig, displayId).not()) destroy("onDisplayAdd:startApp failed", false)
            }

            val rightView = resourceHolder.getLayoutChildViewByTag<View>(freeformLayout, "rightView")
            if (null == rightView) {
                MLog.e(TAG, "right&rightScale view is null")
                destroy("onDisplayAdd:rightView is null")
                return@post
            }
            rightView.setOnClickListener(RightViewClickListener(displayId))
            rightView.setOnLongClickListener(RightViewLongClickListener(this))
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouch(view: View, event: MotionEvent): Boolean {
        val pointerCoords: Array<MotionEvent.PointerCoords?> = arrayOfNulls(event.pointerCount)
        val pointerProperties: Array<MotionEvent.PointerProperties?> = arrayOfNulls(event.pointerCount)
        for (i in 0 until event.pointerCount) {
            val oldCoords = MotionEvent.PointerCoords()
            val pointerProperty = MotionEvent.PointerProperties()
            event.getPointerCoords(i, oldCoords)
            event.getPointerProperties(i, pointerProperty)
            pointerCoords[i] = oldCoords
            pointerCoords[i]!!.apply {
                x = oldCoords.x * freeformConfig.scale
                y = oldCoords.y * freeformConfig.scale
            }
            pointerProperties[i] = pointerProperty
        }

        val newEvent = MotionEvent.obtain(
            event.downTime,
            event.eventTime,
            event.action,
            event.pointerCount,
            pointerProperties,
            pointerCoords,
            event.metaState,
            event.buttonState,
            event.xPrecision,
            event.yPrecision,
            event.deviceId,
            event.edgeFlags,
            event.source,
            event.flags
        )
        MiFreeformServiceHolder.touch(newEvent, displayId)
        newEvent.recycle()
        if (event.action == MotionEvent.ACTION_UP) checkWindowOnTop()
        return true
    }

    /**
     * get freeform screen dimen / freeform view dimen
     */
    fun measureScale() {
        val widthScale = min(defaultDisplayWidth, defaultDisplayHeight) * 1.0f / min(freeformConfig.width, freeformConfig.height)
        val heightScale = max(defaultDisplayWidth, defaultDisplayHeight) * 1.0f / max(freeformConfig.width, freeformConfig.height)
        freeformConfig.scale = min(widthScale, heightScale)
        freeformConfig.freeformWidth = (freeformConfig.width * freeformConfig.scale).roundToInt()
        freeformConfig.freeformHeight = (freeformConfig.height * freeformConfig.scale).roundToInt()
    }

    /**
     * Called in system handler
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
            destroy("addFreeformView:left&leftScale&rightScale view is null")
            return false
        }
        leftView.setOnClickListener(LeftViewClickListener(this))
        leftView.setOnLongClickListener(LeftViewLongClickListener(this))
        leftScaleView.setOnTouchListener(ScaleTouchListener(this, false))
        rightScaleView.setOnTouchListener(ScaleTouchListener(this))

        freeformView = FreeformTextureView(context).apply {
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
            if (freeformConfig.secure) flags = flags or WindowManager.LayoutParams.FLAG_SECURE
            format = PixelFormat.RGBA_8888
            windowAnimations = android.R.style.Animation_Dialog
        }
        SystemServiceHolder.windowManager.watchRotation(rotationWatcher, Display.DEFAULT_DISPLAY)
        runCatching {
            windowManager.addView(freeformLayout, windowParams)
            FreeformWindowManager.topWindow = getFreeformId()
        }.onFailure {
            MLog.e(TAG, "addView failed: $it")
            return false
        }
        return true
    }

    /**
     * Called in system handler
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
            freeformView.setOnTouchListener(this)
        } else {
            freeformConfig.notInHangUpX = windowParams.x
            freeformConfig.notInHangUpY = windowParams.y
            toHangUp()
            topBarView.visibility = View.GONE
            bottomBarView.visibility = View.GONE
            freeformConfig.isHangUp = true
            val gestureDetector = GestureDetector(context, hangUpGestureListener)
            freeformView.setOnTouchListener { _, event ->
                gestureDetector.onTouchEvent(event)
                if (event.action == MotionEvent.ACTION_UP) makeSureFreeformInScreen()
                true
            }
        }
    }

    /**
     * Called in system handler
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
     * Change freeform orientation
     * Called in system handler
     */
    fun changeOrientation() {
        freeformRootView.layoutParams = freeformRootView.layoutParams.apply {
            width = if (freeformConfig.isHangUp) freeformConfig.hangUpWidth else freeformConfig.width
            height = if (freeformConfig.isHangUp) freeformConfig.hangUpHeight else freeformConfig.height
        }
    }

    fun getFreeformId(): String {
        return "${appConfig.packageName},${appConfig.activityName},${appConfig.userId}"
    }

    fun checkWindowOnTop() {
        if (getFreeformId() != FreeformWindowManager.topWindow) {
            handler.post {
                runCatching {
                    windowManager.removeViewImmediate(freeformLayout)
                    windowManager.addView(freeformLayout, windowParams)
                    FreeformWindowManager.topWindow = getFreeformId()
                }
            }
        }
    }

    fun destroy(callReason: String, removeTask: Boolean = true) {
        MLog.i(TAG, "destroy ${getFreeformId()}, displayId=$displayId, callReason: $callReason")
        handler.post {
            runCatching { windowManager.removeViewImmediate(freeformLayout) }.onFailure { exception -> Log.e(TAG, "removeView failed $exception") }
        }
        if (removeTask) runCatching {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                SystemServiceHolder.activityTaskManager.removeTask(freeformTaskStackListener!!.taskId)
            } else {
                SystemServiceHolder.activityManager.removeTask(freeformTaskStackListener!!.taskId)
            }
            freeformTaskStackListener?.listenTaskRemoved = true
        }.onFailure { exception ->
            MLog.e(TAG, "removeTask failed $exception")
        }
        SystemServiceHolder.windowManager.removeRotationWatcher(rotationWatcher)
        FreeformWindowManager.removeWindow(getFreeformId())
    }
}