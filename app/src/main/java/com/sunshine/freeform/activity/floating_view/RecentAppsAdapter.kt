package com.sunshine.freeform.activity.floating_view

import android.app.ActivityOptions
import android.content.Context
import android.content.Intent
import android.content.pm.ActivityInfo
import android.content.pm.PackageManager
import android.graphics.Rect
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.recyclerview.widget.RecyclerView
import com.sunshine.freeform.R
import java.lang.reflect.Method

/**
 * @author sunshine
 * @date 2021/3/7
 */
class RecentAppsAdapter(
    private val apps: List<ActivityInfo>?,
    private val context: Context,
    private val activity: FloatingViewActivity,
    private val viewModel: FloatingViewViewModel
) : RecyclerView.Adapter<RecentAppsAdapter.ViewHolder>() {

    inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
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

    override fun getItemCount(): Int {
        return apps?.size ?: 0
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val packageName = apps!![position].packageName
        try {
            holder.icon.setImageDrawable(apps[position].loadIcon(activity.packageManager))
            holder.click.setOnClickListener {
                if (viewModel.isUseSystemFreeForm()) {
                    val intent = context.packageManager.getLaunchIntentForPackage(packageName)
                    intent?.flags = Intent.FLAG_ACTIVITY_LAUNCH_ADJACENT or Intent.FLAG_ACTIVITY_NEW_TASK
                    val activityOptions = getActivityOptions(context)
                    activityOptions?.launchBounds = Rect(200, 200, 1200, 2000)
                    val bundle = activityOptions?.toBundle()
                    activity.startActivity(intent, bundle)
                    activity.finish()
                } else {
                    activity.finish()
                    val command = "am start -n ${packageName}/${apps[position].name} --display "
                    FreeFormWindow(
                        activity,
                        command,
                        packageName
                    )
                }

            }
        } catch (e: PackageManager.NameNotFoundException) { }
    }

    private fun getActivityOptions(context: Context?): ActivityOptions? {
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

}