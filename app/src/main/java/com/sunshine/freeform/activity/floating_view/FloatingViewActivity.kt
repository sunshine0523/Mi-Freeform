package com.sunshine.freeform.activity.floating_view

import android.annotation.SuppressLint
import android.app.AppOpsManager
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.Intent
import android.content.pm.ActivityInfo
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.provider.Settings
import android.view.*
import android.widget.Toast
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.sunshine.freeform.R
import com.sunshine.freeform.room.FreeFormAppsEntity
import com.sunshine.freeform.service.FloatingService
import com.sunshine.freeform.utils.PackageUtils
import com.sunshine.freeform.utils.TagUtils
import com.sunshine.freeform.view.floating.AllAppsAdapter
import kotlinx.android.synthetic.main.activity_floating_view.*
import java.util.*
import kotlin.collections.ArrayList

/**
 * @author sunshine
 * @data 2021.07.15
 * 悬浮活动选择小窗应用
 */
@Deprecated("Activity can pause top app(such bili) video")
class FloatingViewActivity : AppCompatActivity() {

    companion object {
        const val TAG = "FloatingViewActivity"
    }

    private lateinit var viewModel: FloatingViewViewModel
    private lateinit var recyclerAppsLayout: View

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        supportRequestWindowFeature(Window.FEATURE_NO_TITLE)
        setContentView(R.layout.activity_floating_view)

        viewModel = ViewModelProvider(this).get(FloatingViewViewModel::class.java)

        if (!Settings.canDrawOverlays(this)) {
            try {
                Toast.makeText(this, "请授予米窗的悬浮窗权限", Toast.LENGTH_SHORT).show()
                startActivity(
                    Intent(
                        Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                        Uri.parse("package:$packageName")
                    )
                )
            }catch (e: Exception) {
                Toast.makeText(this, "跳转到设置界面失败，请手动前往设置开启米窗的悬浮窗权限", Toast.LENGTH_SHORT).show()
            }

        }

        //通知悬浮球已经显示
        FloatingService.floatingViewStateListener?.onStart()

        //如果远程服务没有启动就启动远程服务
        //20210603:新增判断是否使用系统级小窗，是则不提示
//        if (!FreeFormUtils.serviceInitSuccess() && !viewModel.isUseSystemFreeForm()) {
//            FreeFormUtils.init(null)
//            Toast.makeText(this, getString(R.string.sui_not_running), Toast.LENGTH_SHORT).show()
//        }

        //点击外部关闭
        root.setOnClickListener {
            finish()
        }

        addRecyclerAppsLayout()
    }

    /**
     * 添加应用选择界面
     */
    private fun addRecyclerAppsLayout() {
        //应用选择回收布局
        recyclerAppsLayout = LayoutInflater.from(this).inflate(R.layout.view_floting_view_recycler_app, null, false)
        val recyclerView: RecyclerView = recyclerAppsLayout.findViewById(R.id.recycler_view)
        //打开全部应用的点击按钮
        //val clickView: View = recyclerAppsLayout.findViewById(R.id.view_click)

        //添加到界面
        if (viewModel.getPosition()) recycler_view_right.addView(recyclerAppsLayout) else recycler_view_left.addView(recyclerAppsLayout)

        //添加
        viewModel.getAllApps().observe(this, Observer {appsList ->
            //删除已经卸载的app
            val noInstallAppsList = ArrayList<FreeFormAppsEntity>()
            appsList?.forEach {
                if (!PackageUtils.hasInstallThisPackage(it.packageName, packageManager)) {
                    noInstallAppsList.add(it)
                }
            }
            viewModel.deleteNotInstall(noInstallAppsList)

            Collections.sort(appsList, AppsComparable())

            recyclerView.layoutManager = LinearLayoutManager(this)
//            recyclerView.adapter = FloatingViewAdapter(this, appsList, this, viewModel, object: FloatingClickListener {
//                override fun onClick() {
//                    addMoreApps()
//                }
//            })
        })

//        //点击打开
//        clickView.setOnClickListener {
//            addMoreApps()
//        }
    }

    /**
     * 加载最近任务和全部应用
     */
    @SuppressLint("ServiceCast")
    private fun addMoreApps() {
        recyclerAppsLayout.visibility = View.GONE
        materialCardView_recent_apps.visibility = View.VISIBLE
        materialCardView_all_apps.visibility = View.VISIBLE

        //检查有无最近应用权限
        val usage = try {
            (getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager).unsafeCheckOp(AppOpsManager.OPSTR_GET_USAGE_STATS, android.os.Process.myUid(), packageName) == AppOpsManager.MODE_ALLOWED
        }catch (e: Exception) {
            false
        }
        //有权限开始进行显示最近应用
        if (usage) {
            getRecentApps()

            recycler_all_apps.layoutManager = GridLayoutManager(this, 5)
            //recycler_all_apps.adapter = AllAppsAdapter(this, this, viewModel)
        } else {
            try {
                Toast.makeText(this, "请授权米窗的使用情况访问权限", Toast.LENGTH_SHORT).show()
                startActivityForResult(
                    Intent(
                        Settings.ACTION_USAGE_ACCESS_SETTINGS,
                        Uri.parse("package:$packageName")
                    ), TagUtils.GET_USAGE_STATS_PERMISSION
                )
            } catch (e: Exception) {
                Toast.makeText(this, "打开授权使用情况访问权限失败，请手动前往设置界面授予", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun getRecentApps() {
        val usageStatsManager = getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
        val time = System.currentTimeMillis()

        //获取最近应用信息
        val usageStatsList = usageStatsManager.queryUsageStats(UsageStatsManager.INTERVAL_BEST, time - 10000, time)
        usageStatsList.sortBy {
            it.lastTimeUsed
        }
        usageStatsList.reverse()

        //获取所有能启动的程序
        val intent = Intent(Intent.ACTION_MAIN, null)
        intent.addCategory(Intent.CATEGORY_LAUNCHER)
        val resolveInfo = packageManager.queryIntentActivities(intent, 0)

        //添加最近应用
        val recentApps = ArrayList<ActivityInfo>()

        for (i in 0 until usageStatsList.size) {
            for (j in 0 until resolveInfo.size) {
                if (resolveInfo[j].activityInfo.applicationInfo.packageName == usageStatsList[i].packageName) {
                    recentApps.add(resolveInfo[j].activityInfo)
                    if (recentApps.size >= 10) break
                }
            }
            if (recentApps.size >= 10) break
        }

        recycler_recent_apps.layoutManager = GridLayoutManager(this, 5)
        recycler_recent_apps.adapter = RecentAppsAdapter(recentApps, this, this, viewModel)
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()

        val view = window.decorView
        val layoutParams = view.layoutParams as WindowManager.LayoutParams
        layoutParams.width = WindowManager.LayoutParams.MATCH_PARENT
        windowManager.updateViewLayout(view, layoutParams)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == TagUtils.GET_USAGE_STATS_PERMISSION) {
            finish()
        }
    }

    override fun onPause() {
        super.onPause()
        if (viewModel.isShowFloating()) {
            //完成后显示界面
            FloatingService.floatingViewStateListener?.onStop()
        }
    }

    override fun finish() {
        super.finish()
        overridePendingTransition(R.anim.fade_in, R.anim.fade_out)
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