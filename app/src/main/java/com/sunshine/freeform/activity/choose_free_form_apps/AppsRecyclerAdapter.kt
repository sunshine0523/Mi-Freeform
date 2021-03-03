package com.sunshine.freeform.activity.choose_free_form_apps

import android.content.pm.PackageManager
import android.content.pm.ResolveInfo
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.appcompat.widget.AppCompatTextView
import androidx.appcompat.widget.SwitchCompat
import androidx.recyclerview.widget.RecyclerView
import com.sunshine.freeform.R

/**
 * @author sunshine
 * @date 2021/1/31
 * 小窗应用回收布局适配器
 */
class AppsRecyclerAdapter(
        private val packages: MutableList<ResolveInfo>,
        private val pm: PackageManager,
        private val viewModel: ChooseAppsViewModel,
        private val appsList: ArrayList<String>
) : RecyclerView.Adapter<AppsRecyclerAdapter.ViewHolder>() {

    //用于存储到sp中
    //private var freeFormAppsSet = freeFormAppsMap.keys.toMutableSet()

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val view: View = itemView.findViewById(R.id.view_app_click)
        val icon: ImageView = itemView.findViewById(R.id.imageView_app_icon)
        val name: AppCompatTextView = itemView.findViewById(R.id.textView_app_name)
        val switch: SwitchCompat = itemView.findViewById(R.id.switch_app)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.item_app, parent, false))
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.icon.setImageDrawable(packages[position].loadIcon(pm))
        holder.name.text = packages[position].loadLabel(pm).toString()
        holder.switch.isChecked = appsList.contains(packages[position].activityInfo.applicationInfo.packageName)
//        println(freeFormAppsSet)
//        println(packages[position].activityInfo.applicationInfo.packageName)
//        println(holder.switch.isChecked)
//        println("---------")

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
        return packages.size
    }

    private fun insert(position: Int) {
        val packageName = packages[position].activityInfo.applicationInfo.packageName
        viewModel.insertApps(packageName)
        appsList.add(packageName)
    }

    private fun delete(position: Int) {
        val packageName = packages[position].activityInfo.applicationInfo.packageName
        viewModel.deleteApps(packageName)
        appsList.remove(packageName)
    }
}