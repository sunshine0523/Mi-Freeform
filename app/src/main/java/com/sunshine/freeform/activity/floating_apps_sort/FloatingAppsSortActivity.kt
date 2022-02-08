package com.sunshine.freeform.activity.floating_apps_sort

import android.os.Bundle
import android.view.View
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.sunshine.freeform.R
import com.sunshine.freeform.activity.base.BaseActivity
import com.sunshine.freeform.room.FreeFormAppsEntity
import com.sunshine.freeform.utils.PackageUtils
import kotlinx.android.synthetic.main.activity_choose_free_form_apps.*
import java.util.*
import kotlin.collections.ArrayList

class FloatingAppsSortActivity : BaseActivity() {

    private lateinit var viewModel: FloatingAppsSortModel
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_choose_free_form_apps)

        setTitle(getString(R.string.sort_apps_label))

        viewModel = ViewModelProvider(this).get(FloatingAppsSortModel::class.java)

        var firstInit = true

        viewModel.getAllApps().observe(this, Observer {

            val appsList = it as ArrayList<FreeFormAppsEntity>

            if (firstInit) {
                //删除已经卸载的app
                val noInstallAppsList = ArrayList<FreeFormAppsEntity>()
                appsList.forEach {
                    if (!PackageUtils.hasInstallThisPackage(it.packageName, packageManager)) {
                        noInstallAppsList.add(it)
                    }
                }
                viewModel.deleteNotInstall(noInstallAppsList)
                //有需要删除的应用时不加载，等删除后再加载
                if (noInstallAppsList.size == 0) {
                    firstInit = false

                    //加载条关闭
                    progress.isIndeterminate = false
                    progress.visibility = View.GONE

                    Collections.sort(appsList, AppsComparable())

                    recycler_apps.layoutManager = LinearLayoutManager(this)
                    recycler_apps.adapter = null
                    recycler_apps.adapter = AppsSortRecyclerAdapter(packageManager, appsList)

                    val itemTouchHelper = ItemTouchHelper(object : ItemTouchHelper.Callback() {
                        override fun getMovementFlags(
                                recyclerView: RecyclerView,
                                viewHolder: RecyclerView.ViewHolder
                        ): Int {
                            val swipeFlags = 0
                            val dragFlags = ItemTouchHelper.UP or
                                    ItemTouchHelper.DOWN
                            return makeMovementFlags(dragFlags, swipeFlags)
                        }

                        override fun onMove(
                                recyclerView: RecyclerView,
                                viewHolder: RecyclerView.ViewHolder,
                                target: RecyclerView.ViewHolder
                        ): Boolean {
                            val fromPosition = viewHolder.adapterPosition
                            val targetPosition = target.adapterPosition

                            if (fromPosition < targetPosition) {
                                for (i in fromPosition until targetPosition) {
                                    Collections.swap(appsList, i, i + 1)
                                }
                            } else {
                                for (i in fromPosition downTo targetPosition + 1) {
                                    Collections.swap(appsList, i, i - 1)
                                }
                            }
                            recyclerView.adapter?.notifyItemMoved(fromPosition, targetPosition)

                            val temp = appsList[fromPosition].packageName
                            appsList[fromPosition].packageName = appsList[targetPosition].packageName
                            appsList[targetPosition].packageName = temp

                            viewModel.update(appsList[fromPosition])
                            viewModel.update(appsList[targetPosition])

                            Collections.sort(appsList, AppsComparable())

                            return true
                        }

                        override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {

                        }
                    })

                    itemTouchHelper.attachToRecyclerView(recycler_apps)
                }
            }
        })
    }

    inner class AppsComparable: Comparator<FreeFormAppsEntity> {

        override fun compare(o1: FreeFormAppsEntity?, o2: FreeFormAppsEntity?): Int {
            return o1!!.sortNum.compareTo(o2!!.sortNum)
        }
    }
}