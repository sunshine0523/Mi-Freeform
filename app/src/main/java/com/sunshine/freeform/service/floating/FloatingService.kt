package com.sunshine.freeform.service.floating

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
import android.util.Log
import android.util.TimeUtils
import android.view.*
import android.widget.ImageView
import android.widget.Toast
import androidx.cardview.widget.CardView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.sunshine.freeform.R
import com.sunshine.freeform.callback.FloatButtonChangeListener
import com.sunshine.freeform.callback.ServiceStateListener
import com.sunshine.freeform.room.DatabaseRepository
import com.sunshine.freeform.service.core.CoreService
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
        var listener: ServiceStateListener? = null
        var floatButtonSizeChangeListener: FloatButtonChangeListener? = null
        var floatButtonAlphaChangeListener: FloatButtonChangeListener? = null

        private const val TAG = "FloatingService"
    }

    // 新建悬浮窗控件
    private var button: View? = null

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

    //选择小窗应用界面
    private var freeFormAppsFloatingView: FreeFormAppsFloatingView? = null

    //延时隐藏按钮
    private var delayInvisibleThread: DelayInvisible? = null
    //当前按钮可见性
    private var buttonVisible = true

    override fun onBind(intent: Intent): IBinder {
        TODO("Return the communication channel to the service.")
    }

//    inner class MyBind : Binder() {
//        val getService: FloatingService
//            get() = this@FloatingService
//    }

    @SuppressLint("CommitPrefEdits")
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {

        sp = application.getSharedPreferences("com.sunshine.freeform_preferences", Context.MODE_PRIVATE)
        edit = sp?.edit()

        //当超过一定时间后隐藏悬浮按钮
        //delayInvisibleThread = DelayInvisible()
        //delayInvisibleThread?.start()

        //初始化小窗连接
        FreeFormConfig.init(listener, sp?.getInt("freeform_control_model", 1) ?: 1)
        removeFloatingButtonWindows()
        showFloatingButtonWindow()

        orientation = resources.configuration.orientation

        vibrator = getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        vibrationEffect = VibrationEffect.createOneShot(25, 255)

        return super.onStartCommand(intent, flags, startId)
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)

        orientation = resources.configuration.orientation

        freeFormAppsFloatingView?.removeFloatingViewWindows()
        removeFloatingButtonWindows()
        showFloatingButtonWindow()

        FreeFormConfig.orientation = orientation
        FreeFormConfig.orientationChanged()
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onDestroy() {
        super.onDestroy()

        floatButtonSizeChangeListener = null
        floatButtonAlphaChangeListener = null

        removeFloatingButtonWindows()
        freeFormAppsFloatingView?.removeFloatingViewWindows()

        //只有都不需要服务才关闭
        if (sp?.getBoolean("switch_notify", false) != true) {
            //关闭与服务的连接
            FreeFormConfig.onDelete(true)
        }

        println("service onDestroy")
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun showFloatingButtonWindow() {
        if (Settings.canDrawOverlays(this)) {
            button = ImageView(this)
            button!!.alpha  = ((sp?.getInt("floating_button_alpha", 200) ?: 200).toFloat() / 255)
            (button!! as ImageView).setImageResource(R.drawable.ic_circle)
            val buttonSize = sp?.getInt("floating_button_size", 125) ?: 125

            windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
            layoutParams = WindowManager.LayoutParams()

            button!!.setOnClickListener(FloatingClickListener())

            val touchListener = FloatingTouchListener()
            button!!.setOnLongClickListener {
                vibrator.vibrate(vibrationEffect)
                isMove = true
                button!!.setOnTouchListener(touchListener)
                true
            }

            // 设置LayoutParam
            layoutParams!!.apply {
                type = WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
                //不能设置imageView的layoutparams，要设置这个
                width = buttonSize
                height = buttonSize
                //设置这个在点击外部时相应外部操作
                flags = WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                format = PixelFormat.RGBA_8888
                gravity = Gravity.CENTER_VERTICAL

                //在屏幕左边为-1，右边为1
                val position = if(sp?.getBoolean("switch_show_location", false) == true) 1 else -1
                val point = Point()
                windowManager!!.defaultDisplay.getSize(point)

                var w = 0
                //横屏
                w = if (orientation == Configuration.ORIENTATION_LANDSCAPE) {
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
            windowManager!!.addView(button, layoutParams)

            //按钮大小监听，改变大小
            floatButtonSizeChangeListener = object : FloatButtonChangeListener {
                override fun onChanged(size: Int) {
                    //如果当前不是显示状态，就显示
                    if (!buttonVisible) {
                        buttonVisible = true
                        (button as ImageView).setImageResource(R.drawable.ic_circle)
                        delayInvisibleThread?.reset()
                    }
                    layoutParams!!.apply {
                        width = size
                        height = size
                    }
                    windowManager!!.updateViewLayout(button, layoutParams)
                }

            }
            //透明度
            floatButtonAlphaChangeListener = object : FloatButtonChangeListener {
                override fun onChanged(size: Int) {
                    button!!.alpha = (size.toFloat() / 255).toFloat()
                    windowManager!!.updateViewLayout(button, layoutParams)
                }

            }
        } else {
            Toast.makeText(this, getString(R.string.draw_overlay_permission_fail), Toast.LENGTH_SHORT).show()
            stopSelf()
        }
    }



    @SuppressLint("ClickableViewAccessibility")
    private fun removeFloatingButtonWindows() {
        if (button != null) {
            windowManager?.removeView(button)
            button!!.setOnTouchListener(null)
            button!!.setOnClickListener(null)
            button = null
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
                    windowManager!!.updateViewLayout(button, layoutParams)
                }
                MotionEvent.ACTION_UP -> {
                    //将y位置保存
                    windowManager!!.defaultDisplay.getSize(point)
                    //区分横竖屏存储
                    if (point.y > point.x) edit?.putInt(
                        "switch_show_location_y_portrait",
                        y - point.y / 2 - button!!.height
                    ) else edit?.putInt(
                        "switch_show_location_y_landscape",
                        y - point.y / 2 - button!!.height
                    )
                    edit?.apply()

                    //恢复非点击模式
                    isMove = false
                    button!!.setOnTouchListener(null)
                }
            }
            return true
        }
    }

    inner class FloatingClickListener : View.OnClickListener {
        override fun onClick(v: View?) {
            if (!isMove) {
                if (CoreService.isRunning) {
                    freeFormAppsFloatingView = FreeFormAppsFloatingView(this@FloatingService, button, windowManager!!, sp, CoreService.floatingApps)
                    freeFormAppsFloatingView!!.removeFloatingViewWindows()
                    freeFormAppsFloatingView!!.showFloatingViewWindow()
                } else {
                    Toast.makeText(this@FloatingService, getString(R.string.core_not_running), Toast.LENGTH_SHORT).show()
                }
            }

        }
    }

    inner class DelayInvisible : Thread() {

        private var startTime: Long = 0L

        public fun reset() {
            startTime = System.currentTimeMillis()
        }

        override fun run() {
            startTime = System.currentTimeMillis()
            while (true) {
                //如果按钮不可见无需设置
                if (buttonVisible && startTime + 1000 < System.currentTimeMillis()) {
                    Handler(Looper.getMainLooper()).post {
                        buttonVisible = false
                        (button as ImageView).setImageResource(R.drawable.corners_bg)
                        layoutParams?.apply {
                            width = 50
                            //在屏幕左边为-1，右边为1
                            val position = if(sp?.getBoolean("switch_show_location", false) == true) 1 else -1
                            val point = Point()
                            windowManager!!.defaultDisplay.getSize(point)

                            var w = 0
                            //横屏
                            w = if (orientation == Configuration.ORIENTATION_LANDSCAPE) {
                                max(point.x, point.y)
                            } else {
                                //竖屏
                                min(point.x, point.y)
                            }
                            x = (w / 2 - 20) * position
                        }
                        windowManager?.updateViewLayout(button, layoutParams)
                    }
                }
                try {
                    TimeUnit.SECONDS.sleep(1)
                }catch (e: Exception) {}
            }
        }
    }
}