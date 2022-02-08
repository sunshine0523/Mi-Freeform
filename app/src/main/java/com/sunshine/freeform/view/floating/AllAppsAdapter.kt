package com.sunshine.freeform.view.floating

import android.app.ActivityOptions
import android.content.Context
import android.content.Intent
import android.content.pm.LauncherActivityInfo
import android.content.pm.LauncherApps
import android.content.pm.PackageManager
import android.content.pm.ResolveInfo
import android.os.UserManager
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.github.promeg.pinyinhelper.Pinyin
import com.sunshine.freeform.R
import com.sunshine.freeform.callback.ClickListener
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import me.zhanghai.android.appiconloader.AppIconLoader
import java.lang.reflect.Method
import java.util.*
import kotlin.Comparator
import kotlin.collections.ArrayList
import kotlin.collections.HashMap

/**
 * @author sunshine
 * @date 2021/3/7
 */
@DelicateCoroutinesApi
class AllAppsAdapter(
    private val context: Context,
    private val callback: ClickListener
) : RecyclerView.Adapter<AllAppsAdapter.ViewHolder>() {

    private var userManager: UserManager
    private var allAppsList = ArrayList<LauncherActivityInfo>()
    private var resolveInfoList: MutableList<ResolveInfo>
    //使用拼音排序
    private var appsPinyinMap = HashMap<String, String>()

    private var appIconLoader: AppIconLoader

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
        var activityName = ""
        resolveInfoList.forEach {
            if (applicationInfo.packageName == packageName) activityName = it.activityInfo.name
        }
        try {
            holder.icon.setImageBitmap(appIconLoader.loadIcon(applicationInfo))
            holder.appName.text = allAppsList[position].label
            holder.click.setOnClickListener {
                val command = "am start -n ${packageName}/${activityName} --display "
                FreeFormView(
                    context,
                    command,
                    packageName
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

    init {
        val intent = Intent(Intent.ACTION_MAIN, null)
        intent.addCategory(Intent.CATEGORY_LAUNCHER)

        resolveInfoList = context.packageManager.queryIntentActivities(intent, 0)

        userManager = context.getSystemService(Context.USER_SERVICE) as UserManager
        val launcherApps = context.getSystemService(Context.LAUNCHER_APPS_SERVICE) as LauncherApps

        GlobalScope.launch(Dispatchers.IO) {
            userManager.userProfiles.forEach {
                allAppsList.addAll(launcherApps.getActivityList(null, it))
            }

            allAppsList.forEach {
                appsPinyinMap[it.label.toString()] = Pinyin.toPinyin(it.label[0])
            }
            Collections.sort(allAppsList, PinyinComparable())
        }

        appIconLoader = AppIconLoader(context.resources.getDimensionPixelSize(android.R.dimen.app_icon_size), false, context)
    }

    inner class PinyinComparable : Comparator<LauncherActivityInfo>{
        override fun compare(o1: LauncherActivityInfo?, o2: LauncherActivityInfo?): Int {
            return appsPinyinMap[o1!!.label]!!.compareTo(appsPinyinMap[o2!!.label]!!)
        }
    }
}