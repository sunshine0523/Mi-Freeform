package com.sunshine.freeform.activity.floating_view

import android.content.Context
import android.content.SharedPreferences
import android.content.res.Configuration
import android.graphics.PixelFormat
import android.graphics.Point
import android.provider.Settings
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.WindowManager
import android.widget.Toast

import androidx.cardview.widget.CardView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

import com.sunshine.freeform.R
import com.sunshine.freeform.utils.FreeFormUtils

import kotlin.math.max
import kotlin.math.min

/**
 * @author sunshine
 * @date 2021/2/27
 */
@Deprecated(message = "This class is deprecation. Because it use SP")
class FreeFormAppsFloatingView(
    private val context: Context,
    private val button: View?,
    private val windowManager: WindowManager,
    private val sp: SharedPreferences?,
    private val floatingApps: List<String>?
) {
    //是否正在显示选择应用界面，可以做两个标记：显示应用界面时同时取消悬浮按钮的显示
    private var showFloatingView = false

    private var floatingView: View? = null

    fun showFloatingViewWindow() {
        if (Settings.canDrawOverlays(context)) {
            //切换状态
            showFloatingView = true
            button?.visibility = View.GONE

            floatingView = LayoutInflater.from(context).inflate(R.layout.view_floating, null, false)
            val floatingViewLayoutParams = WindowManager.LayoutParams()
            floatingViewLayoutParams.apply {
                width = WindowManager.LayoutParams.MATCH_PARENT
                height = WindowManager.LayoutParams.MATCH_PARENT
                type = WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
                flags = WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                format = PixelFormat.RGBA_8888
                gravity = Gravity.CENTER_VERTICAL

                val point = Point()
                windowManager.defaultDisplay.getSize(point)

                var width = 0
                //横屏
                width = if (FreeFormUtils.orientation == Configuration.ORIENTATION_LANDSCAPE) {
                    max(point.x, point.y)
                } else {
                    //竖屏
                    min(point.x, point.y)
                }
                x = width / 2
            }

            setFloatingViewContent(floatingView!!)

            windowManager.addView(floatingView, floatingViewLayoutParams)
        } else {
            Toast.makeText(context, context.getString(R.string.draw_overlay_permission_fail), Toast.LENGTH_SHORT).show()
        }
    }

    /**
     * 设置悬浮窗口的内容
     * 就是设置显示的APP
     */
    private fun setFloatingViewContent(floatingView: View) {
        //false左 true右
        val position = sp?.getBoolean("switch_show_location", false)?:false

//        //区分左右
//        val recyclerView: RecyclerView = if (position) {
//            floatingView.findViewById<CardView>(R.id.cardview_floating_right).visibility = View.VISIBLE
//            floatingView.findViewById(R.id.recycler_floating_right)
//        } else {
//            floatingView.findViewById<CardView>(R.id.cardview_floating_left).visibility = View.VISIBLE
//            floatingView.findViewById(R.id.recycler_floating_left)
//        }

        val rootView: ConstraintLayout = floatingView.findViewById(R.id.floating_root_view)

        //rootView点击返回悬浮按钮
        rootView.setOnClickListener {
            removeFloatingViewWindows()
            button?.visibility = View.VISIBLE
        }

//        recyclerView.layoutManager = LinearLayoutManager(context)

        /**
         * @see com.sunshine.freeform.view.floating.FloatingView
         * 在上述类中修改了adapter的参数类型，所以下述需要注释掉
         */
//        recyclerView.adapter = FreeFormAppsFloatingAdapter(context,
//            floatingApps,  object : com.sunshine.freeform.callback.FloatingClickListener {
//                override fun onClick() {
//                    //回调后关闭应用界面显示悬浮窗按钮
//                    removeFloatingViewWindows()
//                    button?.visibility = View.VISIBLE
//                }
//
//            })
    }

    fun removeFloatingViewWindows() {
        if (floatingView != null) {
            windowManager.removeView(floatingView)
            showFloatingView = false
            floatingView = null
        }
    }
}