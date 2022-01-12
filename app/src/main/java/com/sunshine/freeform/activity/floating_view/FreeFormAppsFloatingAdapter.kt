package com.sunshine.freeform.activity.floating_view

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ResolveInfo
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.recyclerview.widget.RecyclerView
import com.sunshine.freeform.R
import com.sunshine.freeform.callback.ClickListener
import com.sunshine.freeform.room.FreeFormAppsEntity


/**
 * @author sunshine
 * @date 2021/2/4
 * @param model 小窗启动模式 imagereader mediacodec
 */
class FreeFormAppsFloatingAdapter(
    private val context: Context,
    private val apps: List<FreeFormAppsEntity>?,
    private val callBack: ClickListener
) : RecyclerView.Adapter<FreeFormAppsFloatingAdapter.ViewHolder>() {

    private var resolveInfo: MutableList<ResolveInfo>

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val icon: ImageView = itemView.findViewById(R.id.imageView_icon)
        val click: View = itemView.findViewById(R.id.view_click)
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

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val packageName = apps!![position].packageName
        try {
            val appInfo = context.packageManager.getApplicationInfo(packageName, 0)
            var activityName = ""
            resolveInfo.forEach {
                if (it.activityInfo.applicationInfo.packageName == packageName) {
                    activityName = it.activityInfo.name
                }
            }
            holder.icon.setImageDrawable(appInfo.loadIcon(context.packageManager))
            holder.click.setOnClickListener {
                val command = "am start -n ${packageName}/${activityName} --display "
                FreeFormWindow(context, command, packageName)
                callBack.onClick()
            }
        } catch (e: PackageManager.NameNotFoundException) {}
    }

    override fun getItemCount(): Int {
        return apps?.size ?: 0
    }

    init {
        val intent = Intent(Intent.ACTION_MAIN, null)
        intent.addCategory(Intent.CATEGORY_LAUNCHER)
        resolveInfo = context.packageManager.queryIntentActivities(intent, 0)
    }
}