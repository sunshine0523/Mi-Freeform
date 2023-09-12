package com.sunshine.freeform.ui.freeform

import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import com.sunshine.freeform.MiFreeformServiceManager
import com.sunshine.freeform.R
import kotlin.math.roundToInt

/**
 * @author KindBrave
 * @since 2023/9/11
 * Use for open a freeform window
 */
class FreeformActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_floating)
        val viewModel = FreeformViewModel(this.application)

        val key = intent?.getStringExtra("key")
        val isClearable = intent?.getBooleanExtra("isClearable", false) ?: false
        val pendingIntent =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) intent?.getParcelableExtra("pendingIntent", PendingIntent::class.java)
            else intent?.getParcelableExtra("pendingIntent") as PendingIntent?
        Log.i("Mi-Freeform", "$key $isClearable $pendingIntent ${pendingIntent?.creatorPackage}")
        MiFreeformServiceManager.createWindow(
            pendingIntent,
            viewModel.getIntSp("freeform_width", (viewModel.screenWidth * 0.8).roundToInt()),
            viewModel.getIntSp("freeform_height", (viewModel.screenHeight * 0.5).roundToInt()),
            viewModel.getIntSp("freeform_dpi", viewModel.screenDensityDpi),
        )
        if (isClearable) MiFreeformServiceManager.cancelNotification(key)
        finish()
    }
}