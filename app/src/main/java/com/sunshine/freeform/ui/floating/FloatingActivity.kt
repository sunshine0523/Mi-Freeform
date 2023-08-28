package com.sunshine.freeform.ui.floating

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import com.sunshine.freeform.R

class FloatingActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_floating)
        FloatingWindow(application.applicationContext, intent.getBooleanExtra("isLeft", true))
        finish()
    }
}