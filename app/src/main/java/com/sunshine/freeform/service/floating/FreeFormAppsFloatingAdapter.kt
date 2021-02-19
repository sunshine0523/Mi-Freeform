package com.sunshine.freeform.service.floating

import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ResolveInfo
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.recyclerview.widget.RecyclerView
import com.sunshine.freeform.R
import com.sunshine.freeform.callback.FloatingClickListener


/**
 * @author sunshine
 * @date 2021/2/4
 * @param model 小窗启动模式 imagereader mediacodec
 */
class FreeFormAppsFloatingAdapter(
    private val service: FloatingService,
    private val apps: List<String>?,
    private val model: Int,
    private val callBack: FloatingClickListener
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
        val packageName = apps!![position]
        try {
            val appInfo = service.packageManager.getApplicationInfo(packageName, 0)
            var activityName = ""
            resolveInfo.forEach {
                if (it.activityInfo.applicationInfo.packageName == packageName) {
                    activityName = it.activityInfo.name
                }
            }
            holder.icon.setImageDrawable(appInfo.loadIcon(service.packageManager))
            holder.click.setOnClickListener {
                val command = "am start -n ${packageName}/${activityName} --display "
                if (model == 2) FreeFormMediaCodecView(service, command, packageName)
                else FreeFormImageReaderView(service, command, packageName)
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
        resolveInfo = service.packageManager.queryIntentActivities(intent, 0)
    }
}