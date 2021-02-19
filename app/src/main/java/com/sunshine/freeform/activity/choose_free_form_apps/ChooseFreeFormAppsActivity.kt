package com.sunshine.freeform.activity.choose_free_form_apps

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.sunshine.freeform.R
import com.sunshine.freeform.service.floating.FloatingService
import kotlinx.android.synthetic.main.activity_choose_free_form_apps.*

/**
 * @author sunshine
 * @date 2021/1/31
 * 显示小窗的应用选择
 */
class ChooseFreeFormAppsActivity : AppCompatActivity() {

    private lateinit var viewModel: ChooseFreeFormAppsViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_choose_free_form_apps)

        viewModel = ViewModelProvider(this).get(ChooseFreeFormAppsViewModel::class.java)

        //获取数据库中要使用小窗的应用列表，并且放到一个表中，用于在列表中展示
        val freeFormAppsSet = HashSet<String>()
        viewModel.getAllFreeFormApps().observe(this, { list ->
            list?.forEach {
                freeFormAppsSet.add(it)
            }
            //将更新提供给服务
            FloatingService.floatingApps = list
        })

        val intent = Intent(Intent.ACTION_MAIN, null)
        intent.addCategory(Intent.CATEGORY_LAUNCHER)
        val packages = packageManager.queryIntentActivities(intent, 0)

        recycler_apps.layoutManager = LinearLayoutManager(this)
        recycler_apps.adapter = FreeFormAppsRecyclerAdapter(packages, packageManager, viewModel, freeFormAppsSet)
    }
}