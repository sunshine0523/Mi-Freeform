package com.sunshine.freeform.ui.freeform

import android.annotation.SuppressLint
import android.app.*
import android.content.Context
import android.content.Intent
import android.graphics.PixelFormat
import android.graphics.SurfaceTexture
import android.hardware.display.DisplayManager
import android.hardware.display.VirtualDisplay
import android.hardware.input.IInputManager
import android.net.Uri
import android.os.Build
import android.os.SystemClock
import android.provider.Settings
import android.util.Log
import android.view.*
import android.widget.FrameLayout
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.cardview.widget.CardView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.marginBottom
import androidx.core.view.marginTop
import com.sunshine.freeform.R
import com.sunshine.freeform.app.MiFreeform
import com.sunshine.freeform.databinding.ViewFreeformBinding
import kotlinx.coroutines.*
import rikka.shizuku.ShizukuBinderWrapper
import rikka.shizuku.SystemServiceHelper
import java.lang.reflect.Field
import java.lang.reflect.Method
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

class FreeformView(
    override val config: FreeformConfig,
    private val context: Context,
) : FreeformViewAbs(config), View.OnTouchListener {
    //服务
    private val windowManager: WindowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
    private val displayManager: DisplayManager = context.getSystemService(Context.DISPLAY_SERVICE) as DisplayManager
    private var activityTaskManager: IActivityTaskManager? = null
    private var activityManager: IActivityManager? = null
    private var inputManager: IInputManager? = null
    private var iWindowManager: IWindowManager? = null

    //ViewModel
    private val viewModel = FreeformViewModel(context)

    private val scope = MainScope()

    //默认屏幕，用于获取横竖屏状态
    private val defaultDisplay: Display = displayManager.getDisplay(Display.DEFAULT_DISPLAY)

    //界面binding
    private lateinit var binding: ViewFreeformBinding

    //判断是否是初次启动，防止屏幕旋转时再初始化
    private var firstInit = true

    //该小窗是否已经销毁
    private var isDestroy = false

    //是否处于隐藏状态，当打开米窗的正在运行小窗界面时，应当隐藏所有小窗
    private var isHidden = false

    //是否处于后台挂起状态
    private var isBackstage = false

    //小窗中应用的taskId
    private var taskId = -1

    //叠加层Params
    private val windowLayoutParams = WindowManager.LayoutParams()

    //虚拟屏幕
    private lateinit var virtualDisplay: VirtualDisplay

    //物理屏幕方向
    private var screenRotation = defaultDisplay.rotation
    //虚拟屏幕方向，1 竖屏， 0 横屏
    private var virtualDisplayRotation = VIRTUAL_DISPLAY_ROTATION_PORTRAIT

    private val iRotationWatcher = object : IRotationWatcher.Stub() {
        override fun onRotationChanged(rotation: Int) {
            if (rotation != screenRotation) {
                screenRotation = rotation
                scope.launch(Dispatchers.Main) {
                    onScreenOrientationChanged()
                }
            }
        }
    }

//    //屏幕监听
//    private val displayListener = object : DisplayManager.DisplayListener {
//        override fun onDisplayAdded(displayId: Int) {}
//
//        override fun onDisplayRemoved(displayId: Int) {}
//
//        override fun onDisplayChanged(displayId: Int) {
//            if (displayId == Display.DEFAULT_DISPLAY) {
//                val tempScreenRotation = defaultDisplay.rotation
//                if (tempScreenRotation != screenRotation) {
//                    screenRotation = tempScreenRotation
//                    onScreenOrientationChanged()
//                }
//            }
//        }
//    }

    private val screenListener = ScreenListener(context)

    //触摸监听
    private val touchListener = TouchListener()
    private val touchListenerPreQ = TouchListenerPreQ()

    //小窗自由缩放处理
    private val scaleTouchListenerAuto = ScaleTouchListenerAuto()

    //屏幕宽高，不保证大小
    private var realScreenWidth = 0
    private var realScreenHeight = 0

    //小窗的“尺寸”，该尺寸只在小窗内屏幕方向改变时变化
    private var freeformScreenHeight = 0
    private var freeformScreenWidth = 0

    //小窗界面的宽高，该宽高不随着屏幕、小窗方向改变而改变，即h>w恒成立。该尺寸只在物理屏幕方向变化时变化
    private var freeformHeight = 0
    private var freeformWidth = 0

    //max(freeformScreenHeight,freeformScreenWidth)，用于计算scale
    private var maxFreeformScreenSize = 0
    private var minFreeformScreenSize = 0

    //缩放比例
    private var scaleX: Float = 1f
    private var scaleY: Float = 1f

    private val leftGestureDetector = GestureDetector(context, object : GestureDetector.SimpleOnGestureListener() {
        override fun onSingleTapUp(e: MotionEvent): Boolean {
            destroy()
            return true
        }

        override fun onLongPress(e: MotionEvent) {
            moveTaskToDefaultDisplay()
        }
    })

    private val rightGestureDetector = GestureDetector(context, object : GestureDetector.SimpleOnGestureListener() {
        override fun onSingleTapUp(e: MotionEvent): Boolean {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                MiFreeform.me?.getControlService()?.pressBack(virtualDisplay.display.displayId)
            } else {
                pressBackPreQ()
            }

            return true
        }

        override fun onLongPress(e: MotionEvent) {
            toBackstage()
        }
    })

    //新增 手动调整小窗方向 q220904.7
    private val middleGestureDetector = GestureDetector(context, object : GestureDetector.SimpleOnGestureListener() {
        override fun onDoubleTap(e: MotionEvent): Boolean {
            if (config.manualAdjustFreeformRotation) {
                virtualDisplayRotation = if (virtualDisplayRotation == VIRTUAL_DISPLAY_ROTATION_PORTRAIT) {
                    VIRTUAL_DISPLAY_ROTATION_LANDSCAPE
                } else {
                    VIRTUAL_DISPLAY_ROTATION_PORTRAIT
                }
                onFreeFormRotationChanged()
            }
            return false
        }
    })

    private val hangUpTipSize = context.resources.getDimension(R.dimen.hang_up_tip_size)
    //是否处于挂起状态
    private var isHangUp = false
    //挂起位置，0：是否在左，1：是否在上
    private val hangUpPosition = BooleanArray(2)

    @RequiresApi(Build.VERSION_CODES.Q)
    private val taskStackListener = MTaskStackListener()

    private fun initSystemService() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            setDisplayIdMethod = MotionEvent::class.java.getMethod("setDisplayId", Int::class.javaPrimitiveType)
        }
        try {
            activityTaskManager = IActivityTaskManager.Stub.asInterface(ShizukuBinderWrapper(SystemServiceHelper.getSystemService("activity_task")))
            //目前仅支持Android Q及以上版本
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                activityTaskManager?.registerTaskStackListener(taskStackListener)
            }
            activityManager = IActivityManager.Stub.asInterface(ShizukuBinderWrapper(SystemServiceHelper.getSystemService("activity")))
        }catch (e: Exception) {}
        try {
            inputManager = IInputManager.Stub.asInterface(ShizukuBinderWrapper(SystemServiceHelper.getSystemService("input")))
        }catch (e: Exception) {}
        try {
            iWindowManager = IWindowManager.Stub.asInterface(ShizukuBinderWrapper(SystemServiceHelper.getSystemService("window")))
        } catch (e: Exception) {}
    }

    private fun initConfig() {
        config.maxHeight = FreeformHelper.getDefaultHeight(context, defaultDisplay, config.widthHeightRatio)
        //config.widthHeightRatio = ...

        realScreenWidth = context.resources.displayMetrics.widthPixels
        realScreenHeight = context.resources.displayMetrics.heightPixels

        config.freeformDpi = FreeformHelper.getScreenDpi(context)
        freeformHeight = if (FreeformHelper.screenIsPortrait(screenRotation)) (config.maxHeight * 0.75).roundToInt() else (config.maxHeight * 0.8).roundToInt()
        freeformWidth = (freeformHeight * config.widthHeightRatio).roundToInt()

        //优化 QQ和微信也支持缩放了 q220917.1
        freeformScreenHeight = FreeformHelper.getDefaultHeight(context, defaultDisplay, config.widthHeightRatio)
        if (!config.useCustomConfig) {
            freeformScreenHeight -=
                if (FreeformHelper.screenIsPortrait(screenRotation)) viewModel.getIntSp("freeform_scale_portrait", 0)
                else viewModel.getIntSp("freeform_scale_landscape", 0)

            freeformScreenHeight = max(1, freeformScreenHeight)
        }
        freeformScreenWidth = (freeformScreenHeight * config.widthHeightRatio).roundToInt()
        maxFreeformScreenSize = freeformScreenHeight
        minFreeformScreenSize = freeformScreenWidth

        if (config.useCustomConfig) return
        //---------------客制化配置-----------------

        config.rememberPosition = viewModel.getBooleanSp("remember_freeform_position", false)
        if (config.rememberPosition) {
            config.rememberX = if (FreeformHelper.screenIsPortrait(screenRotation)) {
                viewModel.getIntSp(REMEMBER_X, 0)
            } else {
                viewModel.getIntSp(REMEMBER_LAND_X, 0)
            }
            config.rememberY = if (FreeformHelper.screenIsPortrait(screenRotation)) {
                viewModel.getIntSp(REMEMBER_Y, 0)
            } else {
                viewModel.getIntSp(REMEMBER_LAND_Y, 0)
            }
            var rememberHeight = if (FreeformHelper.screenIsPortrait(screenRotation)) {
                viewModel.getIntSp(REMEMBER_HEIGHT, freeformHeight)
            } else {
                viewModel.getIntSp(REMEMBER_LAND_HEIGHT, freeformHeight)
            }
            //修复 尝试修复记录的小窗大小异常的问题 q220904.1
            rememberHeight = min(max(MIN_HEIGHT, rememberHeight), config.maxHeight)
            freeformHeight = rememberHeight
            freeformWidth = (freeformHeight * config.widthHeightRatio).roundToInt()
            scaleX = freeformHeight / maxFreeformScreenSize.toFloat()
        }

        //修复 开启记录位置选项后触控错位的问题 q220925.3
        scaleX = freeformHeight / maxFreeformScreenSize.toFloat()
        scaleY = scaleX

        config.useSuiRefuseToFullScreen = viewModel.getBooleanSp("use_sui_refuse_to_fullscreen", false)
        config.changeDpi = viewModel.getBooleanSp("change_dpi", false)
        config.manualAdjustFreeformRotation = viewModel.getBooleanSp("manual_adjust_freeform_rotation", false)
        config.autoScale = viewModel.getBooleanSp("auto_scale", false)
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun initView() {
        binding = ViewFreeformBinding.bind(LayoutInflater.from(context).inflate(R.layout.view_freeform, null, false))

        binding.bottomBar.leftView.setOnTouchListener(this)
        binding.bottomBar.middleView.setOnTouchListener(this)
        binding.bottomBar.rightView.setOnTouchListener(this)
        if (config.autoScale) {
            binding.leftScale.setOnTouchListener(scaleTouchListenerAuto)
            binding.rightScale.setOnTouchListener(scaleTouchListenerAuto)
        } else {
            binding.leftScale.setOnTouchListener(this)
            binding.rightScale.setOnTouchListener(this)
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            binding.textureView.setOnTouchListener(touchListener)
        } else {
            binding.textureView.setOnTouchListener(touchListenerPreQ)
        }

        binding.textureView.alpha = 0f

        initDisplay()
    }

    private fun initDisplay() {
        try {
            virtualDisplay = displayManager.createVirtualDisplay(
                "MiFreeform@${config.packageName}@${config.userId}",
                freeformScreenWidth,
                freeformScreenHeight,
                config.freeformDpi,
                null,
                DisplayManager.VIRTUAL_DISPLAY_FLAG_PRESENTATION
            )
        } catch (e: Exception) {
            Toast.makeText(context, context.getString(R.string.create_display_fail), Toast.LENGTH_SHORT).show()
            return
        }

        screenListener.begin(object : ScreenListener.ScreenStateListener {
            override fun onScreenOn() {}

            //关闭屏幕隐藏小窗
            override fun onScreenOff() {
                //挂起状态无需更新
                //修复 在有正在运行程序的情况下锁屏，米窗崩溃的问题 q220902.1
                //优化 锁屏后小窗的状态 q220917.3
                if (!isBackstage) {
                    binding.root.alpha = 0f
                    windowLayoutParams.flags =
                            WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS or
                            WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE
                            WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED or
                            WindowManager.LayoutParams.FLAG_ALT_FOCUSABLE_IM
                    windowManager.updateViewLayout(binding.root, windowLayoutParams)
                }
            }

            //解锁恢复小窗
            override fun onUserPresent() {
                //挂起状态无需更新
                if (!isBackstage) {
                    binding.root.alpha = 1f
                    windowLayoutParams.flags =
                            WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS or
                            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                            WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
                            WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED or
                            WindowManager.LayoutParams.FLAG_ALT_FOCUSABLE_IM

                    windowManager.updateViewLayout(binding.root, windowLayoutParams)
                }
            }
        })

        initOrientationChangedListener()
        initTextureViewListener()

        showWindow()
    }

    private fun initOrientationChangedListener() {
        iWindowManager?.watchRotation(iRotationWatcher, Display.DEFAULT_DISPLAY)
    }

    private fun initTextureViewListener() {
        //冷启动监听
        var updateFrameCount = 0
        var initFinish = false

        binding.textureView.surfaceTextureListener = object : TextureView.SurfaceTextureListener {
            override fun onSurfaceTextureAvailable(
                surface: SurfaceTexture,
                width: Int,
                height: Int
            ) {
                surface.setDefaultBufferSize(freeformScreenWidth, freeformScreenHeight)
                virtualDisplay.surface = Surface(surface)

                if (firstInit) {
                    if (MiFreeform.me?.getControlService()?.execShell("am start -n ${config.packageName}/${config.activityName} --user ${config.userId} --display ${virtualDisplay.display.displayId}", false) == true) {
                        FreeformHelper.addFreeformToSet(this@FreeformView)
                        firstInit = false
                    }
                    //启动失败
                    else {
                        destroy()
                    }
                }
            }

            override fun onSurfaceTextureSizeChanged(
                surface: SurfaceTexture,
                width: Int,
                height: Int
            ) {
                surface.setDefaultBufferSize(freeformScreenWidth, freeformScreenHeight)
            }

            override fun onSurfaceTextureDestroyed(surface: SurfaceTexture): Boolean {
                return true
            }

            override fun onSurfaceTextureUpdated(surface: SurfaceTexture) {
                if (!initFinish) {
                    ++updateFrameCount
                    if (updateFrameCount > 2) {
                        binding.lottieView.cancelAnimation()
                        binding.lottieView.animate().alpha(0f).setDuration(200).start()
                        binding.textureView.animate().alpha(1f).setDuration(200).start()
                        initFinish = true
                    }
                }
            }
        }
    }

    private fun showWindow() {
        if (virtualDisplayRotation == VIRTUAL_DISPLAY_ROTATION_PORTRAIT) {
            windowLayoutParams.apply {
                width = freeformWidth + 2 * context.resources.getDimension(R.dimen.scale_view_size).toInt()
                height = freeformHeight + context.resources.getDimension(R.dimen.top_bar_height).toInt() + context.resources.getDimension(R.dimen.bottom_bar_height).toInt() + context.resources.getDimension(R.dimen.freeform_shadow).toInt()
            }
        } else {
            windowLayoutParams.apply {
                width = freeformHeight  + 2 * context.resources.getDimension(R.dimen.scale_view_size).toInt()
                height = freeformWidth + context.resources.getDimension(R.dimen.top_bar_height).toInt() + context.resources.getDimension(R.dimen.bottom_bar_height).toInt() + context.resources.getDimension(R.dimen.freeform_shadow).toInt()
            }
        }
        windowLayoutParams.apply {
            type = WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY
            flags =
                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS or
                        WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                        WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
                        WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED or
                        WindowManager.LayoutParams.FLAG_ALT_FOCUSABLE_IM
            format = PixelFormat.RGBA_8888
            windowAnimations = android.R.style.Animation_Dialog
        }

        setWindowNoUpdateAnimation()

        //恢复记录的位置
        if (config.rememberPosition) {
            windowLayoutParams.apply {
                x = config.rememberX
                y = config.rememberY
            }
        } else {
            //横屏移动到屏幕左侧显示小窗
            if (screenRotation == Surface.ROTATION_90 || screenRotation == Surface.ROTATION_270) {
                windowLayoutParams.apply {
                    x = (width - realScreenWidth) / 2
                    //往上移动一些
                    y = 0
                }
            }
        }

        try {
            windowManager.addView(binding.root, windowLayoutParams)
        } catch (e: Exception) {
            try {
                windowManager.removeViewImmediate(binding.root)
            } catch (e: Exception) {}

            if (Settings.canDrawOverlays(context)) {
                windowManager.addView(binding.root, windowLayoutParams.apply {
                    type = WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
                })
            } else {
                destroy()
                try {
                    Toast.makeText(context, context.getString(R.string.request_overlay_permission), Toast.LENGTH_LONG).show()
                    val intent = Intent(
                        Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                        Uri.parse("package:${context.packageName}")
                    )
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    context.startActivity(
                        intent
                    )
                } catch (e: Exception) {
                    Toast.makeText(context, context.getString(R.string.request_overlay_permission_fail), Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    /**
     * 禁用更新过渡动画
     */
    private fun setWindowNoUpdateAnimation() {
        val classname = "android.view.WindowManager\$LayoutParams"
        try {
            val layoutParamsClass: Class<*> = Class.forName(classname)
            val privateFlags: Field = layoutParamsClass.getField("privateFlags")
            val noAnim: Field = layoutParamsClass.getField("PRIVATE_FLAG_NO_MOVE_ANIMATION")
            var privateFlagsValue: Int = privateFlags.getInt(windowLayoutParams)
            val noAnimFlag: Int = noAnim.getInt(windowLayoutParams)
            privateFlagsValue = privateFlagsValue or noAnimFlag
            privateFlags.setInt(windowLayoutParams, privateFlagsValue)
        } catch (e: Exception) { }
    }

    private fun setWindowEnableUpdateAnimation() {
        val classname = "android.view.WindowManager\$LayoutParams"
        try {
            val layoutParamsClass: Class<*> = Class.forName(classname)
            val privateFlags: Field = layoutParamsClass.getField("privateFlags")
            val noAnim: Field = layoutParamsClass.getField("PRIVATE_FLAG_NO_MOVE_ANIMATION")
            var privateFlagsValue: Int = privateFlags.getInt(windowLayoutParams)
            val noAnimFlag: Int = noAnim.getInt(windowLayoutParams)
            privateFlagsValue = privateFlagsValue and noAnimFlag.inv()
            privateFlags.setInt(windowLayoutParams, privateFlagsValue)
        } catch (e: Exception) { }
    }

    private fun onFreeFormRotationChanged() {
        val tempHeight = max(freeformScreenHeight, freeformScreenWidth)
        val tempWidth = min(freeformScreenHeight, freeformScreenWidth)

        maxFreeformScreenSize = tempHeight
        minFreeformScreenSize = tempWidth

        if (virtualDisplayRotation == VIRTUAL_DISPLAY_ROTATION_PORTRAIT) {
            freeformScreenHeight = tempHeight
            freeformScreenWidth = tempWidth
        } else {
            freeformScreenHeight = tempWidth
            freeformScreenWidth = tempHeight

            //优化 小窗内部旋转时，如果旋转后的尺寸大于屏幕尺寸，则进行调整 q220904.3
            //小窗横屏-物理屏幕竖屏
            if (FreeformHelper.screenIsPortrait(screenRotation)) {
                if (freeformHeight > realScreenWidth) {
                    freeformHeight = realScreenWidth
                    freeformWidth = (freeformHeight * config.widthHeightRatio).roundToInt()
                    scaleX = freeformHeight / maxFreeformScreenSize.toFloat()
                    scaleY = scaleX
                }
            }
        }

        resizeVirtualDisplay()
        updateWindowSize(false)
    }

    private fun onScreenOrientationChanged() {
        if (screenRotation == Surface.ROTATION_0 || screenRotation == Surface.ROTATION_180) {
            realScreenHeight = max(context.resources.displayMetrics.widthPixels, context.resources.displayMetrics.heightPixels)
            realScreenWidth = min(context.resources.displayMetrics.widthPixels, context.resources.displayMetrics.heightPixels)
        } else {
            realScreenWidth = max(context.resources.displayMetrics.widthPixels, context.resources.displayMetrics.heightPixels)
            realScreenHeight = min(context.resources.displayMetrics.widthPixels, context.resources.displayMetrics.heightPixels)
        }

        config.maxHeight = FreeformHelper.getDefaultHeight(context, defaultDisplay, config.widthHeightRatio)

        //该赋值是不需要的，maxFreeformScreenSize只需要与max(freeformScreenHeight, freeformScreenWidth)同步即可 q220908.1
        //maxFreeformScreenSize = config.maxHeight

        freeformHeight = if (FreeformHelper.screenIsPortrait(defaultDisplay.rotation)) (config.maxHeight * 0.75).roundToInt() else (config.maxHeight * 0.8).roundToInt()
        freeformWidth = (freeformHeight * config.widthHeightRatio).roundToInt()

        if (config.changeDpi) {
            freeformScreenHeight = FreeformHelper.getDefaultHeight(context, defaultDisplay, config.widthHeightRatio)
            freeformScreenHeight -=
                if (FreeformHelper.screenIsPortrait(screenRotation)) viewModel.getIntSp("freeform_scale_portrait", 0)
                else viewModel.getIntSp("freeform_scale_landscape", 0)
            freeformScreenHeight = max(1, freeformScreenHeight)
            freeformScreenWidth = (freeformScreenHeight * config.widthHeightRatio).roundToInt()
            maxFreeformScreenSize = freeformScreenHeight
            minFreeformScreenSize = freeformScreenWidth
            scaleX = freeformHeight / maxFreeformScreenSize.toFloat()
            scaleY = scaleX

            //修复转屏后屏幕dpi不正确的问题 q220908.2
            val tempFreeformScreenHeight = freeformScreenHeight
            val tempFreeformScreenWidth = freeformScreenWidth

            maxFreeformScreenSize = tempFreeformScreenHeight
            minFreeformScreenSize = tempFreeformScreenWidth

            if (virtualDisplayRotation == VIRTUAL_DISPLAY_ROTATION_PORTRAIT) {
                freeformScreenHeight = max(tempFreeformScreenHeight, tempFreeformScreenWidth)
                freeformScreenWidth = min(tempFreeformScreenHeight, tempFreeformScreenWidth)
            } else {
                freeformScreenHeight = min(tempFreeformScreenHeight, tempFreeformScreenWidth)
                freeformScreenWidth = max(tempFreeformScreenHeight, tempFreeformScreenWidth)
            }

            resizeVirtualDisplay()
        }

        windowLayoutParams.apply {
            x = 0
            y = 0
        }

        updateWindowSize(true)

        if (isHangUp) toHangUp()
    }

    private fun resizeVirtualDisplay() {
        virtualDisplay.resize(
            freeformScreenWidth,
            freeformScreenHeight,
            config.freeformDpi
        )
    }

    private fun updateWindowSize(orientationChanged: Boolean) {
        scaleX = freeformHeight / maxFreeformScreenSize.toFloat()
        scaleY = freeformWidth / minFreeformScreenSize.toFloat()
        if (virtualDisplayRotation == VIRTUAL_DISPLAY_ROTATION_PORTRAIT) {
            windowLayoutParams.apply {
                width = freeformWidth + 2 * context.resources.getDimension(R.dimen.scale_view_size).toInt()
                height = freeformHeight + context.resources.getDimension(R.dimen.top_bar_height).toInt() + context.resources.getDimension(R.dimen.bottom_bar_height).toInt() + context.resources.getDimension(R.dimen.freeform_shadow).toInt()
            }
        } else {
            windowLayoutParams.apply {
                width = freeformHeight + 2 * context.resources.getDimension(R.dimen.scale_view_size).toInt()
                height = freeformWidth + context.resources.getDimension(R.dimen.bottom_bar_height).toInt() + context.resources.getDimension(R.dimen.freeform_shadow).toInt()
            }
        }

        if (orientationChanged) {
            //横屏移动到屏幕左侧显示小窗
            if (screenRotation == Surface.ROTATION_90 || screenRotation == Surface.ROTATION_270) {
                windowLayoutParams.apply {
                    x = (width - realScreenWidth) / 2
                }
            }
        }

        //非后台挂起状态才进行刷新
        if (!isBackstage) {
            windowManager.updateViewLayout(
                binding.root,
                windowLayoutParams
            )
        }
    }

    /**
     * 如果小窗无法控制了，可以尝试移动到屏幕中心以控制
     */
    override fun toScreenCenter() {
        windowLayoutParams.x = 0
        windowLayoutParams.y = 0
    }

    override fun moveToFirst() {
        windowManager.removeViewImmediate(binding.root)
        windowManager.addView(binding.root, windowLayoutParams)
        FreeformHelper.addFreeformToSet(this)
    }

    /**
     * 隐藏小窗到后台
     */
    private fun toBackstage() {
        windowManager.removeViewImmediate(binding.root)
        FreeformHelper.removeFreeformFromSet(this)
        FreeformHelper.addMiniFreeformToSet(this)
        isBackstage = true
    }

    /**
     * 从后台恢复
     */
    override fun fromBackstage() {
        windowManager.addView(binding.root, windowLayoutParams)
        FreeformHelper.removeMiniFreeformFromSet(this)
        FreeformHelper.addFreeformToSet(this)
        isBackstage = false
    }

    /**
     * 尝试将小窗内容全屏
     */
    private fun moveTaskToDefaultDisplay() {
        isDestroy = true
        virtualDisplay.resize(realScreenWidth, realScreenHeight, FreeformHelper.getScreenDpi(context))
        MiFreeform.me?.getControlService()?.execShell("am start -n ${config.packageName}/${config.activityName} --user ${config.userId} --display 0", false)
        destroy()
    }

    /**
     * 通知windowManager刷新缩放后的界面
     */
    private fun notifyWindowScale() {
        binding.root.scaleX = 1f
        binding.root.scaleY = 1f
        scaleX = freeformHeight / maxFreeformScreenSize.toFloat()
        scaleY = freeformWidth / minFreeformScreenSize.toFloat()
        updateWindowSize(false)
    }

    //按下时的坐标
    private var lastX = -1f
    private var lastY = -1f
    //当前正在操作的界面id
    private var touchId = -1
    @SuppressLint("ClickableViewAccessibility")
    override fun onTouch(v: View, event: MotionEvent): Boolean {
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                if (FreeformHelper.isShowingFirst(this)) {
                    handleDownEvent(v, event)
                } else {
                    //不是处于最上层要移动到最上层
                    moveToFirst()
                }
            }
            MotionEvent.ACTION_MOVE -> {
                handleMoveEvent(v, event)
            }
            MotionEvent.ACTION_UP -> {
                handleUpEvent(v, event)
            }
        }
        return true
    }

    private fun handleDownEvent(v: View, event: MotionEvent) {
        if (touchId == -1) touchId = v.id

        lastX = event.rawX
        lastY = event.rawY
        when(v.id) {
            //左键监听，单击关闭，长按全屏
            R.id.leftView -> {
                if (touchId == R.id.leftView) leftGestureDetector.onTouchEvent(event)
            }
            R.id.middleView -> {
                middleGestureDetector.onTouchEvent(event)
            }
            //右键监听，点击返回，长按后台挂起
            R.id.rightView -> {
                if (touchId == R.id.rightView) rightGestureDetector.onTouchEvent(event)
            }

            R.id.leftScale, R.id.rightScale -> {
                beforeScaleHeight = freeformHeight
            }
        }
    }

    private fun handleMoveEvent(v: View, event: MotionEvent) {
        when(v.id) {
            //左键监听，单击关闭，双击全屏
            R.id.leftView -> {
                if (touchId == R.id.leftView) leftGestureDetector.onTouchEvent(event)
            }
            R.id.rightView -> {
                if (touchId == R.id.rightView) rightGestureDetector.onTouchEvent(event)
            }
            R.id.middleView -> {
                if (touchId == R.id.middleView) {
                    val dx = event.rawX - lastX
                    val dy = event.rawY - lastY

                    handleMove(dx, dy)
                    handleHangUp(event.rawX, event.rawY)
                    lastX = event.rawX
                    lastY = event.rawY

                    middleGestureDetector.onTouchEvent(event)
                }
            }
            R.id.leftScale -> {
                if (touchId == R.id.leftScale) {
                    val dx = event.rawX - lastX
                    val dy = event.rawY - lastY
                    handleScaleLeft(dx, dy)
                    lastX = event.rawX
                    lastY = event.rawY
                }
            }
            R.id.rightScale -> {
                if (touchId == R.id.rightScale) {
                    val dx = event.rawX - lastX
                    val dy = event.rawY - lastY
                    handleScaleRight(dx, dy)
                    lastX = event.rawX
                    lastY = event.rawY
                }
            }
        }
    }

    private fun handleUpEvent(v: View, event: MotionEvent) {
        when (v.id) {
            R.id.middleView -> {
                middleGestureDetector.onTouchEvent(event)
            }
            //左键监听，单击关闭，双击全屏
            R.id.leftView -> {
                if (touchId == R.id.leftView) leftGestureDetector.onTouchEvent(event)
            }
            //右按键返回
            R.id.rightView -> {
                if (touchId == R.id.rightView) rightGestureDetector.onTouchEvent(event)
            }
            R.id.leftScale, R.id.rightScale -> {
                notifyWindowScale()
            }
        }
        touchId = -1
    }

    //指本次与缩放前的比例
    private var mScale = 1f
    private var beforeScaleHeight = freeformHeight

    /**
     * 缩放处理
     * 优化 尝试更加流畅的缩放动画 q220904.8
     */
    private fun handleScaleLeft(dx: Float, dy: Float) {
        //缩小
        if (dx > 0 && dy < 0) {
            val tempHeight = freeformHeight + dy
            val tempWidth = freeformWidth - dx
            //在尺寸内
            if (tempHeight > MIN_HEIGHT && tempHeight <= config.maxHeight && tempWidth > MIN_WIDTH && tempWidth < realScreenWidth) {
                //优化 尝试更加跟手的缩放操作 q220904.6
                val maxMove = max(abs(dx), abs(dy)).roundToInt()
                freeformHeight -= maxMove
                freeformWidth = (freeformHeight * config.widthHeightRatio).roundToInt()

                if (freeformHeight > beforeScaleHeight) {
                    beforeScaleHeight = freeformHeight
                    notifyWindowScale()
                } else {
                    mScale = freeformHeight / beforeScaleHeight.toFloat()
                    binding.root.scaleX = mScale
                    binding.root.scaleY = mScale
                }
            }
        }
        //放大
        else if (dx < 0 && dy > 0) {
            val tempHeight = freeformHeight + dy
            val tempWidth = freeformWidth - dx
            //在尺寸内
            if (tempHeight > MIN_HEIGHT && tempHeight <= config.maxHeight && tempWidth > MIN_WIDTH && tempWidth < realScreenWidth) {
                val maxMove = max(abs(dx), abs(dy)).roundToInt()
                freeformHeight += maxMove
                freeformWidth = (freeformHeight * config.widthHeightRatio).roundToInt()

                if (freeformHeight > beforeScaleHeight) {
                    beforeScaleHeight = freeformHeight
                    notifyWindowScale()
                } else {
                    mScale = freeformHeight / beforeScaleHeight.toFloat()
                    binding.root.scaleX = mScale
                    binding.root.scaleY = mScale
                }
            }
        }
    }

    /**
     * 缩放处理
     */
    private fun handleScaleRight(dx: Float, dy: Float) {
        //缩小
        if (dx < 0 && dy < 0) {
            val tempHeight = freeformHeight + dy
            val tempWidth = freeformWidth + dx
            //在尺寸内
            if (tempHeight > MIN_HEIGHT && tempHeight <= config.maxHeight && tempWidth > MIN_WIDTH && tempWidth < realScreenWidth) {
                val maxMove = max(abs(dx), abs(dy)).roundToInt()
                freeformHeight -= maxMove
                freeformWidth = (freeformHeight * config.widthHeightRatio).roundToInt()

                if (freeformHeight > beforeScaleHeight) {
                    beforeScaleHeight = freeformHeight
                    notifyWindowScale()
                } else {
                    mScale = freeformHeight / beforeScaleHeight.toFloat()
                    binding.root.scaleX = mScale
                    binding.root.scaleY = mScale
                }
            }
        }
        //放大
        else if (dx > 0 && dy > 0) {
            val tempHeight = freeformHeight + dy
            val tempWidth = freeformWidth + dx
            //在尺寸内
            if (tempHeight > MIN_HEIGHT && tempHeight <= config.maxHeight && tempWidth > MIN_WIDTH && tempWidth < realScreenWidth) {
                val maxMove = max(abs(dx), abs(dy)).roundToInt()
                freeformHeight += maxMove
                freeformWidth = (freeformHeight * config.widthHeightRatio).roundToInt()

                if (freeformHeight > beforeScaleHeight) {
                    beforeScaleHeight = freeformHeight
                    notifyWindowScale()
                } else {
                    mScale = freeformHeight / beforeScaleHeight.toFloat()
                    binding.root.scaleX = mScale
                    binding.root.scaleY = mScale
                }
            }
        }
    }

    /**
     * 移动位置处理
     */
    private fun handleMove(dx: Float, dy: Float) {
        windowManager.updateViewLayout(binding.root, windowLayoutParams.apply {
            x = (x + dx).roundToInt()
            y = ((y + dy).roundToInt())
        })
    }

    private var setDisplayIdMethod: Method? = null

    /**
     * 挂起处理
     */
    private fun handleHangUp(rawX: Float, rawY: Float) {

        if (rawY >= realScreenHeight - hangUpTipSize) {
            //左下角挂起
            if (rawX <= hangUpTipSize) {
                hangUpPosition[0] = true
                hangUpPosition[1] = false
                toHangUp()
            }
            //右下角挂起
            else if (rawX >= realScreenWidth - hangUpTipSize) {
                hangUpPosition[0] = false
                hangUpPosition[1] = false
                toHangUp()
            }
        } else if (rawY <= hangUpTipSize) {
            //左上角挂起
            if (rawX <= hangUpTipSize) {
                hangUpPosition[0] = true
                hangUpPosition[1] = true
                toHangUp()
            }
            //右上角挂起
            else if (rawX >= realScreenWidth - hangUpTipSize) {
                hangUpPosition[0] = false
                hangUpPosition[1] = true
                toHangUp()
            }
        }
    }

    /**
     * 挂起
     */
    @SuppressLint("ClickableViewAccessibility")
    private fun toHangUp() {
        //隐藏底栏
        binding.bottomBar.root.visibility = View.GONE

        //将阴影预留调整为0，可以防止挂起时缩放比例有问题 q220925.1
        val layoutParams = binding.freeformRoot.layoutParams as ConstraintLayout.LayoutParams
        layoutParams.bottomMargin = 0
        layoutParams.topMargin = 0
        binding.freeformRoot.layoutParams = layoutParams

        //阻止控制
        binding.leftScale.setOnTouchListener(null)
        binding.rightScale.setOnTouchListener(null)

        binding.textureView.setOnTouchListener { _, event ->
            if (event.action == MotionEvent.ACTION_UP) {
                binding.bottomBar.root.visibility = View.VISIBLE

                val layoutParams = binding.freeformRoot.layoutParams as ConstraintLayout.LayoutParams
                layoutParams.bottomMargin = context.resources.getDimension(R.dimen.freeform_shadow_height).roundToInt()
                layoutParams.topMargin = context.resources.getDimension(R.dimen.freeform_shadow).roundToInt()
                binding.freeformRoot.layoutParams = layoutParams

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    binding.textureView.setOnTouchListener(touchListener)
                } else {
                    binding.textureView.setOnTouchListener(touchListenerPreQ)
                }
                if (config.autoScale) {
                    binding.leftScale.setOnTouchListener(scaleTouchListenerAuto)
                    binding.rightScale.setOnTouchListener(scaleTouchListenerAuto)
                } else {
                    binding.leftScale.setOnTouchListener(this)
                    binding.rightScale.setOnTouchListener(this)
                }

                if (virtualDisplayRotation == VIRTUAL_DISPLAY_ROTATION_PORTRAIT) {
                    windowLayoutParams.apply {
                        //修复 挂起后再恢复小窗，小窗缩放异常的问题 q220904.4
                        width = freeformWidth + 2 * context.resources.getDimension(R.dimen.scale_view_size).toInt()
                        height = freeformHeight + context.resources.getDimension(R.dimen.top_bar_height).toInt() + context.resources.getDimension(R.dimen.bottom_bar_height).toInt() + context.resources.getDimension(R.dimen.freeform_shadow).toInt()
                        x = 0
                        y = 0
                    }
                    //横屏移动到屏幕左侧显示小窗
                    if (!FreeformHelper.screenIsPortrait(screenRotation)) {
                        windowLayoutParams.apply {
                            x = (width - realScreenWidth) / 2
                            //往上移动一些
                            y = 0
                        }
                    }

                } else {
                    windowLayoutParams.apply {
                        width = freeformHeight + 2 * context.resources.getDimension(R.dimen.scale_view_size).toInt()
                        height = freeformWidth + context.resources.getDimension(R.dimen.bottom_bar_height).toInt() + context.resources.getDimension(R.dimen.freeform_shadow).toInt()
                        x = 0
                        y = 0
                    }
                    //横屏移动到屏幕左侧显示小窗
                    if (!FreeformHelper.screenIsPortrait(screenRotation)) {
                        windowLayoutParams.apply {
                            x = (width - realScreenWidth) / 2
                            //往上移动一些
                            y = 0
                        }
                    }
                }
                windowManager.updateViewLayout(binding.root, windowLayoutParams)
                isHangUp = false

                //解冻屏幕方向
                //iWindowManager?.thawDisplayRotation(virtualDisplay.display.displayId)

                setWindowNoUpdateAnimation()
            }
            true
        }

        setWindowEnableUpdateAnimation()
        if (virtualDisplayRotation == VIRTUAL_DISPLAY_ROTATION_PORTRAIT) {
            windowManager.updateViewLayout(binding.root, windowLayoutParams.apply {
                width = (HANGUP_HEIGHT * config.widthHeightRatio).roundToInt() + 2 * context.resources.getDimension(R.dimen.scale_view_size).toInt()
                height = HANGUP_HEIGHT
                x = if (hangUpPosition[0]) ((realScreenWidth - HANGUP_HEIGHT * config.widthHeightRatio) / -2).roundToInt() else ((realScreenWidth - HANGUP_HEIGHT * config.widthHeightRatio) / 2).roundToInt()
                y = if (hangUpPosition[1]) (HANGUP_HEIGHT - realScreenHeight) / 2 else (realScreenHeight - HANGUP_HEIGHT) / 2
            })
        } else {
            windowManager.updateViewLayout(
                binding.root,
                windowLayoutParams.apply {
                    width = HANGUP_HEIGHT + 2 * context.resources.getDimension(R.dimen.scale_view_size).toInt()
                    height = (HANGUP_HEIGHT * config.widthHeightRatio).roundToInt()
                    //优化 横屏小窗挂起的位置 q220903.1
                    x = if (hangUpPosition[0]) (realScreenWidth - HANGUP_HEIGHT) / -2 else (realScreenWidth - HANGUP_HEIGHT) / 2
                    y = if (hangUpPosition[1]) ((HANGUP_HEIGHT * config.widthHeightRatio - realScreenHeight) / 2).roundToInt() else ((realScreenHeight - HANGUP_HEIGHT * config.widthHeightRatio) / 2).roundToInt()
                }
            )
        }

        //挂起时尝试冻结屏幕方向。可能无效 q220904.4
        //iWindowManager?.freezeDisplayRotation(virtualDisplay.display.displayId, -1)
        isHangUp = true
    }

    private fun pressBackPreQ() {
        val down = KeyEvent(
            SystemClock.uptimeMillis(),
            SystemClock.uptimeMillis(),
            KeyEvent.ACTION_DOWN,
            KeyEvent.KEYCODE_BACK,
            0
        )
        val up = KeyEvent(
            SystemClock.uptimeMillis(),
            SystemClock.uptimeMillis(),
            KeyEvent.ACTION_UP,
            KeyEvent.KEYCODE_BACK,
            0
        )

        try {
            KeyEvent::class.java.getMethod("setSource", Int::class.javaPrimitiveType)
                .invoke(down, InputDevice.SOURCE_KEYBOARD)
            inputManager?.injectInputEvent(down, virtualDisplay.display.displayId)

            KeyEvent::class.java.getMethod("setSource", Int::class.javaPrimitiveType)
                .invoke(up, InputDevice.SOURCE_KEYBOARD)
            inputManager?.injectInputEvent(up, virtualDisplay.display.displayId)
        } catch (e: Exception) {

        }
    }

    private fun destroy() {
        //记录位置
        if (viewModel.getBooleanSp("remember_freeform_position", false)) {
            val sp = context.getSharedPreferences(MiFreeform.APP_SETTINGS_NAME, Context.MODE_PRIVATE)
            if (screenRotation == Surface.ROTATION_90 || screenRotation == Surface.ROTATION_270) {
                sp.edit()
                    .putInt(REMEMBER_LAND_X, windowLayoutParams.x)
                    .putInt(REMEMBER_LAND_Y, windowLayoutParams.y)
                    .putInt(REMEMBER_LAND_HEIGHT, freeformHeight)
                    .apply()
            } else {
                sp.edit()
                    .putInt(REMEMBER_X, windowLayoutParams.x)
                    .putInt(REMEMBER_Y, windowLayoutParams.y)
                    .putInt(REMEMBER_HEIGHT, freeformHeight)
                    .apply()
            }
        }

        isDestroy = true
        try {
            windowManager.removeViewImmediate(binding.root)
        }catch (e: Exception) { }
        virtualDisplay.surface.release()
        virtualDisplay.release()

        try {
            iWindowManager?.removeRotationWatcher(iRotationWatcher)
        } catch (e: Exception) {}


        try {
            screenListener.unregisterListener()
        } catch (e: Exception) {}

        //移除小窗管理
        FreeformHelper.removeFreeformFromSet(this)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            activityTaskManager?.unregisterTaskStackListener(taskStackListener)
        }
    }

    init {
        if (MiFreeform.me?.getControlService()?.asBinder()?.pingBinder() == true) {
            //尝试恢复小窗状态
            //优化 如果小窗移动到屏幕外导致无法控制，可以尝试从侧边栏再次点击该应用以移动到屏幕中心 q220917.4
            if (FreeformHelper.isAppInFreeform(config.packageName, config.userId)) {
                val freeformView = FreeformHelper.getFreeformStackSet().getByPackageName(config.packageName, config.userId)
                freeformView?.toScreenCenter()
                freeformView?.moveToFirst()
            } else if (FreeformHelper.isAppInMiniFreeform(config.packageName, config.userId)) {
                FreeformHelper.getMiniFreeformStackSet().getByPackageName(config.packageName, config.userId)?.fromBackstage()
            } else {
                initSystemService()
                initConfig()

                //youtube单独适配
                if (config.packageName == YOUTUBE) {
                    config.activityName = YOUTUBE_ACTIVITY
                }
//                if (config.packageName == QQ) {
//                    config.freeformDpi = FreeformHelper.getScreenDpi(context)
//                }
                initView()
            }
        }
        else {
            MiFreeform.me?.initShizuku()
            Toast.makeText(context, context.getString(R.string.service_not_running), Toast.LENGTH_SHORT).show()
        }
    }

    //优化 将触摸设置为一等公民，以支持多点触控，也可以看一下为什么那样，多点触控就不支持了... q220906.1
    private inner class TouchListener : View.OnTouchListener{
        @SuppressLint("ClickableViewAccessibility")
        override fun onTouch(v: View, event: MotionEvent): Boolean {
            handleTouch(event)
            when(event.action) {
                MotionEvent.ACTION_DOWN -> {
                    if (FreeformHelper.isShowingFirst(this@FreeformView)) {
                        touchId = R.id.textureView
                    } else {
                        moveToFirst()
                    }
                }
                MotionEvent.ACTION_UP -> {
                    touchId = -1
                }
            }
            return true
        }

        /**
         * 触控处理
         */
        private fun handleTouch(event: MotionEvent) {
            val pointerCoords: Array<MotionEvent.PointerCoords?> = arrayOfNulls(event.pointerCount)
            val pointerProperties: Array<MotionEvent.PointerProperties?> = arrayOfNulls(event.pointerCount)
            for (i in 0 until event.pointerCount) {
                val oldCoords = MotionEvent.PointerCoords()
                val pointerProperty = MotionEvent.PointerProperties()
                event.getPointerCoords(i, oldCoords)
                event.getPointerProperties(i, pointerProperty)
                pointerCoords[i] = oldCoords
                pointerCoords[i]!!.apply {
                    x = oldCoords.x / scaleX
                    y = oldCoords.y / scaleY
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

            setDisplayIdMethod?.invoke(newEvent, virtualDisplay.display.displayId)
            inputManager!!.injectInputEvent(newEvent, 0)
            newEvent.recycle()
        }
    }

    private inner class TouchListenerPreQ : View.OnTouchListener {
        @SuppressLint("ClickableViewAccessibility")
        override fun onTouch(v: View, event: MotionEvent): Boolean {
            handleTouch(event)
            when(event.action) {
                MotionEvent.ACTION_DOWN -> {
                    if (FreeformHelper.isShowingFirst(this@FreeformView)) {
                        touchId = R.id.textureView
                    } else {
                        moveToFirst()
                    }
                }
                MotionEvent.ACTION_UP -> {
                    touchId = -1
                    //handleTouch2(event)
                }
            }
            return true
        }

        /**
         * 触控处理
         */
        private fun handleTouch(event: MotionEvent) {
            val pointerCoords: Array<MotionEvent.PointerCoords?> = arrayOfNulls(event.pointerCount)
            val pointerProperties: Array<MotionEvent.PointerProperties?> = arrayOfNulls(event.pointerCount)
            for (i in 0 until event.pointerCount) {
                val oldCoords = MotionEvent.PointerCoords()
                val pointerProperty = MotionEvent.PointerProperties()
                event.getPointerCoords(i, oldCoords)
                event.getPointerProperties(i, pointerProperty)
                pointerCoords[i] = oldCoords
                pointerCoords[i]!!.apply {
                    x = oldCoords.x / scaleX
                    y = oldCoords.y / scaleY
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
            inputManager?.injectInputEvent(newEvent, virtualDisplay.display.displayId)
            newEvent.recycle()
        }
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    private inner class MTaskStackListener : TaskStackListener() {
        override fun onTaskRemovalStarted(taskInfo: ActivityManager.RunningTaskInfo) {
            if (taskInfo.taskId == taskId) {
                scope.launch(Dispatchers.Main) {
                    destroy()
                }
            }
        }

        override fun onTaskDisplayChanged(tId: Int, newDisplayId: Int) {
            if (taskId == -1 && newDisplayId == virtualDisplay.display.displayId) taskId = tId

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                if (config.useSuiRefuseToFullScreen && !isDestroy && tId == taskId && newDisplayId == Display.DEFAULT_DISPLAY) {
                    activityTaskManager?.moveRootTaskToDisplay(tId, virtualDisplay.display.displayId)
                }
            }
        }

        override fun onTaskMovedToFront(taskInfo: ActivityManager.RunningTaskInfo) {
            try {
                val userId = taskInfo::class.java.getField("userId").get(taskInfo)
                if (taskInfo.baseActivity!!.packageName == config.packageName && userId == config.userId) {
                    taskId = taskInfo.taskId
                }
            } catch (e: Exception) { }
        }

        override fun onTaskRequestedOrientationChanged(tId: Int, requestedOrientation: Int) {
            //q220902.2 某些竖屏软件也会横屏，经查，会有一个requestedOrientation为2的情况，将其转为1
            var tempRotation = requestedOrientation
            if (tempRotation != VIRTUAL_DISPLAY_ROTATION_PORTRAIT && tempRotation != VIRTUAL_DISPLAY_ROTATION_LANDSCAPE) tempRotation = VIRTUAL_DISPLAY_ROTATION_PORTRAIT
            if (taskId == tId && tempRotation != virtualDisplayRotation) {
                virtualDisplayRotation = tempRotation
                scope.launch(Dispatchers.Main) {
                    onFreeFormRotationChanged()
                }
            }
        }

        //q220903.2 Android 10系统上需要该回调监听
        override fun onActivityRequestedOrientationChanged(tId: Int, requestedOrientation: Int) {
            var tempRotation = requestedOrientation
            if (tempRotation != VIRTUAL_DISPLAY_ROTATION_PORTRAIT && tempRotation != VIRTUAL_DISPLAY_ROTATION_LANDSCAPE) tempRotation = VIRTUAL_DISPLAY_ROTATION_PORTRAIT
            if (taskId == tId && tempRotation != virtualDisplayRotation) {
                virtualDisplayRotation = tempRotation
                scope.launch(Dispatchers.Main) {
                    onFreeFormRotationChanged()
                }
            }
        }
    }

    /**
     * 支持自由缩放的缩放监听
     */
    private inner class ScaleTouchListenerAuto : View.OnTouchListener {

        private var lastX = 0f
        private var lastY = 0f

        @SuppressLint("ClickableViewAccessibility")
        override fun onTouch(v: View, event: MotionEvent): Boolean {
            when(event.action) {
                MotionEvent.ACTION_DOWN -> {
                    if (FreeformHelper.isShowingFirst(this@FreeformView)) {
                        lastX = event.rawX
                        lastY = event.rawY
                        if (touchId == -1) touchId = v.id
                    } else {
                        //不是处于最上层要移动到最上层
                        moveToFirst()
                    }

                }
                MotionEvent.ACTION_MOVE -> {
                    val dx = event.rawX - lastX
                    val dy = event.rawY - lastY
                    when(v.id) {
                        R.id.leftScale -> handleScaleLeft(dx, dy)
                        R.id.rightScale -> handleScaleRight(dx, dy)
                    }
                    lastX = event.rawX
                    lastY = event.rawY
                }
                MotionEvent.ACTION_UP -> {
                    touchId = -1

                    freeformScreenWidth = (freeformScreenHeight * (freeformWidth / freeformHeight.toFloat())).roundToInt()
                    minFreeformScreenSize = freeformScreenWidth
                    scaleY = freeformWidth / minFreeformScreenSize.toFloat()
                    resizeVirtualDisplay()
                    //修复 自由调整比例时小窗比例可能不刷新的问题 q220925.2
                    binding.textureView.surfaceTextureListener?.onSurfaceTextureSizeChanged(binding.textureView.surfaceTexture!!, freeformScreenWidth, freeformScreenHeight)
                }
            }

            return true
        }

        private fun handleScaleLeft(dx: Float, dy: Float) {
            val tempHeight = freeformHeight + dy
            val tempWidth = freeformWidth - dx
            //在尺寸内
            if (tempHeight > MIN_HEIGHT && tempHeight <= config.maxHeight && tempWidth > MIN_WIDTH && tempWidth < realScreenWidth) {
                freeformHeight = tempHeight.roundToInt()
                freeformWidth = tempWidth.roundToInt()

                notifyWindowScale()
            }
        }

        /**
         * 缩放处理
         */
        private fun handleScaleRight(dx: Float, dy: Float) {
            val tempHeight = freeformHeight + dy
            val tempWidth = freeformWidth + dx
            //在尺寸内
            if (tempHeight > MIN_HEIGHT && tempHeight <= config.maxHeight && tempWidth > MIN_WIDTH && tempWidth < realScreenWidth) {
                freeformHeight = tempHeight.roundToInt()
                freeformWidth = tempWidth.roundToInt()

                notifyWindowScale()
            }
        }
    }

    companion object {
        private const val TAG = "FreeformView"

        private const val MIN_WIDTH = 400
        private const val MIN_HEIGHT = 600

        private const val HANGUP_HEIGHT = 500

        private const val QQ = "com.tencent.mobileqq"
        private const val YOUTUBE = "com.google.android.youtube"
        private const val YOUTUBE_ACTIVITY = "com.google.android.youtube.HomeActivity"

        const val REMEMBER_X = "freeform_remember_x"
        const val REMEMBER_Y = "freeform_remember_y"
        const val REMEMBER_LAND_X = "freeform_remember_land_x"
        const val REMEMBER_LAND_Y = "freeform_remember_land_y"
        const val REMEMBER_HEIGHT = "freeform_remember_height"
        const val REMEMBER_LAND_HEIGHT = "freeform_remember_land_height"

        private const val VIRTUAL_DISPLAY_ROTATION_PORTRAIT = 1
        private const val VIRTUAL_DISPLAY_ROTATION_LANDSCAPE = 0
    }
}