package com.sunshine.freeform.activity.floating_view

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.*
import androidx.preference.PreferenceManager
import com.sunshine.freeform.R
import com.sunshine.freeform.broadcast.StartFloatingViewReceiver
import com.sunshine.freeform.service.CoreService
import kotlinx.coroutines.DelicateCoroutinesApi

/**
 * @author sunshine
 * @data 2021.07.15
 */
@DelicateCoroutinesApi
class FloatingViewActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        supportRequestWindowFeature(Window.FEATURE_NO_TITLE)
        setContentView(R.layout.activity_floating_view)

        val sp = PreferenceManager.getDefaultSharedPreferences(this)
        sendBroadcast(
            Intent(this, StartFloatingViewReceiver::class.java)
                .apply { putExtra("showLocation", if (sp.getBoolean(CoreService.SHOW_LOCATION, false)) -1 else 1)  })
        finish()
    }
}