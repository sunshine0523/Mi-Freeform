package com.sunshine.freeform.activity.floating_setting

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import com.sunshine.freeform.R
import kotlinx.android.synthetic.main.activity_floating_setting.*

class FloatingSettingActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_floating_setting)

        supportFragmentManager
                .beginTransaction()
                .replace(R.id.floating_setting_view, FloatingSettingView(this))
                .commit()
    }
}