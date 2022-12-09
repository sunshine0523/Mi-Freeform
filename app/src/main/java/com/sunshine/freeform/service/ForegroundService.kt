package com.sunshine.freeform.service

import android.annotation.SuppressLint
import android.app.*
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.SharedPreferences
import android.content.res.Configuration
import android.graphics.BitmapFactory
import android.graphics.PixelFormat
import android.hardware.SensorManager
import android.hardware.display.DisplayManager
import android.net.Uri
import android.os.IBinder
import android.provider.Settings
import android.util.DisplayMetrics
import android.util.Log
import android.view.*
import android.widget.Toast
import com.sunshine.freeform.R
import com.sunshine.freeform.app.MiFreeform
import com.sunshine.freeform.broadcast.StartFreeformReceiver
import com.sunshine.freeform.ui.floating.ChooseAppFloatingView
import com.sunshine.freeform.ui.floating.FloatingActivity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
import rikka.shizuku.ShizukuBinderWrapper
import rikka.shizuku.SystemServiceHelper
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

class ForegroundService : Service(), SharedPreferences.OnSharedPreferenceChangeListener, View.OnTouchListener, GestureDetector.OnGestureListener, ChooseAppFloatingView.OnWindowRemoveCallback {

    private lateinit var sp: SharedPreferences

    private val scope = MainScope()

    //是否正在展示悬浮按钮
    private var isShowingFloating = false
    //是否正在展示选择应用
    private var isShowingChooseApp = false

    //窗口管理器
    private lateinit var windowManager: WindowManager
    private lateinit var windowLayoutParams: WindowManager.LayoutParams

    //悬浮按钮配置
    private lateinit var config: FloatingConfig

    //屏幕宽高
    private var screenWidth: Int = 0
    private var screenHeight: Int = 0

    //物理屏幕方向，1竖屏，2横屏
    private var screenRotation: Int = 0
    //通过shizuku服务获得的物理屏幕方向，0 1 2 3分别代表四个方向
    private var displayRotation = Surface.ROTATION_0

    //shizuku获取到的监听屏幕方向的服务
    private lateinit var iWindowManager: IWindowManager
    private lateinit var rotationWatcher: IRotationWatcher

    private lateinit var gestureDetector: GestureDetector

    //悬浮按钮界面
    private lateinit var floatView: View

    //应用选择悬浮界面
    private lateinit var chooseAppFloatingView: ChooseAppFloatingView

    //触摸模式，标记是点击还是长按
    private var touchMode = 0

    private lateinit var displayManager: DisplayManager

    private var startFreeformReceiver = StartFreeformReceiver()

    //获取默认屏幕
    private lateinit var defaultDisplay: Display

    //屏幕监听
    private val displayListener = object : DisplayManager.DisplayListener {
        override fun onDisplayAdded(displayId: Int) {

        }
        override fun onDisplayRemoved(displayId: Int) {

        }

        /*
            20221208 屏幕旋转可以分为两种
            1.主动旋转，即手动将设备旋转。此时defaultDisplay.rotation是可以正确的反应屏幕方向的
            2.被动旋转，比如在竖屏状态下打开一个视频，或者在竖屏状态下打开横屏游戏，此时defaultDisplay.rotation无法反应正确的屏幕方向
            原因：Display#rotation反应的不是真实的方向，比如平板，就是横着是ROTATION_0
         */
        override fun onDisplayChanged(displayId: Int) {
            if (displayId == Display.DEFAULT_DISPLAY) {

            }
        }
    }

    override fun onBind(intent: Intent): IBinder? {
        return null
    }

    override fun onCreate() {
        super.onCreate()

        sp = getSharedPreferences(MiFreeform.APP_SETTINGS_NAME, Context.MODE_PRIVATE)
        sp.registerOnSharedPreferenceChangeListener(this)
        if (sp.getInt("service_type", KeepAliveService.SERVICE_TYPE) == SERVICE_TYPE) {
            registerReceiver(startFreeformReceiver, IntentFilter("com.sunshine.freeform.start_freeform"))

            //q221208.1 修复屏幕旋转后侧边栏不贴边的问题
            iWindowManager = IWindowManager.Stub.asInterface(
                ShizukuBinderWrapper(
                    SystemServiceHelper.getSystemService("window"))
            )
            rotationWatcher = object : IRotationWatcher.Stub() {
                override fun onRotationChanged(rotation: Int) {
                    scope.launch(Dispatchers.Main) {
                        displayRotation = rotation

                        //q220902.3 如果程序崩溃的话，那么resources.configuration.orientation获取到的方向是错误的，所以不应该用该方法
                        val tempScreenRotation = if (displayRotation == Surface.ROTATION_0 || displayRotation == Surface.ROTATION_180) {
                            Configuration.ORIENTATION_PORTRAIT
                        } else {
                            Configuration.ORIENTATION_LANDSCAPE
                        }

                        if (tempScreenRotation != screenRotation) {
                            screenRotation = tempScreenRotation

                            if (screenRotation == Configuration.ORIENTATION_PORTRAIT) {
                                screenHeight = max(resources.displayMetrics.widthPixels, resources.displayMetrics.heightPixels)
                                screenWidth = min(resources.displayMetrics.widthPixels, resources.displayMetrics.heightPixels)
                            } else {
                                screenWidth = max(resources.displayMetrics.widthPixels, resources.displayMetrics.heightPixels)
                                screenHeight = min(resources.displayMetrics.widthPixels, resources.displayMetrics.heightPixels)
                            }

                            removeFloating()
                            initConfig()
                            try {
                                chooseAppFloatingView.onScreenRotationChanged(screenRotation)
                            } catch (e: Exception) {}
                        }
                    }
                }
            }
            iWindowManager.watchRotation(rotationWatcher, Display.DEFAULT_DISPLAY)

            displayManager = getSystemService(Context.DISPLAY_SERVICE) as DisplayManager
            displayManager.registerDisplayListener(displayListener, null)
            defaultDisplay = displayManager.getDisplay(Display.DEFAULT_DISPLAY)

            windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager
            windowLayoutParams = WindowManager.LayoutParams()

            gestureDetector = GestureDetector(this, this)

            screenWidth = resources.displayMetrics.widthPixels
            screenHeight = resources.displayMetrics.heightPixels
            screenRotation = resources.configuration.orientation

            initConfig()
            chooseAppFloatingView = ChooseAppFloatingView(this, config.positionX, this)
        } else {
            //如果不是前台服务模式，则关闭服务
            stopSelf()
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val notificationIntent = Intent(this, FloatingActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(this, 0, notificationIntent, PendingIntent.FLAG_IMMUTABLE)
        val builder = Notification.Builder(this.applicationContext,
            CHANNEL_ID
        )
        //创建通知渠道
        val channel = NotificationChannel(
            CHANNEL_ID,
                getString(R.string.foreground_notification_name),
                NotificationManager.IMPORTANCE_LOW
        )
        notificationManager.createNotificationChannel(channel)

        builder.setContentIntent(pendingIntent)
                .setLargeIcon(BitmapFactory.decodeResource(resources, R.mipmap.ic_launcher))
                .setContentTitle(getString(R.string.foreground_notification_title))
                .setContentText(getString(R.string.foreground_notification_text))
                .setSmallIcon(R.drawable.tile_icon)
                .setWhen(System.currentTimeMillis())
        val notification = builder.build()
        notification.flags = Notification.FLAG_ONGOING_EVENT or Notification.FLAG_NO_CLEAR

        startForeground(3, notification)

        return START_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        displayManager.unregisterDisplayListener(displayListener)
        if (isShowingFloating) removeFloating()
        sp.unregisterOnSharedPreferenceChangeListener(this)

        unregisterReceiver(startFreeformReceiver)

        iWindowManager.removeRotationWatcher(rotationWatcher)
    }

    /**
     * 加载配置
     */
    private fun initConfig() {
        config = getFloatingConfig()
        if (getBooleanSp("show_floating", false) && !isShowingFloating && !isShowingChooseApp) {
            showFloating()
        }
    }

    override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences, key: String) {
        when(key) {
            "show_floating" -> {
                if (getBooleanSp(key, false) && !isShowingFloating && !isShowingChooseApp) {
                    initConfig()
                } else {
                    removeFloating()
                }
            }
            "floating_position_x" -> {
                removeFloating()
                initConfig()
                chooseAppFloatingView.showPositionX = config.positionX
            }
            //Activity传来的打开小窗监听
            "to_show_floating" -> {
                if (!isShowingChooseApp) {
                    removeFloating()
                    //修复 可以打开多个选择应用界面的情况 q220909.1
                    isShowingChooseApp = true
                    chooseAppFloatingView.showFloatingView()
                }
            }
            "floating_alpha" -> {
                config.alpha = getIntSp("floating_alpha", 10) / 10f
                if (isShowingFloating) {
                    try {
                        windowManager.updateViewLayout(
                            floatView,
                            windowLayoutParams.apply { alpha = config.alpha }
                        )
                    }catch (e: Exception) {}
                }
            }
        }
    }

    private fun getBooleanSp(key: String, default: Boolean): Boolean {
        return sp.getBoolean(key, default)
    }

    private fun getIntSp(key: String, default: Int): Int {
        return sp.getInt(key, default)
    }

    private fun setIntSp(key: String, value: Int) {
        sp.edit().putInt(key, value).apply()
    }

    private fun showFloating() {
        floatView =
            if (config.positionX == 1) LayoutInflater.from(this).inflate(R.layout.view_floating_button_right, null, false)
            else LayoutInflater.from(this).inflate(R.layout.view_floating_button_left, null, false)
        val root = floatView.findViewById<View>(R.id.root)

        root.setOnTouchListener(this)

        val floatingButtonWidth = resources.getDimension(R.dimen.floating_button_width).toInt()
        val floatingButtonHeight = resources.getDimension(R.dimen.floating_button_height).toInt()

        if (Settings.canDrawOverlays(this)) {
            windowManager.addView(floatView, windowLayoutParams.apply {
                x = (screenWidth - floatingButtonWidth) / 2 * config.positionX
                y = if (screenRotation == 1) config.positionPortraitY else config.positionLandscapeY
                width = floatingButtonWidth
                height = floatingButtonHeight
                type = WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
                format = PixelFormat.TRANSLUCENT
                flags = flags or
                        WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                        WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL
                windowAnimations = android.R.style.Animation_Dialog
                alpha = config.alpha
            })

            isShowingFloating = true
        } else {
            try {
                Toast.makeText(this, getString(R.string.request_overlay_permission), Toast.LENGTH_LONG).show()
                val intent = Intent(
                    Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    Uri.parse("package:${packageName}")
                )
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                startActivity(
                    intent
                )
            } catch (e: Exception) {
                Toast.makeText(this, getString(R.string.request_overlay_permission_fail), Toast.LENGTH_LONG).show()
            }
        }

    }

    private fun removeFloating() {
        try {
            windowManager.removeViewImmediate(floatView)
        } catch (e: Exception) {

        }
        isShowingFloating = false
    }

    private fun getFloatingConfig(): FloatingConfig {
        return FloatingConfig(
            getIntSp("floating_position_x", -1),
            getIntSp("floating_position_portrait_y", 0),
            getIntSp("floating_position_landscape_y", 0),
            getIntSp("floating_alpha", 10) / 10f
        )
    }

    private fun handleMove(dy: Float) {
        if (screenRotation == Configuration.ORIENTATION_PORTRAIT) {
            config.positionPortraitY = max(screenHeight / -2, min(screenHeight / 2, config.positionPortraitY + dy.roundToInt()))
        }
        else {
            config.positionLandscapeY = max(screenHeight / -2, min(screenHeight / 2, config.positionLandscapeY + dy.roundToInt()))
        }

        windowManager.updateViewLayout(
            floatView,
            windowLayoutParams.apply {
                y  = max(screenHeight / -2, min(screenHeight / 2, y + dy.roundToInt()))
            }
        )
    }

    //按下时的坐标
    private var lastY = -1f
    @SuppressLint("ClickableViewAccessibility")
    override fun onTouch(v: View, event: MotionEvent): Boolean {
        gestureDetector.onTouchEvent(event)
        when(event.action) {
            MotionEvent.ACTION_DOWN -> {
                lastY = event.rawY
            }
            MotionEvent.ACTION_MOVE -> {
                if (touchMode == SCROLL) {
                    val dy = event.rawY - lastY
                    handleMove(dy)
                    lastY = event.rawY
                }
            }
            MotionEvent.ACTION_UP -> {
                if (touchMode == SCROLL) {
                    if (screenRotation == Configuration.ORIENTATION_PORTRAIT) setIntSp("floating_position_portrait_y", config.positionPortraitY)
                    else setIntSp("floating_position_landscape_y", config.positionLandscapeY)
                }
                touchMode = 0
            }
        }
        return true
    }

    override fun onDown(e: MotionEvent): Boolean {
        return false
    }

    override fun onShowPress(e: MotionEvent) {

    }

    override fun onSingleTapUp(e: MotionEvent): Boolean {
        chooseAppFloatingView.showFloatingView()
        removeFloating()
        isShowingChooseApp = true
        return false
    }

    override fun onScroll(
        e1: MotionEvent,
        e2: MotionEvent,
        distanceX: Float,
        distanceY: Float
    ): Boolean {
        touchMode = SCROLL
        return false
    }

    override fun onLongPress(e: MotionEvent) {

    }

    override fun onFling(
        e1: MotionEvent,
        e2: MotionEvent,
        velocityX: Float,
        velocityY: Float
    ): Boolean {
        return false
    }

    override fun onChooseAppWindowRemove() {
        isShowingChooseApp = false
        if (getBooleanSp("show_floating", false) && !isShowingFloating) {
            showFloating()
        }
    }

    companion object {
        private const val TAG = "ForegroundService"
        private const val SCROLL = 1
        private const val CHANNEL_ID = "CHANNEL_ID_SUNSHINE_FREEFORM_FOREGROUND"
        const val SERVICE_TYPE = 1
    }

    /**
     * 悬浮按钮配置类
     */
    data class FloatingConfig(
        //垂直按钮横坐标，-1左，1右
        var positionX: Int,
        //竖屏状态悬浮按钮纵坐标
        var positionPortraitY: Int,
        var positionLandscapeY: Int,
        //侧边栏透明度
        var alpha: Float
    )
}