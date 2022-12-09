package com.sunshine.freeform.ui.floating

import android.app.ActivityOptions
import android.content.Context
import android.content.pm.LauncherActivityInfo
import android.content.pm.PackageManager
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import com.github.promeg.pinyinhelper.Pinyin
import com.sunshine.freeform.R
import com.sunshine.freeform.systemapi.UserHandle
import com.sunshine.freeform.ui.freeform.FreeformConfig
import com.sunshine.freeform.ui.freeform.FreeformView
import java.lang.reflect.Method
import kotlin.collections.ArrayList

/**
 * @author sunshine
 * @date 2021/3/7
 */
class AllAppsAdapter(
    private val context: Context,
    private val allAppsList: ArrayList<LauncherActivityInfo>,
    private val callback: ChooseAppFloatingView.ClickListener
) : RecyclerView.Adapter<AllAppsAdapter.ViewHolder>() {

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
        return allAppsList.size
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val applicationInfo = allAppsList[position].applicationInfo
        val packageName = applicationInfo.packageName
        val activityName = allAppsList[position].name
        try {
            Glide.with(context)
                .load(applicationInfo.loadIcon(context.packageManager))
                .transition(DrawableTransitionOptions.withCrossFade())
                .into(holder.icon)
            holder.appName.text = allAppsList[position].label
            holder.click.setOnClickListener {
                val userId = UserHandle.getUserId(allAppsList[position].user, allAppsList[position].applicationInfo.uid)

                FreeformView(
                    FreeformConfig(
                        packageName = packageName,
                        activityName = activityName,
                        userId = userId
                    ),
                    context
                )
                callback.onClick()
            }
        } catch (e: PackageManager.NameNotFoundException) {}
    }

    fun getIndex(str: String?): Int {
        allAppsList.forEach {
            if(Pinyin.toPinyin(it.label[0]).substring(0..0) == str){
                return allAppsList.indexOf(it)
            }
        }
        return -1
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