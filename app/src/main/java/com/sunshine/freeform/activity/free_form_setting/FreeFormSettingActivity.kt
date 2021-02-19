package com.sunshine.freeform.activity.free_form_setting

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import com.sunshine.freeform.R

class FreeFormSettingActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_free_form_setting)

        val freeFormSettingView = FreeFormSettingView()

        supportFragmentManager
                .beginTransaction()
                .replace(R.id.free_form_setting_view, freeFormSettingView)
                .commit()
    }
}