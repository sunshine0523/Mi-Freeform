package com.sunshine.freeform.activity.splash

import android.content.Context
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import com.sunshine.freeform.R
import com.sunshine.freeform.activity.main.MainActivity
import com.tencent.bugly.crashreport.CrashReport

class SplashActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash)

        //初始化bugly
        CrashReport.initCrashReport(application)

        val sp = getSharedPreferences("com.sunshine.freeform_preferences", Context.MODE_PRIVATE)

        if (sp.getBoolean("switch_hide_recent", false)) {
            startActivity(Intent(this, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS))
        } else {
            startActivity(Intent(this, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        }
        finish()
    }
}