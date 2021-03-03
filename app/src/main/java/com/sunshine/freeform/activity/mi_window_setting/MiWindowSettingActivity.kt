package com.sunshine.freeform.activity.mi_window_setting

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import com.sunshine.freeform.R
import com.sunshine.freeform.service.core.CoreService
import com.sunshine.freeform.service.notification.NotificationService
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

        viewModel.getAllFreeFormApps().observe(this, Observer {
            //将更新提供给服务
            CoreService.floatingApps = it
        })

        viewModel.getAllNotificationApps().observe(this, Observer {
            //将更新提供给服务
            NotificationService.notificationApps = it
        })



        supportFragmentManager
            .beginTransaction()
            .replace(R.id.mi_window_setting_view, MiWindowSettingView())
            .commit()
    }
}