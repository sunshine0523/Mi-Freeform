package com.sunshine.freeform.activity.floating_setting

import android.os.Bundle
import com.sunshine.freeform.R
import com.sunshine.freeform.activity.base.BaseActivity

class FloatingSettingActivity : BaseActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_floating_setting)
        setTitle(getString(R.string.floating_label))

        supportFragmentManager
                .beginTransaction()
                .replace(R.id.floating_setting_view, FloatingSettingView())
                .commit()
    }
}