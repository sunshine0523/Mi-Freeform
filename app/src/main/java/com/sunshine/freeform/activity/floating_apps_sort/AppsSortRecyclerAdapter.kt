package com.sunshine.freeform.activity.floating_apps_sort

import android.content.pm.PackageManager
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.appcompat.widget.AppCompatTextView
import androidx.appcompat.widget.SwitchCompat
import androidx.recyclerview.widget.RecyclerView
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
        val applicationInfo = pm.getApplicationInfo(appsList[position].packageName, 0)

        holder.icon.setImageDrawable(applicationInfo.loadIcon(pm))
        holder.name.text = pm.getApplicationLabel(applicationInfo)
        holder.switch.visibility = View.GONE
        holder.view.setOnClickListener {  }
    }

    override fun getItemCount(): Int {
        return appsList.size
    }
}