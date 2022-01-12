package com.sunshine.freeform.service

import android.annotation.SuppressLint
import android.app.Service
import android.content.Intent
import android.content.res.Configuration
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.Point
import android.net.Uri
import android.os.IBinder
import android.provider.Settings
import android.util.Log
import android.view.*
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.Toast
import com.sunshine.freeform.R
import com.sunshine.freeform.view.floating.FloatingView
import com.sunshine.freeform.view.floating.FreeFormView
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

@DelicateCoroutinesApi
class Floating2Service : Service() {
    private lateinit var floatingView: LinearLayout
    private lateinit var button: ImageView

    private lateinit var windowManager: android.view.WindowManager
    private lateinit var floatingLayoutParams: android.view.WindowManager.LayoutParams

    private lateinit var floatingDataStoreViewModel: FloatingDataStoreViewModel

    private var showLocation = SHOW_LOCATION_DEFAULT

    private var orientation = Configuration.ORIENTATION_UNDEFINED

    companion object {
        private const val TAG = "Floating2Service"
        private const val FLOATING_POSITION_KEY = "floating_position"
        private const val FLOATING_BUTTON_SIZE = "floating_button_size"
        private const val DEFAULT_BUTTON_HEIGHT = 250
        private const val DEFAULT_BUTTON_WIDTH = 75
        private const val SHOW_LOCATION = "show_location"
        //floating button default location is left
        private const val SHOW_LOCATION_DEFAULT = -1
        private const val FLOATING_SHOW_LOCATION_Y_LANDSCAPE = "floating_show_location_y_landscape"
        private const val FLOATING_SHOW_LOCATION_Y_PORTRAIT = "floating_show_location_y_portrait"
    }

    override fun onBind(intent: Intent): IBinder {
        TODO("Return the communication channel to the service.")
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        if (newConfig.orientation != orientation) {
            removeFloatingView()
            updateFloating()
            //update FloatingView&FreeFormView
            FloatingView.orientationChangedListener?.onChanged(newConfig.orientation)
            FreeFormView.orientationChangedListener.onChanged(newConfig.orientation)
            orientation = newConfig.orientation
        }
        super.onConfigurationChanged(newConfig)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        init()
        updateFloating()
        return START_NOT_STICKY
    }

    override fun onDestroy() {
        removeFloatingView()
        super.onDestroy()
    }

    private fun init() {
        floatingDataStoreViewModel = FloatingDataStoreViewModel(this)
        windowManager = getSystemService(WINDOW_SERVICE) as android.view.WindowManager
        floatingView = LinearLayout(this)
        floatingView.setBackgroundColor(Color.TRANSPARENT)
        floatingView.alpha  = 0.75f
        orientation = resources.configuration.orientation
        GlobalScope.launch(Dispatchers.IO) {
            floatingView.gravity = if (floatingDataStoreViewModel.readIntFlow(FLOATING_POSITION_KEY).first() == 1) Gravity.END else Gravity.START
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun updateFloating() {
        if (Settings.canDrawOverlays(this)) {
            //get screen width&height size
            val point = Point()
            windowManager.defaultDisplay.getSize(point)

            GlobalScope.launch(Dispatchers.IO) {
                val buttonHeight = floatingDataStoreViewModel.readIntFlow(FLOATING_BUTTON_SIZE, DEFAULT_BUTTON_HEIGHT).first()
                showLocation = floatingDataStoreViewModel.readIntFlow(SHOW_LOCATION, SHOW_LOCATION_DEFAULT).first()
                //get user`s preference about y position
                val y = if (resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE) {
                    floatingDataStoreViewModel.readIntFlow(FLOATING_SHOW_LOCATION_Y_LANDSCAPE, -(point.y / 2)).first()
                } else {
                    floatingDataStoreViewModel.readIntFlow(FLOATING_SHOW_LOCATION_Y_PORTRAIT, -(point.y / 2)).first()
                }

                launch(Dispatchers.Main) {
                    button = ImageView(this@Floating2Service)
                    button.setImageResource(R.drawable.ic_bar)
                    button.layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
                    floatingView.addView(button)
                    floatingLayoutParams = android.view.WindowManager.LayoutParams()

                    floatingLayoutParams.apply {
                        type = android.view.WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY

                        width = DEFAULT_BUTTON_WIDTH
                        height = buttonHeight

                        //设置这个在点击外部时相应外部操作
                        flags = android.view.WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or android.view.WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                        format = PixelFormat.RGBA_8888
                        gravity = Gravity.CENTER_VERTICAL

                        //get real screen width
                        val screenWidth = if (resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE) {
                            max(point.x, point.y)
                        } else {
                            min(point.x, point.y)
                        }

                        x = screenWidth / 2 * showLocation
                        this.y = y
                    }

                    floatingView.setOnTouchListener(FloatingTouchListener())

                    windowManager.addView(floatingView, floatingLayoutParams)

                }//Main
            }//IO

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
            Log.e(TAG, "$e")
        }
    }

    inner class FloatingTouchListener : View.OnTouchListener {
        private var posX = 0.0f
        private var posY = 0.0f
        private var curX = 0.0f
        private var curY = 0.0f
        //If have done that don`t done
        var hasDone = false
        @SuppressLint("ClickableViewAccessibility")
        override fun onTouch(v: View?, event: MotionEvent?): Boolean {
            when(event?.action) {
                MotionEvent.ACTION_DOWN -> {
                    posX = event.x
                    posY = event.y
                }
                MotionEvent.ACTION_MOVE -> {
                    curX = event.x
                    curY = event.y
                    if (!hasDone) {
                        when {
                            (curX - posX) > 100 -> {
                                try {
                                    FloatingView(
                                        this@Floating2Service,
                                        showLocation
                                    )
                                    hasDone = true
                                }catch (e: Exception) {

                                }

                            }
                            (curY - posY) > 0 && (abs(curY - posY) > 25) -> Log.v(TAG, "下")
                            (curY - posY) < 0 && (abs(curY - posY) > 25) -> Log.v(TAG, "上")
                        }
                    }

                }
                MotionEvent.ACTION_UP -> {
                    hasDone = false
                }
            }
            return true
        }

    }
}