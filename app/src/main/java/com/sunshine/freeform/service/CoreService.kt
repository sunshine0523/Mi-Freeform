package com.sunshine.freeform.service

import android.annotation.SuppressLint
import android.app.*
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.res.Configuration
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.Point
import android.net.Uri
import android.os.IBinder
import android.provider.Settings
import android.view.*
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.Toast
import androidx.preference.PreferenceManager
import com.sunshine.freeform.R
import com.sunshine.freeform.broadcast.StartFloatingViewReceiver
import com.sunshine.freeform.callback.ServiceDataListener
import com.sunshine.freeform.view.floating.FloatingView
import com.sunshine.freeform.view.floating.FreeFormView
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

@DelicateCoroutinesApi
class CoreService : Service(), ServiceDataListener {
    private lateinit var sp: SharedPreferences

    private lateinit var floatingView: LinearLayout
    private lateinit var button: ImageView

    private lateinit var windowManager: WindowManager
    private lateinit var floatingLayoutParams: WindowManager.LayoutParams

    private var showLocation = SHOW_LOCATION_DEFAULT

    private var orientation = Configuration.ORIENTATION_UNDEFINED

    private var screenWidth = 0

    companion object {
        private const val TAG = "CoreService"
        private const val FLOATING_BUTTON_SIZE = "floating_button_size"
        private const val DEFAULT_BUTTON_HEIGHT = 250
        private const val DEFAULT_BUTTON_WIDTH = 75
        const val SHOW_LOCATION = "show_location"
        private const val SHOW_FOREGROUND = "show_foreground"
        //floating button default location is left
        const val SHOW_LOCATION_DEFAULT = -1
        private const val FLOATING_SHOW_LOCATION_Y_LANDSCAPE = "floating_show_location_y_landscape"
        private const val FLOATING_SHOW_LOCATION_Y_PORTRAIT = "floating_show_location_y_portrait"
        const val SHOW_FLOATING = "show_floating"
        private const val CHANNEL_ID = "CHANNEL_ID_SUNSHINE_FREEFORM_FOREGROUND"

        private var serviceDataListener: ServiceDataListener? = null
        fun getServiceDataListener() = serviceDataListener
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        if (newConfig.orientation != orientation) {
            if (sp.getBoolean(SHOW_FLOATING, false)) {
                removeFloatingView()
                updateFloating()
                //update FloatingView&FreeFormView
                FloatingView.orientationChangedListener?.onChanged(newConfig.orientation)
                FreeFormView.orientationChangedListener.onChanged(newConfig.orientation)
            }
            orientation = newConfig.orientation
        }
        super.onConfigurationChanged(newConfig)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        sp = PreferenceManager.getDefaultSharedPreferences(this)
        serviceDataListener = this

        if (sp.getBoolean(SHOW_FLOATING, false)) initBar()
        if (sp.getBoolean(SHOW_FOREGROUND, false)) startForeground()
        return START_NOT_STICKY
    }

    override fun onDestroy() {
        serviceDataListener = null
        if (sp.getBoolean(SHOW_FLOATING, false)) removeFloatingView()
        if (sp.getBoolean(SHOW_FOREGROUND, false)) stopForeground(true)
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    private fun initBar() {
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        floatingView = LinearLayout(this)
        floatingView.setBackgroundColor(Color.TRANSPARENT)
        floatingView.alpha  = 0.75f
        orientation = resources.configuration.orientation
        updateFloating()
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun updateFloating() {
        if (Settings.canDrawOverlays(this)) {
            //get screen width&height size
            val point = Point()
            windowManager.defaultDisplay.getSize(point)
            val buttonHeight = sp.getInt(FLOATING_BUTTON_SIZE, DEFAULT_BUTTON_HEIGHT)
            showLocation = if (sp.getBoolean(SHOW_LOCATION, false)) 1 else -1
            //get user`s preference about y position
            val y = if (resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE) {
                sp.getInt(FLOATING_SHOW_LOCATION_Y_LANDSCAPE, -(point.y / 2))
            } else {
                sp.getInt(FLOATING_SHOW_LOCATION_Y_PORTRAIT, -(point.y / 2))
            }

            button = ImageView(this@CoreService)
            button.setImageResource(R.drawable.ic_bar)
            button.layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
            floatingView.addView(button)
            floatingLayoutParams = WindowManager.LayoutParams()

            floatingLayoutParams.apply {
                type = WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY

                width = DEFAULT_BUTTON_WIDTH
                height = buttonHeight

                //设置这个在点击外部时相应外部操作
                flags = WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                format = PixelFormat.RGBA_8888
                gravity = Gravity.CENTER_VERTICAL

                //get real screen width
                screenWidth = if (resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE) {
                    max(point.x, point.y)
                } else {
                    min(point.x, point.y)
                }

                x = screenWidth / 2 * showLocation
                this.y = y
            }

            floatingView.setOnTouchListener(FloatingTouchListener())

            windowManager.addView(floatingView, floatingLayoutParams)

        } else {
            try {
                Toast.makeText(this, getString(R.string.request_overlay_permission), Toast.LENGTH_LONG).show()
                val intent = Intent(
                    Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    Uri.parse("package:${packageName}")
                )
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                startActivity(
                    intent
                )
            } catch (e: Exception) {
                Toast.makeText(this, getString(R.string.request_overlay_permission_fail), Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun removeFloatingView() {
        try {
            windowManager.removeViewImmediate(floatingView)
        }catch (e: Exception){

        }
    }

    private fun startForeground() {
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val notificationIntent = Intent(this, StartFloatingViewReceiver::class.java)
        notificationIntent.putExtra("showLocation", if (sp.getBoolean(SHOW_LOCATION, false)) -1 else 1)
        val pendingIntent = PendingIntent.getBroadcast(this, 0, notificationIntent, PendingIntent.FLAG_IMMUTABLE)
        val builder = Notification.Builder(this.applicationContext,
            CHANNEL_ID
        )
        //创建通知渠道
        val channel = NotificationChannel(
            CHANNEL_ID,
            getString(R.string.foreground_notification_name),
            NotificationManager.IMPORTANCE_HIGH
        )
        notificationManager.createNotificationChannel(channel)

        builder.setContentIntent(pendingIntent)
            .setLargeIcon(BitmapFactory.decodeResource(resources, R.mipmap.ic_launcher))
            .setContentTitle(getString(R.string.foreground_notification_title))
            .setContentText(getString(R.string.foreground_notification_text))
            .setSmallIcon(R.drawable.tile_icon)
            .setWhen(System.currentTimeMillis())
        val notification = builder.build()
        notification.flags = Notification.FLAG_ONGOING_EVENT or Notification.FLAG_NO_CLEAR

        startForeground(3, notification)
    }

    override fun onDataChanged(key: String, newValue: Any) {
        when (key) {
            SHOW_FLOATING -> {
                if (newValue as Boolean) {
                    removeFloatingView()
                    initBar()
                } else {
                    removeFloatingView()
                }
            }
            SHOW_FOREGROUND -> {
                if (newValue as Boolean) {
                    startForeground()
                } else {
                    stopForeground(true)
                }
            }
            SHOW_LOCATION -> {
                if (sp.getBoolean(SHOW_FLOATING, false)) {
                    showLocation = if (newValue as Boolean) 1 else -1
                    windowManager.updateViewLayout(floatingView, floatingLayoutParams.apply { x = screenWidth / 2 * showLocation })
                }
            }
        }
    }

    @DelicateCoroutinesApi
    inner class FloatingTouchListener : View.OnTouchListener {
        private var posX = 0.0f
        private var posY = 0.0f
        private var curX = 0.0f
        private var curY = 0.0f
        private var lastX = 0.0f
        private var lastY = 0.0f
        //If have done that don`t done
        private var hasDone = false
        //If floating is moved,changed.
        private var hasMoved = false
        @SuppressLint("ClickableViewAccessibility")
        override fun onTouch(v: View?, event: MotionEvent?): Boolean {
            when(event?.action) {
                MotionEvent.ACTION_DOWN -> {
                    posX = event.x
                    posY = event.y
                    lastX = event.rawX
                    lastY = event.rawY
                    v?.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
                }
                MotionEvent.ACTION_MOVE -> {
                    curX = event.x
                    curY = event.y
                    if (abs(curY - posY) > 50) {
                        windowManager.updateViewLayout(
                            floatingView,
                            floatingLayoutParams.apply {
                                y += (event.rawY - lastY).roundToInt()
                            })
                        hasMoved = true
                    }
                    lastX = event.rawX
                    lastY = event.rawY
                }
                MotionEvent.ACTION_UP -> {
                    hasDone = false
                    if (hasMoved) {
                        if (resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE) {
                            sp.edit().putInt(FLOATING_SHOW_LOCATION_Y_LANDSCAPE, floatingLayoutParams.y).apply()
                        } else {
                            sp.edit().putInt(FLOATING_SHOW_LOCATION_Y_PORTRAIT, floatingLayoutParams.y).apply()
                        }
                        hasMoved = false
                    }else {
                        //点击即可选择界面
                        try {
                            FloatingView(
                                this@CoreService,
                                showLocation
                            )
                            hasDone = true
                        }catch (e: Exception) {

                        }
                    }
                }
            }
            return true
        }

    }
}