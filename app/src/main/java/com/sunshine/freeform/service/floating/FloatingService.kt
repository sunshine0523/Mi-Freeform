package com.sunshine.freeform.service.floating

import android.annotation.SuppressLint
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.res.Configuration
import android.graphics.PixelFormat
import android.graphics.Point
import android.os.Binder
import android.os.IBinder
import android.provider.Settings
import android.view.*
import android.widget.Button
import android.widget.Toast
import androidx.cardview.widget.CardView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.sunshine.freeform.R
import com.sunshine.freeform.callback.OrientationChangeListener
import com.sunshine.freeform.callback.ServiceStateListener
import com.sunshine.freeform.utils.ScreenOrientationListener
import com.sunshine.freeform.utils.ServiceUtils
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
        //悬浮窗应用，因为service无法获取room，所以采用这个方式
        var floatingApps: List<String>? = null
        var listener: ServiceStateListener? = null

        //屏幕方向，1 竖屏 2横屏 0未定义
        var orientation = Configuration.ORIENTATION_UNDEFINED

        /**
         * 服务中判断屏幕方向
         * 这样在后台就可判断屏幕方向了
         */
        var orientationChangeListener: OrientationChangeListener? = null
    }

    // 新建悬浮窗控件
    private var button: Button? = null
    private var floatingView: View? = null
    private var windowManager: WindowManager? = null
    private var layoutParams: WindowManager.LayoutParams? = null
    //是否正在显示选择应用界面，可以做两个标记：显示应用界面时同时取消悬浮按钮的显示
    private var showFloatingView = false
    //判断当前对悬浮按钮的行为是touch还是click，如果是touch就拦截click监听防止滑动后显示应用栏
    private var isMove = false

    private lateinit var sp: SharedPreferences
    private lateinit var edit: SharedPreferences.Editor

    override fun onBind(intent: Intent): IBinder {
        return MyBind()
    }

    inner class MyBind : Binder() {
        val getService: FloatingService
            get() = this@FloatingService
    }

    @SuppressLint("CommitPrefEdits")
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        sp = application.getSharedPreferences("com.sunshine.freeform_preferences", Context.MODE_PRIVATE)
        edit = sp.edit()

        //初始化小窗连接
        FreeFormConfig.init(listener)
        removeFloatingButtonWindows()
        showFloatingButtonWindow()

        toListenOrientation()

        ScreenOrientationListener(this).setOnOrientationChangedListener(object : ScreenOrientationListener.OnOrientationChangedListener {
            override fun onOrientationChanged(orientation: Int) {
                println("changed")
            }

        })

        return super.onStartCommand(intent, flags, startId)
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onDestroy() {
        super.onDestroy()
        //关闭屏幕方向监听
        orientationChangeListener = null

        removeFloatingButtonWindows()
        removeFloatingViewWindows()

        //关闭与服务的连接
        FreeFormConfig.onDelete(true)
        println("service onDestroy")
    }

    /**
     * 监听屏幕方向
     */
    private fun toListenOrientation() {
        orientationChangeListener = object : OrientationChangeListener {
            override fun onChanged() {
                removeFloatingViewWindows()
                removeFloatingButtonWindows()
                showFloatingButtonWindow()
                FreeFormConfig.orientationChanged()
            }
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun showFloatingButtonWindow() {
        if (Settings.canDrawOverlays(this)) {
            button = Button(applicationContext)

            windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
            layoutParams = WindowManager.LayoutParams()

            button!!.setOnTouchListener(FloatingTouchListener())
            button!!.setOnClickListener(FloatingClickListener())

            // 设置LayoutParam
            layoutParams!!.apply {
                type = WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
                width = WindowManager.LayoutParams.WRAP_CONTENT
                height = WindowManager.LayoutParams.WRAP_CONTENT
                //设置这个在点击外部时相应外部操作
                flags = WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                format = PixelFormat.RGBA_8888
                gravity = Gravity.CENTER_VERTICAL

                //在屏幕左边为-1，右边为1
                val position = if(sp.getBoolean("switch_show_location", false)) 1 else -1
                val point = Point()
                windowManager!!.defaultDisplay.getSize(point)

                var width = 0
                //横屏
                width = if (orientation == Configuration.ORIENTATION_LANDSCAPE) {
                    y = sp.getInt("switch_show_location_y_landscape", 0)
                    max(point.x, point.y)
                } else {
                    //竖屏
                    y = sp.getInt("switch_show_location_y_portrait", 0)
                    min(point.x, point.y)
                }
                x = width / 2 * position
            }

            // 将悬浮窗控件添加到WindowManager
            windowManager!!.addView(button, layoutParams)
        } else {
            Toast.makeText(this, getString(R.string.draw_overlay_permission_fail), Toast.LENGTH_SHORT).show()
            stopSelf()
        }
    }

    private fun showFloatingViewWindow() {
        if (Settings.canDrawOverlays(this)) {
            //切换状态
            showFloatingView = true
            button!!.visibility = View.GONE

            floatingView = LayoutInflater.from(this@FloatingService).inflate(R.layout.view_floating, null, false)
            val floatingViewLayoutParams = WindowManager.LayoutParams()
            floatingViewLayoutParams.apply {
                width = WindowManager.LayoutParams.MATCH_PARENT
                height = WindowManager.LayoutParams.MATCH_PARENT
                type = WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
                flags = WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                format = PixelFormat.RGBA_8888
                gravity = Gravity.CENTER_VERTICAL

                val point = Point()
                windowManager!!.defaultDisplay.getSize(point)

                var width = 0
                //横屏
                width = if (orientation == Configuration.ORIENTATION_LANDSCAPE) {
                    max(point.x, point.y)
                } else {
                    //竖屏
                    min(point.x, point.y)
                }
                x = width / 2
            }

            setFloatingViewContent(floatingView!!)

            windowManager!!.addView(floatingView, floatingViewLayoutParams)
        } else {
            Toast.makeText(this, getString(R.string.draw_overlay_permission_fail), Toast.LENGTH_SHORT).show()
            stopSelf()
        }
    }

    /**
     * 设置悬浮窗口的内容
     * 就是设置显示的APP
     */
    private fun setFloatingViewContent(floatingView: View) {
        //false左 true右
        val position = sp.getBoolean("switch_show_location", false)

        //区分左右
        val recyclerView: RecyclerView = if (position) {
            floatingView.findViewById<CardView>(R.id.cardview_floating_right).visibility = View.VISIBLE
            floatingView.findViewById(R.id.recycler_floating_right)
        } else {
            floatingView.findViewById<CardView>(R.id.cardview_floating_left).visibility = View.VISIBLE
            floatingView.findViewById(R.id.recycler_floating_left)
        }

        val rootView: ConstraintLayout = floatingView.findViewById(R.id.floating_root_view)

        //rootView点击返回悬浮按钮
        rootView.setOnClickListener {
            removeFloatingViewWindows()
            button!!.visibility = View.VISIBLE
        }

        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = FreeFormAppsFloatingAdapter(this, floatingApps, sp.getInt("freeform_model", 1), object : com.sunshine.freeform.callback.FloatingClickListener {
            override fun onClick() {
                //回调后关闭应用界面显示悬浮窗按钮
                removeFloatingViewWindows()
                button!!.visibility = View.VISIBLE
            }

        })
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

    private fun removeFloatingViewWindows() {
        if (floatingView != null) {
            windowManager?.removeView(floatingView)
            showFloatingView = false
            floatingView = null
        }
    }

    inner class FloatingTouchListener : View.OnTouchListener {
        private var y = 0
        private val point = Point()
        private var isMoveNow = false
        @SuppressLint("ClickableViewAccessibility")
        override fun onTouch(v: View?, event: MotionEvent?): Boolean {
            //对于这次有没有一个滑动操作，如果有的话，那么在up时不将全局变量进行变成false操作
            when(event!!.action) {
                MotionEvent.ACTION_DOWN -> {
                    //每一次按下就是新一轮判断，所以应该重置isMoveNow状态
                    isMoveNow = false

                    y = event.rawY.toInt()
                }
                MotionEvent.ACTION_MOVE -> {
                    //滑动模式不相应click事件
                    isMove = true
                    isMoveNow = true

                    val nowY = event.rawY.toInt()
                    val movedY: Int = nowY - y
                    y = nowY
                    layoutParams!!.y = layoutParams!!.y + movedY
                    windowManager!!.updateViewLayout(button, layoutParams)
                }
                MotionEvent.ACTION_UP -> {
                    //这次不是滑动就将isMove设置为false
                    if (!isMoveNow) isMove = false

                    //将y位置保存
                    windowManager!!.defaultDisplay.getSize(point)
                    //区分横竖屏存储
                    if (point.y > point.x) edit.putInt(
                            "switch_show_location_y_portrait",
                            y - point.y / 2 - button!!.height
                    ) else edit.putInt(
                            "switch_show_location_y_landscape",
                            y - point.y / 2 - button!!.height
                    )
                    edit.apply()
                }
            }
            return false
        }
    }

    inner class FloatingClickListener : View.OnClickListener {
        override fun onClick(v: View?) {
            if (!isMove) {
                removeFloatingViewWindows()
                showFloatingViewWindow()
            }
        }
    }
}