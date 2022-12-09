package com.sunshine.freeform.ui.choose_apps

import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.LauncherActivityInfo
import android.os.UserManager
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Filter
import android.widget.Filterable
import android.widget.ImageView
import androidx.appcompat.widget.AppCompatTextView
import androidx.appcompat.widget.SwitchCompat
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import com.sunshine.freeform.R
import com.sunshine.freeform.room.FreeFormAppsEntity
import com.sunshine.freeform.room.NotificationAppsEntity
import com.sunshine.freeform.systemapi.UserHandle
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

/**
 * @author sunshine
 * @date 2021/1/31
 * 小窗应用回收布局适配器
 * @param allAppsList 所有应用列表
 * @param appsList 已经选择的应用列表
 */
class AppsRecyclerAdapter<T>(
    private var allAppsList: ArrayList<LauncherActivityInfo>,
    private val viewModel: ChooseAppsViewModel,
    private val appsList: ArrayList<T>,
    private val type: Int,
    private val context: Context
) : RecyclerView.Adapter<AppsRecyclerAdapter.ViewHolder>() {

    private var filterAllAppsList = allAppsList

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val view: View = itemView.findViewById(R.id.touch_root)
        val icon: ImageView = itemView.findViewById(R.id.image_icon)
        val name: AppCompatTextView = itemView.findViewById(R.id.text_title)
        val packageName: AppCompatTextView = itemView.findViewById(R.id.text_summary)
        val switch: SwitchCompat = itemView.findViewById(R.id.switch_app)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.item_app2, parent, false))
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val userId = UserHandle.getUserId(filterAllAppsList[position].user, filterAllAppsList[position].applicationInfo.uid)
        Glide.with(context)
            .load(filterAllAppsList[position].applicationInfo.loadIcon(context.packageManager))
            .transition(DrawableTransitionOptions.withCrossFade())
            .into(holder.icon)
        holder.name.text = if (userId == 0) filterAllAppsList[position].label else "${filterAllAppsList[position].label}-分身${userId}"
        holder.packageName.text = filterAllAppsList[position].applicationInfo.packageName
        if (type == ChooseAppsActivity.TYPE_FLOATING) {
            holder.switch.isChecked = contains(FreeFormAppsEntity(-1, filterAllAppsList[position].applicationInfo.packageName, userId))
        } else {
            holder.switch.isChecked = contains(NotificationAppsEntity(filterAllAppsList[position].applicationInfo.packageName, UserHandle.getUserId(filterAllAppsList[position].user, filterAllAppsList[position].applicationInfo.uid)))
        }

        //点击对应改变数据库
        holder.view.setOnClickListener {
            holder.switch.isChecked = !holder.switch.isChecked
            if (holder.switch.isChecked) {
                insert(position)
            } else {
                delete(position)
            }
        }
    }

    override fun getItemCount(): Int {
        return filterAllAppsList.size
    }

    @SuppressLint("NotifyDataSetChanged")
    fun updateDate(newAllAppsList: ArrayList<LauncherActivityInfo>) {
        filterAllAppsList = newAllAppsList
        notifyDataSetChanged()
    }

    private fun insert(position: Int) {
        val packageName = filterAllAppsList[position].applicationInfo.packageName
        viewModel.insertApps(packageName, UserHandle.getUserId(filterAllAppsList[position].user, filterAllAppsList[position].applicationInfo.uid))

        if (type == ChooseAppsActivity.TYPE_FLOATING) {
            appsList.add(FreeFormAppsEntity(-1, filterAllAppsList[position].applicationInfo.packageName, UserHandle.getUserId(filterAllAppsList[position].user, filterAllAppsList[position].applicationInfo.uid)) as T)
        } else {
            appsList.add(NotificationAppsEntity(filterAllAppsList[position].applicationInfo.packageName, UserHandle.getUserId(filterAllAppsList[position].user, filterAllAppsList[position].applicationInfo.uid)) as T)
        }

    }

    private fun delete(position: Int) {
        val packageName = filterAllAppsList[position].applicationInfo.packageName
        viewModel.deleteApps(packageName, UserHandle.getUserId(filterAllAppsList[position].user, filterAllAppsList[position].applicationInfo.uid))

        if (type == ChooseAppsActivity.TYPE_FLOATING) {
            remove(FreeFormAppsEntity(-1, filterAllAppsList[position].applicationInfo.packageName, UserHandle.getUserId(filterAllAppsList[position].user, filterAllAppsList[position].applicationInfo.uid)))
        } else {
            remove(NotificationAppsEntity(filterAllAppsList[position].applicationInfo.packageName, UserHandle.getUserId(filterAllAppsList[position].user, filterAllAppsList[position].applicationInfo.uid)))
        }
    }

    /**
     * 已经选择的列表中是否有item
     */
    private fun contains(item: FreeFormAppsEntity): Boolean {
        appsList.forEach {
            if ((it as FreeFormAppsEntity).userId == item.userId &&
                (it as FreeFormAppsEntity).packageName == item.packageName) return true
        }
        return false
    }

    /**
     * 已经选择的列表中是否有item
     */
    private fun contains(item: NotificationAppsEntity): Boolean {
        appsList.forEach {
            if ((it as NotificationAppsEntity).userId == item.userId &&
                (it as NotificationAppsEntity).packageName == item.packageName) return true
        }
        return false
    }

    /**
     * 从列表中移除
     */
    private fun remove(item: FreeFormAppsEntity) {
        var toBeRemovedItem: T? = null
        appsList.forEach {
            if ((it as FreeFormAppsEntity).userId == item.userId &&
                (it as FreeFormAppsEntity).packageName == item.packageName) {
                toBeRemovedItem = it
                return@forEach
            }
        }

        appsList.remove(toBeRemovedItem)
    }

    /**
     * 从列表中移除
     */
    private fun remove(item: NotificationAppsEntity) {
        var toBeRemovedItem: T? = null
        appsList.forEach {
            if ((it as NotificationAppsEntity).userId == item.userId &&
                (it as NotificationAppsEntity).packageName == item.packageName) {
                toBeRemovedItem = it
                return@forEach
            }
        }

        appsList.remove(toBeRemovedItem)
    }

    companion object {
        private const val TAG = "AppsRecyclerAdapter"
    }
}