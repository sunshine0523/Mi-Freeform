package com.sunshine.freeform.activity.choose_free_form_apps

import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ResolveInfo

import android.os.Bundle
import android.view.*

import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SearchView
import androidx.core.view.MenuItemCompat

import com.sunshine.freeform.R
import com.sunshine.freeform.service.core.CoreService
import com.sunshine.freeform.service.notification.NotificationService
import com.sunshine.freeform.service.floating.FloatingService

import kotlinx.android.synthetic.main.activity_choose_free_form_apps.*

import java.util.*

import kotlin.Comparator
import kotlin.collections.ArrayList

/**
 * @author sunshine
 * @date 2021/1/31
 * 显示小窗的应用选择
 */
class ChooseAppsActivity : AppCompatActivity() {

    private lateinit var viewModel: ChooseAppsViewModel

    private lateinit var pm: PackageManager
    private var packages: MutableList<ResolveInfo>? = null
    var appsList: ArrayList<String>? = null

    //如果全选/全不选触发，需要刷新界面
    private var needToUpdateView = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_choose_free_form_apps)

        viewModel = ViewModelProvider(this).get(ChooseAppsViewModel::class.java)

        pm = packageManager

        //需要指定列表模式1 是小窗应用 2 是 气泡应用
        val type = intent.getIntExtra("type", 1)
        viewModel.type = type

        //第一次获取就加载界面，否则就不加载了，要不会频繁刷新界面
        var firstObserver = true

        //获取数据库中要使用小窗的应用列表，并且放到一个表中，用于在列表中展示
        viewModel.getAllApps().observe(this, Observer{ list ->
            appsList = list as ArrayList<String>
            if (type == 1) {
                //将更新提供给服务
                CoreService.floatingApps = list
            } else {
                NotificationService.notificationApps = list
            }
            if (firstObserver || needToUpdateView) {
                val intent = Intent(Intent.ACTION_MAIN, null)
                intent.addCategory(Intent.CATEGORY_LAUNCHER)
                packages = pm.queryIntentActivities(intent, 0)

                Collections.sort(packages, AppsComparable())

                recycler_apps.layoutManager = LinearLayoutManager(this)
                recycler_apps.adapter = null
                recycler_apps.adapter = AppsRecyclerAdapter(packages!!, pm, viewModel, appsList!!)

                firstObserver = false
                needToUpdateView = false
            }
        })
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_choose_apps, menu)

        //需要在这里获取搜索框
        val searchItem = menu?.findItem(R.id.app_bar_search)
        val searchView = searchItem?.actionView as SearchView
        searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                return true
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                if (newText.isNullOrBlank()) {
                    recycler_apps.adapter = null
                    recycler_apps.adapter = AppsRecyclerAdapter(packages!!, pm, viewModel, appsList!!)
                    return true
                }

                val newPackages: MutableList<ResolveInfo> = arrayListOf()
                packages?.forEach {
                    newPackages.add(it)
                }

                packages?.forEach {
                    //不区分大小写
                    if (!it.loadLabel(pm).toString().contains(newText, true)) {
                        newPackages.remove(it)
                    }
                }

                recycler_apps.adapter = null
                recycler_apps.adapter = AppsRecyclerAdapter(newPackages, pm, viewModel, appsList!!)
                return true
            }

        })

        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.choose_all -> {
                viewModel.insertAllApps(packages)
                needToUpdateView = true
            }
            R.id.choose_all_cancel -> {
                viewModel.deleteAll()
                needToUpdateView = true
            }
        }
        return super.onOptionsItemSelected(item)
    }

    inner class AppsComparable: Comparator<ResolveInfo>{

        override fun compare(o1: ResolveInfo?, o2: ResolveInfo?): Int {
            return o1!!.loadLabel(pm).toString().compareTo(o2!!.loadLabel(pm).toString())
        }
    }
}