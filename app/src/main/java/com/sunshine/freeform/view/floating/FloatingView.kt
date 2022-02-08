package com.sunshine.freeform.view.floating

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.graphics.PixelFormat
import android.graphics.Point
import android.net.Uri
import android.provider.Settings
import android.util.DisplayMetrics
import android.util.Log
import android.view.*
import android.widget.GridView
import android.widget.LinearLayout
import android.widget.Toast
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.sunshine.freeform.R
import com.sunshine.freeform.callback.ClickListener
import com.sunshine.freeform.callback.OrientationChangedListener
import com.sunshine.freeform.room.FreeFormAppsEntity
import com.sunshine.freeform.utils.PackageUtils
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.util.*
import kotlin.collections.ArrayList
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

/**
 * @author sunshine
 * @date 2022/1/6
 * Floating Choose Apps View
 * @see com.sunshine.freeform.activity.floating_view.FreeFormAppsFloatingView
 */
@DelicateCoroutinesApi
class FloatingView(
    private val context: Context,
    private val showLocation: Int
) {

    private val floatingViewViewModel = FloatingViewViewModel(context)
    private var allFreeFormApps: ArrayList<FreeFormAppsEntity>? = null
    private val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
    private var floatingView: View? = null
    private var floatingViewLayoutParams = WindowManager.LayoutParams()
    private var allAppsView: View? = null

    companion object {
        const val TAG = "FloatingView"

        var orientationChangedListener: OrientationChangedListener? = null
    }

    init {
        GlobalScope.launch(Dispatchers.IO) {
            allFreeFormApps = floatingViewViewModel.getAllFreeFormApps().first() as ArrayList<FreeFormAppsEntity>?
            launch(Dispatchers.Main) {
                showFloatingView()

                orientationChangedListener = object : OrientationChangedListener {
                    override fun onChanged(orientation: Int) {
                        //不在显示就不重新显示
                        try {
                            windowManager.removeViewImmediate(floatingView)
                            showFloatingView()
                        }catch (e: Exception){

                        }
                        try {
                            windowManager.removeViewImmediate(allAppsView)
                            showAllAppsView()
                        }catch (e: Exception) {

                        }
                    }
                }
            }
        }
    }

    private fun showFloatingView() {
        if (Settings.canDrawOverlays(context)) {
            floatingView = LayoutInflater.from(context).inflate(R.layout.view_floating, null, false)
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
                //TODO 这个地方获取屏幕方向需要注意，可能有问题
                width = if (context.resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE) {
                    max(point.x, point.y)
                } else {
                    //竖屏
                    min(point.x, point.y)
                }
                x = width / 2
            }

            setFloatingViewContent(floatingView)

            //click out to remove
            floatingView?.setOnClickListener {
                windowManager.removeViewImmediate(floatingView)
            }

            windowManager.addView(floatingView, floatingViewLayoutParams)
        } else {
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

    /**
     * 设置悬浮窗口的内容
     * 就是设置显示的APP
     */
    private fun setFloatingViewContent(floatingView: View?) {
        //TODO 这里有一个奇怪的事情：当下面的xml布局为google的cardview时，就会解析失败，但是用androidx的就可以
        val recyclerAppsLayout = LayoutInflater.from(context).inflate(R.layout.view_floting_view_recycler_app, null, false)
        val recyclerView: RecyclerView = recyclerAppsLayout.findViewById(R.id.recycler_view)

        //添加到界面
        if (showLocation == -1)
            floatingView?.findViewById<LinearLayout>(R.id.recycler_view_contain_left)?.addView(recyclerAppsLayout)
        else floatingView?.findViewById<LinearLayout>(R.id.recycler_view_contain_right)?.addView(recyclerAppsLayout)

        //删除已经卸载的app
        val noInstallAppsList = ArrayList<FreeFormAppsEntity>()
        allFreeFormApps?.forEach {
            if (!PackageUtils.hasInstallThisPackage(it.packageName, context.packageManager)) {
                noInstallAppsList.add(it)
            }
        }
        noInstallAppsList.forEach {
            allFreeFormApps?.remove(it)
        }

        Collections.sort(allFreeFormApps, AppsComparable())

        recyclerView.layoutManager = LinearLayoutManager(context)
        recyclerView.adapter = FloatingViewAdapter(
            context,
            allFreeFormApps,
            object: ClickListener {
                override fun onClick() {
                    windowManager.removeViewImmediate(floatingView)
                }
            },
            object : ClickListener {
                override fun onClick() {
                    windowManager.removeViewImmediate(floatingView)
                    showAllAppsView()
                }

            }
        )

        if (noInstallAppsList.isNotEmpty()) {
            //Delete NOT install apps from db
            GlobalScope.launch(Dispatchers.IO) {
                floatingViewViewModel.deleteNotInstall(noInstallAppsList)
            }
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun showAllAppsView() {
        if (Settings.canDrawOverlays(context)) {
            val point = Point()
            val dm = DisplayMetrics()
            windowManager.defaultDisplay.getSize(point)
            windowManager.defaultDisplay.getMetrics(dm)

            var realWidth = 0
            var realHeight = 0
            var overlayWidth = 0
            var overlayHeight = 0

            if (context.resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE) {
                realWidth = max(point.x, point.y)
                realHeight = min(point.x, point.y)
                overlayWidth = realWidth / 2
                overlayHeight = realHeight
            } else {
                realWidth = min(point.x, point.y)
                realHeight = max(point.x, point.y)
                overlayWidth = realWidth / 4 * 3
                overlayHeight = realHeight / 3 * 2
            }

            allAppsView = LayoutInflater.from(context).inflate(R.layout.view_all_apps, null, false)
            val layoutParams = WindowManager.LayoutParams().apply {
                width = overlayWidth
                height = overlayHeight
                type = WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
                format = PixelFormat.RGBA_8888
                gravity = Gravity.CENTER_VERTICAL
            }
            val recyclerView = allAppsView!!.findViewById<RecyclerView>(R.id.recyclerView)
            val adapter = AllAppsAdapter(context, object : ClickListener {
                override fun onClick() {
                    windowManager.removeViewImmediate(allAppsView)
                }
            })
            recyclerView.layoutManager = GridLayoutManager(context, 3)
            recyclerView.adapter = adapter

            allAppsView!!.findViewById<WaveSideBarView>(R.id.waveSideBarView).setOnTouchLetterChangeListener {
                val pos = adapter.getIndex(it)
                if(pos != -1){
                    recyclerView.scrollToPosition(pos)
                    val layoutManager = recyclerView.layoutManager as GridLayoutManager
                    layoutManager.scrollToPositionWithOffset(pos, 0)
                }
            }

            //点击外部关闭悬浮窗
            allAppsView!!.setOnTouchListener { _, _ ->
                windowManager.removeViewImmediate(allAppsView)
                true
            }

            windowManager.addView(allAppsView, layoutParams)
        } else {
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

    /**
     * 根据序号排序
     */
    inner class AppsComparable: Comparator<FreeFormAppsEntity> {
        override fun compare(o1: FreeFormAppsEntity?, o2: FreeFormAppsEntity?): Int {
            return o1!!.sortNum.compareTo(o2!!.sortNum)
        }
    }
}