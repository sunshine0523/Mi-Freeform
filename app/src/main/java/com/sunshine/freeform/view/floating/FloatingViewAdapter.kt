package com.sunshine.freeform.view.floating

import android.annotation.SuppressLint
import android.app.ActivityOptions
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ResolveInfo
import android.graphics.Rect
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.sunshine.freeform.R
import com.sunshine.freeform.activity.choose_free_form_apps.ChooseAppsActivity
import com.sunshine.freeform.activity.floating_apps_sort.FloatingAppsSortActivity
import com.sunshine.freeform.callback.ClickListener
import com.sunshine.freeform.room.FreeFormAppsEntity
import kotlinx.coroutines.DelicateCoroutinesApi
import java.lang.reflect.Method

/**
 * @author sunshine
 * @date 2021/3/7
 * 选择应用侧边栏适配器
 */
@DelicateCoroutinesApi
class FloatingViewAdapter(
    private val context: Context,
    private val apps: List<FreeFormAppsEntity>?,
    private val callback: ClickListener,
    private val openAllAppsCallback: ClickListener
) : RecyclerView.Adapter<FloatingViewAdapter.ViewHolder>() {

    private var resolveInfoList: MutableList<ResolveInfo>

    inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val icon: ImageView = itemView.findViewById(R.id.imageView_icon)
        val click: View = itemView.findViewById(R.id.view_click)
        val appName: TextView = itemView.findViewById(R.id.textView_appName)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder(
            LayoutInflater.from(parent.context).inflate(
                R.layout.item_floating,
                parent,
                false
            )
        )
    }

    override fun getItemCount(): Int {
        //2021.07.25更新，新增展示最小化的应用
        return apps?.size?.plus(2)!!
    }

    @SuppressLint("SetTextI18n")
    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        //2021.07.15优化了选择应用界面的样式，增加了应用名称显示，所有应用移动到了最上面，新增编辑应用
        when (position) {
            0 -> {
                holder.icon.setImageResource(R.drawable.ic_all)
                holder.appName.text = context.getString(R.string.all_apps)
                holder.click.setOnClickListener {
                    openAllAppsCallback.onClick()
                }
            }

            apps!!.size + 1 -> {
                holder.icon.setImageResource(R.drawable.ic_add)
                holder.appName.text = context.getString(R.string.edit_apps)
                holder.click.setOnClickListener {
                    context.startActivity(Intent(context, ChooseAppsActivity::class.java).apply { addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) })
                    callback.onClick()
                }
            }
            else -> {
                val packageName = apps[position - 1].packageName
                try {
                    val appInfo = context.packageManager.getApplicationInfo(packageName, 0)
                    var activityName = ""
                    resolveInfoList.forEach {
                        if (it.activityInfo.applicationInfo.packageName == packageName) {
                            activityName = it.activityInfo.name
                        }
                    }
                    holder.icon.setImageDrawable(appInfo.loadIcon(context.packageManager))
                    holder.appName.text = appInfo.loadLabel(context.packageManager)
                    holder.click.setOnClickListener {
                        /**
                         * 使用系统级小窗
                         */
//                        if (viewModel.isUseSystemFreeForm()) {
                        if (false) {
                            val intent = context.packageManager.getLaunchIntentForPackage(packageName)
                            intent?.flags = Intent.FLAG_ACTIVITY_LAUNCH_ADJACENT or Intent.FLAG_ACTIVITY_NEW_TASK
                            val activityOptions = getActivityOptions()
                            activityOptions?.launchBounds = Rect(200, 200, 1200, 2000)
                            val bundle = activityOptions?.toBundle()
                            context.startActivity(intent, bundle)
                            callback.onClick()
                        }
                        /**
                         * 使用米窗自带小窗
                         */
                        else {
                            val command = "am start -n ${packageName}/${activityName} --user ${apps[position - 1].userId} --display "
                            FreeFormView(
                                context,
                                command,
                                packageName
                            )

                            //MiFreeFormService.getClient()?.startWithMiFreeForm(packageName, command)
                            callback.onClick()
                        }

                    }
                    //长按进入排序界面
                    holder.click.setOnLongClickListener {
                        context.startActivity(Intent(context, FloatingAppsSortActivity::class.java).apply { addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) })
                        callback.onClick()
                        true
                    }
                } catch (e: PackageManager.NameNotFoundException) {}
            }
        }

    }

    private fun getActivityOptions(): ActivityOptions? {
        val options = ActivityOptions.makeBasic()
        val freeFormStackId = 5
        try {
            val method: Method = ActivityOptions::class.java.getMethod(
                "setLaunchWindowingMode",
                Int::class.javaPrimitiveType
            )
            method.invoke(options, freeFormStackId)
        } catch (e: Exception) {

        }
        return options
    }

    init {
        val intent = Intent(Intent.ACTION_MAIN, null)
        intent.addCategory(Intent.CATEGORY_LAUNCHER)
        resolveInfoList = context.packageManager.queryIntentActivities(intent, 0)
    }
}