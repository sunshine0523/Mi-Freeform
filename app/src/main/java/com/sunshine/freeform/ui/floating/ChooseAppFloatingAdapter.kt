package com.sunshine.freeform.ui.floating

import android.annotation.SuppressLint
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.LauncherApps
import android.content.pm.PackageManager
import android.content.pm.ResolveInfo
import android.os.UserHandle
import android.os.UserManager
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import com.sunshine.freeform.MiFreeformServiceManager
import com.sunshine.freeform.R
import com.sunshine.freeform.room.FreeFormAppsEntity
import kotlin.math.roundToInt

/**
 * @author sunshine
 * @date 2021/3/7
 * 选择应用侧边栏适配器
 */
class ChooseAppFloatingAdapter(
    private val context: Context,
    private val viewModel: FloatingViewModel,
    private val apps: List<FreeFormAppsEntity>?,
    private val callback: FloatingWindow.ClickListener
) : RecyclerView.Adapter<ChooseAppFloatingAdapter.ViewHolder>() {

    private var resolveInfoList: MutableList<ResolveInfo>
    private lateinit var launcherApps: LauncherApps
    private val userHandleMap = HashMap<Int, UserHandle>()

    inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val icon: ImageView = itemView.findViewById(R.id.imageView_icon)
        val click: View = itemView.findViewById(R.id.view_click)
        val appName: TextView = itemView.findViewById(R.id.textView_appName)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val userManager = context.getSystemService(Context.USER_SERVICE) as UserManager
        launcherApps = context.getSystemService(Context.LAUNCHER_APPS_SERVICE) as LauncherApps

        userManager.userProfiles.forEach {
            userHandleMap[com.sunshine.freeform.systemapi.UserHandle.getUserId(it)] = it
        }

        return ViewHolder(
            LayoutInflater.from(parent.context).inflate(
                R.layout.item_floating,
                parent,
                false
            )
        )
    }

    override fun getItemCount(): Int {
        return apps?.size?.plus(2)!!
    }

    @SuppressLint("SetTextI18n")
    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        when (position) {
            0 -> {
                holder.icon.setImageResource(R.drawable.ic_all)
                holder.appName.text = context.getString(R.string.all_apps)
                holder.click.setOnClickListener {
                    MiFreeformServiceManager.createWindow(
                        "com.sunshine.freeform",
                        "com.sunshine.freeform.ui.app_list.AppListActivity",
                        0,
                        viewModel.getIntSp("freeform_width", (viewModel.screenWidth * 0.8).roundToInt()),
                        viewModel.getIntSp("freeform_height", (viewModel.screenHeight * 0.5).roundToInt()),
                        viewModel.getIntSp("freeform_dpi", viewModel.screenDensityDpi),
                    )
                    callback.onClick()
                }
            }

            apps!!.size + 1 -> {
                holder.icon.setImageResource(R.drawable.ic_add)
                holder.appName.text = context.getString(R.string.edit_apps)
                holder.click.setOnClickListener {
                    MiFreeformServiceManager.createWindow(
                        "com.sunshine.freeform",
                        "com.sunshine.freeform.ui.app_list.FreeformAppActivity",
                        0,
                        viewModel.getIntSp("freeform_width", (viewModel.screenWidth * 0.8).roundToInt()),
                        viewModel.getIntSp("freeform_height", (viewModel.screenHeight * 0.5).roundToInt()),
                        viewModel.getIntSp("freeform_dpi", viewModel.screenDensityDpi),
                    )
                    callback.onClick()
                }
            }
            else -> {
                try {
                    val userHandle = if (userHandleMap.containsKey(apps[position - 1].userId)) userHandleMap[apps[position - 1].userId]!! else userHandleMap[0]!!
                    val appInfo = launcherApps.getApplicationInfo(apps[position - 1].packageName, 0, userHandle)
                    Glide.with(context)
                        .load(appInfo.loadIcon(context.packageManager))
                        .transition(DrawableTransitionOptions.withCrossFade())
                        .diskCacheStrategy(DiskCacheStrategy.AUTOMATIC)
                        .into(holder.icon)
                    holder.appName.text = getLabel(appInfo, apps[position - 1].userId)
                    holder.click.setOnClickListener {
                        MiFreeformServiceManager.createWindow(
                            apps[position - 1].packageName,
                            apps[position - 1].activityName,
                            apps[position - 1].userId,
                            viewModel.getIntSp("freeform_width", (viewModel.screenWidth * 0.8).roundToInt()),
                            viewModel.getIntSp("freeform_height", (viewModel.screenHeight * 0.5).roundToInt()),
                            viewModel.getIntSp("freeform_dpi", viewModel.screenDensityDpi)
                        )
                        callback.onClick()
                    }
                    //长按进入排序界面
//                    holder.click.setOnLongClickListener {
//                        context.startActivity(Intent(context, FloatingAppsSortActivity::class.java).apply { addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) })
//                        callback.onClick()
//                        true
//                    }
                } catch (e: PackageManager.NameNotFoundException) {}
            }
        }

    }

    private fun getLabel(applicationInfo: ApplicationInfo, userId: Int): CharSequence {
        return if (userId == 0) {
            context.packageManager.getApplicationLabel(applicationInfo)
        } else {
            "${context.packageManager.getApplicationLabel(applicationInfo)}-${userId}"
        }
    }

    init {
        val intent = Intent(Intent.ACTION_MAIN, null)
        intent.addCategory(Intent.CATEGORY_LAUNCHER)
        resolveInfoList = context.packageManager.queryIntentActivities(intent, 0)
    }
}