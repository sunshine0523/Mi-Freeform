package com.sunshine.freeform.activity.mi_window_setting

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import androidx.lifecycle.ViewModelProvider
import com.sunshine.freeform.R
import com.sunshine.freeform.service.floating.FloatingService

/**
 * @author sunshine
 * @date 2021/1/31
 * 小窗设置
 */
class MiWindowSettingActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_mi_window_setting)

        val viewModel = ViewModelProvider(this).get(MiWindowSettingViewModel::class.java)

        viewModel.getAllFreeFormApps().observe(this, { list ->
            //将更新提供给服务
            FloatingService.floatingApps = list
        })

        val freeFormSettingView = MiWindowSettingView()
        supportFragmentManager
            .beginTransaction()
            .replace(R.id.mi_window_setting_view, freeFormSettingView)
            .commit()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
    }
}