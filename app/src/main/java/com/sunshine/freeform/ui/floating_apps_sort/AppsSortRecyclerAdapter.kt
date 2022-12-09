package com.sunshine.freeform.ui.floating_apps_sort

import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.LauncherApps
import android.content.pm.PackageManager
import android.os.Build
import android.os.UserHandle
import android.os.UserManager
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.annotation.RequiresApi
import androidx.appcompat.widget.AppCompatTextView
import androidx.appcompat.widget.SwitchCompat
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import com.sunshine.freeform.R
import com.sunshine.freeform.room.FreeFormAppsEntity

/**
 * @author sunshine
 * @date 2021/1/31
 * 小窗应用回收布局适配器
 */
class AppsSortRecyclerAdapter(
        private val pm: PackageManager,
        private val appsList: ArrayList<FreeFormAppsEntity>
) : RecyclerView.Adapter<AppsSortRecyclerAdapter.ViewHolder>() {

    private lateinit var context: Context
    private lateinit var launcherApps: LauncherApps
    private val userHandleMap = HashMap<Int, UserHandle>()

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val view: View = itemView.findViewById(R.id.view_app_click)
        val icon: ImageView = itemView.findViewById(R.id.imageView_app_icon)
        val name: AppCompatTextView = itemView.findViewById(R.id.textView_app_name)
        val switch: SwitchCompat = itemView.findViewById(R.id.switch_app)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        context = parent.context
        val userManager = context.getSystemService(Context.USER_SERVICE) as UserManager
        launcherApps = context.getSystemService(Context.LAUNCHER_APPS_SERVICE) as LauncherApps

        userManager.userProfiles.forEach {
            userHandleMap[com.sunshine.freeform.systemapi.UserHandle.getUserId(it)] = it
        }

        return ViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.item_app, parent, false))
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        //val applicationInfo = pm.getApplicationInfo(appsList[position].packageName, 0)
        val userHandle = if (userHandleMap.containsKey(appsList[position].userId)) userHandleMap[appsList[position].userId]!! else userHandleMap[0]!!
        val applicationInfo = launcherApps.getApplicationInfo(appsList[position].packageName, 0, userHandle)
        Glide.with(context)
            .load(applicationInfo.loadIcon(context.packageManager))
            .transition(DrawableTransitionOptions.withCrossFade())
            .into(holder.icon)
        holder.name.text = getLabel(applicationInfo, appsList[position].userId)
        holder.switch.visibility = View.GONE
        holder.view.setOnClickListener {  }
    }

    private fun getLabel(applicationInfo: ApplicationInfo, userId: Int): CharSequence {
        return if (userId == 0) {
            pm.getApplicationLabel(applicationInfo)
        } else {
            "${pm.getApplicationLabel(applicationInfo)}-分身${userId}"
        }
    }

    override fun getItemCount(): Int {
        return appsList.size
    }
}