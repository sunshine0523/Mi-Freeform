package com.sunshine.freeform.activity.choose_free_form_apps

import android.content.Context
import android.content.Intent
import android.content.pm.LauncherActivityInfo
import android.content.pm.LauncherApps
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.UserManager
import android.view.Menu
import android.view.MenuItem
import android.view.View
import androidx.appcompat.widget.SearchView
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.sunshine.freeform.R
import com.sunshine.freeform.activity.base.BaseActivity
import kotlinx.android.synthetic.main.activity_choose_free_form_apps.*
import java.util.*
import kotlin.collections.ArrayList

/**
 * @author sunshine
 * @date 2021/1/31
 * 显示小窗的应用选择
 */
class ChooseAppsActivity : BaseActivity() {

    private lateinit var viewModel: ChooseAppsViewModel

    private lateinit var pm: PackageManager
    private val allAppsList = ArrayList<LauncherActivityInfo>()
    private var appsList: ArrayList<*>? = null

    private lateinit var userManager: UserManager
    private lateinit var launcherApps: LauncherApps

    //如果全选/全不选触发，需要刷新界面
    private var needToUpdateView = false

    //第一次获取就加载界面，否则就不加载了，要不会频繁刷新界面
    private var firstObserver = true

    private var type = 1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_choose_free_form_apps)
        setSupportActionBar(getToolbar())

        viewModel = ViewModelProvider(this).get(ChooseAppsViewModel::class.java)

        pm = packageManager
        userManager = getSystemService(Context.USER_SERVICE) as UserManager
        launcherApps = getSystemService(Context.LAUNCHER_APPS_SERVICE) as LauncherApps

        //需要指定列表模式1 是小窗应用 2 是 气泡应用 3是选择兼容性应用
        type = intent.getIntExtra("type", 1)
        viewModel.type = type

        if (type == 1) {
            //获取数据库中要使用小窗的应用列表，并且放到一个表中，用于在列表中展示
            viewModel.getAllApps().observe(this, Observer{ list ->
                //allAppsList.clear()
                this.appsList = list as ArrayList<*>
                showAppsList(type)
            })
        } else {
            viewModel.getAllNotificationApps().observe(this, Observer { list ->
                //allAppsList.clear()
                this.appsList = list as ArrayList<*>
                showAppsList(type)
            })
        }

    }

    private fun showAppsList(type: Int) {
        //加载条关闭
        progress.isIndeterminate = false
        progress.visibility = View.GONE

        if (firstObserver || needToUpdateView) {
            val intent = Intent(Intent.ACTION_MAIN, null)
            intent.addCategory(Intent.CATEGORY_LAUNCHER)

            /**
             * 获取所有用户下的应用（包括工作资料）
             */
            userManager.userProfiles.forEach {
                allAppsList.addAll(launcherApps.getActivityList(null, it))
            }

            Collections.sort(allAppsList, AppsComparable())

            recycler_apps.layoutManager = LinearLayoutManager(this)
            recycler_apps.adapter = null

            if (type == 1) {
                recycler_apps.adapter = AppsRecyclerAdapter(allAppsList, viewModel, appsList!!, userManager, type, this)
            } else {
                recycler_apps.adapter = AppsRecyclerAdapter(allAppsList, viewModel, appsList!!, userManager, type, this)
            }

            firstObserver = false
            needToUpdateView = false
        }
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
                    recycler_apps.adapter = AppsRecyclerAdapter(allAppsList, viewModel, appsList!!, userManager, type, this@ChooseAppsActivity)
                    return true
                }

                val newAllAppsList =  ArrayList<LauncherActivityInfo>()
                allAppsList.forEach {
                    newAllAppsList.add(it)
                }

                allAppsList.forEach {
                    //不区分大小写
                    if (!it.label.toString().contains(newText, true)) {
                        newAllAppsList.remove(it)
                    }
                }

                recycler_apps.adapter = null
                recycler_apps.adapter = AppsRecyclerAdapter(newAllAppsList, viewModel, appsList!!, userManager, type, this@ChooseAppsActivity)
                return true
            }

        })

        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.choose_all -> {
                needToUpdateView = true
                viewModel.insertAllApps(allAppsList, userManager)
                allAppsList.clear()
            }
            R.id.choose_all_cancel -> {
                needToUpdateView = true
                viewModel.deleteAll()
                allAppsList.clear()
            }
        }
        return super.onOptionsItemSelected(item)
    }

    inner class AppsComparable: Comparator<LauncherActivityInfo>{

        override fun compare(o1: LauncherActivityInfo?, o2: LauncherActivityInfo?): Int {
            return o1!!.label.toString().compareTo(o2!!.label.toString())
        }
    }
}