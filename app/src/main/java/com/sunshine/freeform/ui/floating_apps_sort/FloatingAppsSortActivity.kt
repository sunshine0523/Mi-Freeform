package com.sunshine.freeform.ui.floating_apps_sort

import android.content.Context
import android.content.pm.LauncherApps
import android.os.Build
import android.os.Bundle
import android.os.UserHandle
import android.os.UserManager
import android.view.MenuItem
import android.view.View
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.sunshine.freeform.R
import com.sunshine.freeform.databinding.ActivityFloatingAppsSortBinding
import com.sunshine.freeform.room.FreeFormAppsEntity
import com.sunshine.freeform.utils.PackageUtils
import java.util.*

class FloatingAppsSortActivity : AppCompatActivity() {

    private lateinit var viewModel: FloatingAppsSortModel
    private lateinit var binding: ActivityFloatingAppsSortBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityFloatingAppsSortBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setSupportActionBar(binding.toolbar)
        supportActionBar!!.title = getString(R.string.label_sort_apps)
        supportActionBar!!.setDisplayHomeAsUpEnabled(true)

        viewModel = ViewModelProvider(this)[FloatingAppsSortModel::class.java]

        var firstInit = true

        //处理多用户
        val launcherApps: LauncherApps = getSystemService(Context.LAUNCHER_APPS_SERVICE) as LauncherApps
        val userManager = getSystemService(Context.USER_SERVICE) as UserManager
        val userHandleMap = HashMap<Int, UserHandle>()


        userManager.userProfiles.forEach {
            userHandleMap[com.sunshine.freeform.systemapi.UserHandle.getUserId(it)] = it
        }

        viewModel.getAllApps().observe(this) {

            val appsList = it as ArrayList<FreeFormAppsEntity>

            if (firstInit) {
                //删除已经卸载的app
                val noInstallAppsList = ArrayList<FreeFormAppsEntity>()
                appsList.forEach { entity ->
                    if (entity.userId == 0) {
                        if (!PackageUtils.hasInstallThisPackage(entity.packageName, packageManager)) {
                            noInstallAppsList.add(entity)
                        }
                    } else {
                        val userHandle = if (userHandleMap.containsKey(entity.userId)) userHandleMap[entity.userId]!! else userHandleMap[0]!!
                        if (!PackageUtils.hasInstallThisPackageWithUserId(entity.packageName, launcherApps, userHandle)) {
                            noInstallAppsList.add(entity)
                        }
                    }
                }
                viewModel.deleteNotInstall(noInstallAppsList)
                //有需要删除的应用时不加载，等删除后再加载
                if (noInstallAppsList.size == 0) {
                    firstInit = false

                    //加载条关闭
                    binding.progress.isIndeterminate = false
                    binding.progress.visibility = View.GONE

                    Collections.sort(appsList, AppsComparable())

                    binding.recyclerApps.layoutManager = LinearLayoutManager(this)
                    binding.recyclerApps.adapter = null
                    binding.recyclerApps.adapter = AppsSortRecyclerAdapter(packageManager, appsList)

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

                            val tempPackage = appsList[fromPosition].packageName
                            val tempUser = appsList[fromPosition].userId
                            appsList[fromPosition].packageName = appsList[targetPosition].packageName
                            appsList[fromPosition].userId = appsList[targetPosition].userId
                            appsList[targetPosition].packageName = tempPackage
                            appsList[targetPosition].userId = tempUser

                            viewModel.update(appsList[fromPosition])
                            viewModel.update(appsList[targetPosition])

                            Collections.sort(appsList, AppsComparable())

                            return true
                        }

                        override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {

                        }
                    })

                    itemTouchHelper.attachToRecyclerView(binding.recyclerApps)
                }
            }
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when(item.itemId) {
            android.R.id.home -> {
                finish()
            }
        }
        return true
    }

    inner class AppsComparable: Comparator<FreeFormAppsEntity> {

        override fun compare(o1: FreeFormAppsEntity?, o2: FreeFormAppsEntity?): Int {
            return o1!!.sortNum.compareTo(o2!!.sortNum)
        }
    }
}