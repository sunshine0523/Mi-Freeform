package com.sunshine.freeform.ui.choose_apps

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.pm.LauncherActivityInfo
import android.content.pm.LauncherApps
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.UserManager
import android.view.Menu
import android.view.MenuItem
import android.widget.Filter
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SearchView
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.sunshine.freeform.R
import com.sunshine.freeform.databinding.ActivityChooseAppsBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.Collator
import kotlin.collections.ArrayList

/**
 * @author sunshine
 * @date 2021/1/31
 * 显示小窗的应用选择
 */
class ChooseAppsActivity : AppCompatActivity() {

    private lateinit var binding: ActivityChooseAppsBinding
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

    private var type = TYPE_FLOATING

    private var adapter: AppsRecyclerAdapter<*>? = null

    private val filter = AppNameFilter()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityChooseAppsBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setSupportActionBar(binding.toolbar)
        supportActionBar!!.setDisplayHomeAsUpEnabled(true)

        viewModel = ViewModelProvider(this)[ChooseAppsViewModel::class.java]

        pm = packageManager
        userManager = getSystemService(Context.USER_SERVICE) as UserManager
        launcherApps = getSystemService(Context.LAUNCHER_APPS_SERVICE) as LauncherApps

        //需要指定列表模式1 是小窗应用 2 是 气泡应用 3是选择兼容性应用
        type = intent.getIntExtra("type", TYPE_FLOATING)
        viewModel.type = type

        if (type == TYPE_FLOATING) {
            supportActionBar!!.title = getString(R.string.label_floating_apps)
            //获取数据库中要使用小窗的应用列表，并且放到一个表中，用于在列表中展示
            viewModel.getAllApps().observe(this@ChooseAppsActivity) { list ->
                this@ChooseAppsActivity.appsList = list as ArrayList<*>
                showAppsList(type)
            }
        } else {
            supportActionBar!!.title = getString(R.string.label_notification_apps)
            viewModel.getAllNotificationApps().observe(this) { list ->
                //allAppsList.clear()
                this.appsList = list as ArrayList<*>
                showAppsList(type)
            }
        }

    }

    private fun showAppsList(type: Int) {
        if (firstObserver || needToUpdateView) {
            GlobalScope.launch(Dispatchers.IO) {
                val intent = Intent(Intent.ACTION_MAIN, null)
                intent.addCategory(Intent.CATEGORY_LAUNCHER)

                if (allAppsList.isEmpty()) {
                    /**
                     * 获取所有用户下的应用（包括工作资料）
                     */
                    userManager.userProfiles.forEach {
                        allAppsList.addAll(launcherApps.getActivityList(null, it))
                    }

                    allAppsList.sortWith { o1, o2 ->
                        Collator.getInstance().compare(
                            o1!!.applicationInfo.loadLabel(packageManager),
                            o2!!.applicationInfo.loadLabel(packageManager)
                        )
                    }
                }

                withContext(Dispatchers.Main) {
                    //加载条关闭
                    binding.lottieView.cancelAnimation()
                    binding.lottieView.animate().alpha(0f).setDuration(300).start()

                    binding.recyclerApps.layoutManager = LinearLayoutManager(this@ChooseAppsActivity)
                    adapter = AppsRecyclerAdapter(allAppsList, viewModel, appsList!!, type, this@ChooseAppsActivity)
                    binding.recyclerApps.adapter = adapter

                    firstObserver = false
                    needToUpdateView = false
                }
            }
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_choose_apps, menu)

        //需要在这里获取搜索框
        val searchItem = menu.findItem(R.id.app_bar_search)
        val searchView = searchItem?.actionView as SearchView
        searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String): Boolean {
                //adapter?.filter?.filter(query)
                return false
            }

            override fun onQueryTextChange(newText: String): Boolean {
                filter.filter(newText)
                return true
            }
        })

        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            android.R.id.home -> {
                finish()
            }
            R.id.choose_all -> {
                needToUpdateView = true
                viewModel.insertAllApps(allAppsList, userManager)
                //allAppsList.clear()
            }
            R.id.choose_all_cancel -> {
                //如果列表是空，则不进行更新
                if (appsList != null && appsList!!.isNotEmpty()) {
                    needToUpdateView = true
                    viewModel.deleteAll()
                    //allAppsList.clear()
                }
            }
        }
        return super.onOptionsItemSelected(item)
    }

    inner class AppNameFilter : Filter() {
        override fun performFiltering(constraint: CharSequence): FilterResults {
            val results = FilterResults()
            var newAllAppsList =  ArrayList<LauncherActivityInfo>()

            if (constraint.isBlank()) {
                newAllAppsList = allAppsList
            } else {
                allAppsList.forEach {
                    if (it.label.contains(constraint, ignoreCase = true)) {
                        newAllAppsList.add(it)
                    }
                }
            }

            results.values = newAllAppsList
            results.count = newAllAppsList.size
            return results
        }

        @SuppressLint("NotifyDataSetChanged")
        override fun publishResults(constraint: CharSequence, results: FilterResults) {
            adapter?.updateDate(results.values as ArrayList<LauncherActivityInfo>)
        }

    }

    companion object {
        const val TYPE_FLOATING = 1
        const val TYPE_NOTIFICATION = 2
    }
}