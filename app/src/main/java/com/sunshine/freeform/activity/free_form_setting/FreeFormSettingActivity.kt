package com.sunshine.freeform.activity.free_form_setting

import android.os.Bundle
import com.sunshine.freeform.R
import com.sunshine.freeform.base.BaseActivity

class FreeFormSettingActivity : BaseActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_free_form_setting)
        setTitle(getString(R.string.freeform_setting_label))

        val freeFormSettingView = FreeFormSettingView()

        supportFragmentManager
                .beginTransaction()
                .replace(R.id.free_form_setting_view, freeFormSettingView)
                .commit()
    }
}