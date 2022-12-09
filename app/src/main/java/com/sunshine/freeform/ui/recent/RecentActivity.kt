package com.sunshine.freeform.ui.recent

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.WindowManager
import com.sunshine.freeform.R

class RecentActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_recent)

        window::class.java.getMethod("addSystemFlags", Int::class.javaPrimitiveType).invoke(window, 0x00080000)
    }
}