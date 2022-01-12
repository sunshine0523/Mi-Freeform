package com.sunshine.freeform.service

import android.annotation.SuppressLint
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.res.Configuration
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.Point
import android.os.*
import android.provider.Settings
import android.view.*
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.Toast
import androidx.datastore.preferences.preferencesDataStore
import androidx.lifecycle.ViewModelProvider
import com.sunshine.freeform.R
import com.sunshine.freeform.activity.floating_view.FloatingViewActivity
import com.sunshine.freeform.utils.FreeFormUtils
import com.sunshine.freeform.callback.FloatButtonChangeListener
import com.sunshine.freeform.callback.FloatingViewStateListener
import com.sunshine.freeform.callback.OrientationChangedListener
import java.util.concurrent.TimeUnit
import kotlin.math.max
import kotlin.math.min


/**
 * @author sunshine
 * @date 2021/2/1
 * 悬浮窗
 */

class FloatingService : Service() {
    companion object {
        var floatButtonSizeChangeListener: FloatButtonChangeListener? = null
        var floatButtonAlphaChangeListener: FloatButtonChangeListener? = null
        var floatingViewStateListener: FloatingViewStateListener? = null
        var orientationChangedListener: OrientationChangedListener? = null

        private const val TAG = "FloatingService"
        //用于子线程退出
        private var isRunning = false
    }

    // 新建悬浮窗控件
    private var floatingView: LinearLayout? = null
    private var button: ImageView? = null

    private var windowManager: WindowManager? = null
    private var layoutParams: WindowManager.LayoutParams? = null

    //判断当前对悬浮按钮的行为是touch还是click，如果是touch就拦截click监听防止滑动后显示应用栏
    private var isMove = false

    private var sp: SharedPreferences? = null
    private var edit: SharedPreferences.Editor? = null

    private var orientation = Configuration.ORIENTATION_UNDEFINED

    //振动
    private lateinit var vibrator: Vibrator
    private lateinit var vibrationEffect: VibrationEffect

    //延时隐藏按钮
    private var delayInvisibleThread: DelayInvisible? = null
    //当前按钮可见性
    private var buttonVisible = true

    //选择小窗应用界面
    //private var freeFormAppsFloatingView: FreeFormAppsFloatingView? = null

    override fun onBind(intent: Intent): IBinder {
        TODO("Return the communication channel to the service.")
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        println(newConfig.orientation)
        super.onConfigurationChanged(newConfig)
    }

    @SuppressLint("CommitPrefEdits")
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {

//        isRunning = true
//
//        sp = application.getSharedPreferences("com.sunshine.freeform_preferences", Context.MODE_PRIVATE)
//        edit = sp?.edit()

        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager

//        if (sp?.getBoolean("switch_hide_floating", false) == true) {
//            //当超过一定时间后隐藏悬浮按钮
//            delayInvisibleThread = DelayInvisible()
//            delayInvisibleThread?.start()
//        }

//        removeFloatingButtonWindows()
//        showFloatingButtonWindow()

        orientation = resources.configuration.orientation

        vibrator = getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        vibrationEffect = VibrationEffect.createOneShot(25, 255)

        //选择应用界面点击回调，点击完成就显示
        floatingViewStateListener = object : FloatingViewStateListener {
            override fun onStart() {
                removeFloatingButtonWindows()
            }

            override fun onStop() {
                removeFloatingButtonWindows()
                showFloatingButtonWindow()
            }
        }

        orientationChangedListener = object : OrientationChangedListener {
            override fun onChanged(orientation: Int) {
                //freeFormAppsFloatingView?.removeFloatingViewWindows()
                removeFloatingButtonWindows()
                showFloatingButtonWindow()
            }
        }

        //异常后重启
        return START_NOT_STICKY
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onDestroy() {
        super.onDestroy()
        isRunning = false

        floatButtonSizeChangeListener = null
        floatButtonAlphaChangeListener = null

        //freeFormAppsFloatingView?.removeFloatingViewWindows()

        removeFloatingButtonWindows()
        delayInvisibleThread?.interrupt()
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun showFloatingButtonWindow() {
        //20210711 增加对悬浮窗是否显示的判断，防止因某些原因导致的悬浮窗异常显示
        if (Settings.canDrawOverlays(this) && sp?.getBoolean("switch_floating", false) == true) {
            buttonVisible = true
            //在屏幕左边为-1，右边为1
            val position = if(sp?.getBoolean("switch_show_location", false) == true) 1 else -1

            floatingView = LinearLayout(this)
            floatingView!!.setBackgroundColor(Color.TRANSPARENT)
            floatingView!!.alpha  = ((sp?.getInt("floating_button_alpha", 200) ?: 200).toFloat() / 255)
            floatingView!!.gravity = if (position == 1) Gravity.END else Gravity.START

            button = ImageView(this)
            button!!.setImageResource(R.drawable.ic_circle)
            button!!.layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
            floatingView!!.addView(button)

            layoutParams = WindowManager.LayoutParams()

            floatingView!!.setOnClickListener(FloatingClickListener())
            val touchListener = FloatingTouchListener()
            floatingView!!.setOnLongClickListener {
                //长按时不再响应隐藏按钮
                if (!buttonVisible) {
                    button!!.layoutParams.width = ViewGroup.LayoutParams.MATCH_PARENT
                    button!!.setImageResource(R.drawable.ic_circle)
                }
                //设置为false可以方式变为隐藏，即使现在是显示状态
                buttonVisible = false

                vibrator.vibrate(vibrationEffect)
                isMove = true
                floatingView!!.setOnTouchListener(touchListener)
                true
            }

            // 设置LayoutParam
            layoutParams!!.apply {
                val buttonSize = sp?.getInt("floating_button_size", 125) ?: 125
                val point = Point()
                windowManager!!.defaultDisplay.getSize(point)
                var w = 0
                type = WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
                //不能设置imageView的layoutparams，要设置这个
                width = buttonSize
                height = buttonSize
                //设置这个在点击外部时相应外部操作
                flags = WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                format = PixelFormat.RGBA_8888
                gravity = Gravity.CENTER_VERTICAL

                //横屏
                w = if (FreeFormUtils.orientation == Configuration.ORIENTATION_LANDSCAPE) {
                    y = sp?.getInt("switch_show_location_y_landscape", 0)?:0
                    max(point.x, point.y)
                } else {
                    //竖屏
                    y = sp?.getInt("switch_show_location_y_portrait", 0)?:0
                    min(point.x, point.y)
                }
                x = w / 2 * position
            }

            // 将悬浮窗控件添加到WindowManager
            windowManager!!.addView(floatingView, layoutParams)

            //按钮大小监听，改变大小
            floatButtonSizeChangeListener = object : FloatButtonChangeListener {
                override fun onChanged(size: Int) {
                    //如果当前不是显示状态，就显示
                    if (!buttonVisible) {
                        buttonVisible = true
                        button!!.layoutParams.width = ViewGroup.LayoutParams.MATCH_PARENT
                        button!!.setImageResource(R.drawable.ic_circle)
                        delayInvisibleThread?.reset()
                    }
                    layoutParams!!.apply {
                        width = size
                        height = size
                    }
                    windowManager!!.updateViewLayout(floatingView, layoutParams)
                }

            }
            //透明度
            floatButtonAlphaChangeListener = object : FloatButtonChangeListener {
                override fun onChanged(size: Int) {
                    floatingView!!.alpha = (size.toFloat() / 255)
                    windowManager!!.updateViewLayout(floatingView, layoutParams)
                }
            }

            //如果当前是竖屏，且启用了仅在竖屏显示，那么就显示
            if (resources.configuration.orientation != Configuration.ORIENTATION_LANDSCAPE) {
                if (sp?.getBoolean("switch_preference_floating_only_enable_landscape", false) == true) {
                    floatingView!!.visibility = View.GONE
                    windowManager!!.updateViewLayout(floatingView, layoutParams)
                }
            }
        } else {
            Toast.makeText(this, getString(R.string.draw_overlay_permission_fail), Toast.LENGTH_SHORT).show()
            stopSelf()
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun removeFloatingButtonWindows() {
        if (floatingView != null) {
            windowManager?.removeView(floatingView)
            floatingView!!.setOnTouchListener(null)
            floatingView!!.setOnClickListener(null)
            floatingView = null
        }
    }

    inner class FloatingTouchListener : View.OnTouchListener {
        private var y = 0
        private val point = Point()
        @SuppressLint("ClickableViewAccessibility")
        override fun onTouch(v: View?, event: MotionEvent?): Boolean {
            when(event!!.action) {
                MotionEvent.ACTION_MOVE -> {
                    if (y == 0) y = event.rawY.toInt()
                    val nowY = event.rawY.toInt()
                    val movedY: Int = nowY - y
                    y = nowY
                    layoutParams!!.y = layoutParams!!.y + movedY
                    windowManager!!.updateViewLayout(floatingView, layoutParams)
                }
                MotionEvent.ACTION_UP -> {
                    //将y位置保存
                    windowManager!!.defaultDisplay.getSize(point)
                    //区分横竖屏存储
                    if (point.y > point.x) edit?.putInt(
                        "switch_show_location_y_portrait",
                        y - point.y / 2 - floatingView!!.height
                    ) else edit?.putInt(
                        "switch_show_location_y_landscape",
                        y - point.y / 2 - floatingView!!.height
                    )
                    edit?.apply()

                    //恢复非点击模式
                    isMove = false
                    floatingView!!.setOnTouchListener(null)

                    buttonVisible = true
                    delayInvisibleThread?.reset()
                }
            }
            return true
        }
    }

    inner class FloatingClickListener : View.OnClickListener {
        override fun onClick(v: View?) {
            if (!isMove) {
                if (!buttonVisible) {
                    button!!.layoutParams.width = ViewGroup.LayoutParams.MATCH_PARENT
                    button!!.setImageResource(R.drawable.ic_circle)
                    buttonVisible = true
                }

                when {
                    sp?.getBoolean("switch_use_system_freeform", false) == true -> {
                        val intent = Intent(this@FloatingService, FloatingViewActivity::class.java)
                        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK)
                        startActivity(intent)
                    }

                    FreeFormUtils.getControlService() != null && FreeFormUtils.serviceInitSuccess() -> {
                        //需要--activity-clear-task防止前台被打开
                        FreeFormUtils.getControlService()!!.startActivity("am start -n com.sunshine.freeform/.activity.floating_view.FloatingViewActivity --activity-clear-task")
                        delayInvisibleThread?.reset()
                    }
                    else -> {
                        FreeFormUtils.getControlService()!!.startActivity("am start -n com.sunshine.freeform/.activity.floating_view.FloatingViewActivity --activity-clear-task")
                        delayInvisibleThread?.reset()
//                        Toast.makeText(this@FloatingService, this@FloatingService.getString(R.string.sui_starting), Toast.LENGTH_SHORT).show()
//                        FreeFormUtils.init(object : SuiServerListener {
//                            override fun onStart() {
//                                //需要--activity-clear-task防止前台被打开
//                                FreeFormUtils.getControlService()?.startActivity("am start -n com.sunshine.freeform/.activity.floating_view.FloatingViewActivity --activity-clear-task")
//                                delayInvisibleThread?.reset()
//                            }
//
//                            override fun onStop() {
//                                FreeFormUtils.init(null)
//                            }
//
//                        })
                    }
                }
            }

        }
    }

    inner class DelayInvisible : Thread("DelayInvisibleThread") {

        private var startTime: Long = 0L

        fun reset() {
            startTime = System.currentTimeMillis()
        }

        override fun run() {
            startTime = System.currentTimeMillis()
            while (true) {
                if (!isRunning) break
                //如果按钮不可见无需设置
                if (buttonVisible && startTime + 3000 < System.currentTimeMillis()) {
                    Handler(Looper.getMainLooper()).post {
                        buttonVisible = false
                        (button as ImageView).setImageResource(R.drawable.corners_bg)
                        //设置边缘化后的宽度
                        button!!.layoutParams.width = 12

                        //如果有上一个的线程没有销毁及时，就会有空的问题，这里判断一下
                        if (floatingView != null) {
                            windowManager?.updateViewLayout(floatingView, layoutParams)
                        }
                    }
                }
                try {
                    TimeUnit.SECONDS.sleep(1)
                }catch (e: Exception) {}
            }
        }
    }
}