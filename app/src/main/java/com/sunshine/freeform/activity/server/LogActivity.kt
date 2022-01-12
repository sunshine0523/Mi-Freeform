package com.sunshine.freeform.activity.server

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import com.sunshine.freeform.R
import com.sunshine.freeform.base.BaseActivity
import kotlinx.android.synthetic.main.activity_log.*
import java.io.ObjectInputStream
import java.net.Socket

class LogActivity : BaseActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_log)
        setTitle("运行日志")

        val handler = Handler(Looper.getMainLooper())
        Thread(Runnable {
            try {
                val socket = Socket("127.0.0.1", LOG_PORT)
                val ois = ObjectInputStream(socket.getInputStream())

                var obj: Any

                while (ois.readObject().also { obj = it as Any } != null) {
                    if (obj is String) {
                        handler.post {
                            textView_log.text = obj as String
                        }
                    }
                }
            }catch (e: Exception) {
                println(e)
            }


        }, "logSocketThread").start()
    }

    companion object {
        const val LOG_PORT = 10248;
    }
}