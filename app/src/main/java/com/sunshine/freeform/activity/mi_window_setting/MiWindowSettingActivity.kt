package com.sunshine.freeform.activity.mi_window_setting

import android.os.Bundle
import com.sunshine.freeform.R
import com.sunshine.freeform.activity.base.BaseActivity

/**
 * @author sunshine
 * @date 2021/1/31
 * 小窗设置
 */
class MiWindowSettingActivity : BaseActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_mi_window_setting)
        setTitle(getString(R.string.mi_freeform_setting_label))

        supportFragmentManager
            .beginTransaction()
            .replace(R.id.mi_window_setting_view, MiWindowSettingView())
            .commit()
    }
}