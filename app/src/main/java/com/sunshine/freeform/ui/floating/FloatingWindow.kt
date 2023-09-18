package com.sunshine.freeform.ui.floating

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.content.pm.LauncherApps
import android.graphics.PixelFormat
import android.net.Uri
import android.os.IBinder
import android.os.UserHandle
import android.os.UserManager
import android.provider.Settings
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.WindowManager
import android.widget.LinearLayout
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.sunshine.freeform.R
import com.sunshine.freeform.room.FreeFormAppsEntity
import com.sunshine.freeform.service.FloatingService
import com.sunshine.freeform.utils.PackageUtils
import com.sunshine.freeform.utils.ServiceUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Collections
import java.util.Comparator

/**
 * @author KindBrave
 * @since 2023/8/23
 */
class FloatingWindow(
    private val context: Context,
    private val isLeft: Boolean
) {
    private val scope = MainScope()
    private val windowManager: WindowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
    private val launcherApps: LauncherApps = context.getSystemService(Context.LAUNCHER_APPS_SERVICE) as LauncherApps
    private val userManager: UserManager = context.getSystemService(Context.USER_SERVICE) as UserManager
    private val userHandleMap = HashMap<Int, UserHandle>()
    private val floatingViewModel = FloatingViewModel(context)
    private var allFreeFormApps: ArrayList<FreeFormAppsEntity>? = null
    private var floatingView: View? = null
    private var floatingViewLayoutParams = WindowManager.LayoutParams()

    private var serviceConnection: ServiceConnection?= null
    private var service: FloatingService? = null
    init {
        val intent = Intent(context, FloatingService::class.java)
        if (ServiceUtils.isServiceWork(context, "com.sunshine.freeform.service.FloatingService").not()) {
            context.startService(intent)
        }
        serviceConnection = object : ServiceConnection {
            override fun onServiceConnected(p0: ComponentName?, binder: IBinder) {
                service = (binder as FloatingService.MyBinder).getService()
                if (service?.getShowingSidebar() == true) {
                    context.unbindService(serviceConnection!!)
                } else {
                    userManager.userProfiles.forEach {
                        userHandleMap[com.sunshine.freeform.systemapi.UserHandle.getUserId(it)] = it
                    }
                    showFloatingView()
                }
            }

            override fun onServiceDisconnected(p0: ComponentName?) {

            }

        }
        context.bindService(intent, serviceConnection!!, Context.BIND_AUTO_CREATE)

    }

    private fun showFloatingView() {
        scope.launch(Dispatchers.IO) {
            allFreeFormApps = floatingViewModel.getAllFreeFormApps().first() as ArrayList<FreeFormAppsEntity>?
            withContext(Dispatchers.Main) {
                floatingView = LayoutInflater.from(context).inflate(R.layout.view_floating, null, false)
                floatingViewLayoutParams.apply {
                    width = WindowManager.LayoutParams.MATCH_PARENT
                    height = WindowManager.LayoutParams.MATCH_PARENT
                    type = WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
                    flags = WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                    format = PixelFormat.RGBA_8888
                    gravity = Gravity.CENTER_VERTICAL
                    windowAnimations = if (isLeft) R.style.FloatingViewLeftAnim else R.style.FloatingViewRightAnim
                }

                setFloatingViewContent(floatingView)

                //click out to remove
                floatingView?.setOnClickListener {
                    removeWindow()
                }

                runCatching {
                    windowManager.addView(floatingView, floatingViewLayoutParams)
                    service?.setShowingSidebar(true)
                }.onFailure {
                    if (!Settings.canDrawOverlays(context)) {
                        runCatching {
                            val intent = Intent(
                                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                                Uri.parse("package:${context.packageName}")
                            )
                            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                            context.startActivity(
                                intent
                            )
                        }
                    }
                }
            }
        }
    }

    private fun setFloatingViewContent(floatingView: View?) {
        //TODO 这里有一个奇怪的事情：当下面的xml布局为google的cardview时，就会解析失败，但是用androidx的就可以
        val recyclerAppsLayout = LayoutInflater.from(context).inflate(R.layout.view_choose_app, null, false)
        val recyclerView: RecyclerView = recyclerAppsLayout.findViewById(R.id.recycler_view)

        //添加到界面
        if (isLeft)
            floatingView?.findViewById<LinearLayout>(R.id.recycler_view_contain_left)?.addView(recyclerAppsLayout)
        else floatingView?.findViewById<LinearLayout>(R.id.recycler_view_contain_right)?.addView(recyclerAppsLayout)

        //删除已经卸载的app
        val noInstallAppsList = ArrayList<FreeFormAppsEntity>()
        allFreeFormApps?.forEach {
            if (it.userId == 0) {
                if (!PackageUtils.hasInstallThisPackage(it.packageName, context.packageManager)) {
                    noInstallAppsList.add(it)
                }
            } else {
                val userHandle = if (userHandleMap.containsKey(it.userId)) userHandleMap[it.userId]!! else userHandleMap[0]!!
                if (!PackageUtils.hasInstallThisPackageWithUserId(it.packageName, launcherApps, userHandle)) {
                    noInstallAppsList.add(it)
                }
            }
        }
        noInstallAppsList.forEach {
            allFreeFormApps?.remove(it)
        }

        Collections.sort(allFreeFormApps, AppsComparable())

        recyclerView.layoutManager = LinearLayoutManager(context)
        recyclerView.adapter = ChooseAppFloatingAdapter(
            context,
            floatingViewModel,
            allFreeFormApps,
            object: ClickListener() {
                override fun onClick() {
                    removeWindow()
                }
            }
        )

        if (noInstallAppsList.isNotEmpty()) {
            //Delete NOT install apps from db
            scope.launch(Dispatchers.IO) {
                floatingViewModel.deleteNotInstall(noInstallAppsList)
            }
        }
    }

    private fun removeWindow() {
        runCatching {
            windowManager.removeViewImmediate(floatingView)
            service?.setShowingSidebar(false)
            if (serviceConnection != null) {
                context.unbindService(serviceConnection!!)
            }
        }
    }

    abstract class ClickListener {
        open fun onClick() {}
    }

    inner class AppsComparable: Comparator<FreeFormAppsEntity> {
        override fun compare(o1: FreeFormAppsEntity?, o2: FreeFormAppsEntity?): Int {
            return o1!!.sortNum.compareTo(o2!!.sortNum)
        }
    }
}