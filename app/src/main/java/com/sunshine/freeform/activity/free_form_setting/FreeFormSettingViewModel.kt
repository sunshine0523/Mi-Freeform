package com.sunshine.freeform.activity.free_form_setting

import android.app.Application
import android.content.Context
import android.content.SharedPreferences
import android.content.res.Configuration
import android.graphics.Point
import android.view.WindowManager
import androidx.lifecycle.AndroidViewModel
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

/**
 * @author sunshine
 * @date 2021/2/23
 */
class FreeFormSettingViewModel(application: Application) : AndroidViewModel(application) {

    private val sp: SharedPreferences = application.getSharedPreferences("com.sunshine.freeform_preferences", Context.MODE_PRIVATE)

    private var defaultWidth = 0
    private var defaultHeight = 0
    private var orientation = application.resources.configuration.orientation
    private val mApplication = application

    private var screenWidth: Int = 0
    private var screenHeight: Int = 0

    init {
        onChanged()
    }

    fun getWidth(): Int {
        return if (orientation == Configuration.ORIENTATION_LANDSCAPE) sp.getInt("width_land", defaultWidth) else sp.getInt("width", defaultWidth)
    }

    fun getHeight(): Int {
        return if (orientation == Configuration.ORIENTATION_LANDSCAPE) sp.getInt("height_land", defaultHeight) else sp.getInt("height", defaultHeight)
    }

    fun getScreenWidth(): Int {
        return screenWidth
    }

    fun getScreenHeight(): Int {
        return screenHeight
    }

    fun save(width: Int, height: Int) {
        if (orientation == Configuration.ORIENTATION_LANDSCAPE) {
            sp.edit().apply {
                putInt("width_land", width)
                putInt("height_land", height)
                apply()
            }
        } else {
            sp.edit().apply {
                putInt("width", width)
                putInt("height", height)
                apply()
            }
        }

    }

    //屏幕旋转
    fun onChanged() {
        orientation = mApplication.resources.configuration.orientation
        val point = Point()
        val windowManager = mApplication.getSystemService(Context.WINDOW_SERVICE) as WindowManager
        windowManager.defaultDisplay.getSize(point)

        if (orientation == Configuration.ORIENTATION_LANDSCAPE) {
            screenWidth = max(point.x, point.y)
            screenHeight = min(point.x, point.y)
        } else {
            screenWidth = min(point.x, point.y)
            screenHeight = max(point.x, point.y)
        }

        //横屏
        if (orientation == Configuration.ORIENTATION_LANDSCAPE) {
            //横屏状态，默认宽为屏幕1/4，高为屏幕80%
            val saveWidth = sp.getInt("width_land", -1)
            val saveHeight = sp.getInt("height_land", -1)
            //横屏宽高反过来了
            defaultWidth = if (saveWidth == -1) screenWidth / 3 else saveWidth
            defaultHeight = if (saveHeight == -1) (screenHeight * 0.7).roundToInt() else saveHeight
        } else {
            //竖屏状态，默认宽为屏幕60%，高为屏幕40%
            val saveWidth = sp.getInt("width", -1)
            val saveHeight = sp.getInt("height", -1)
            defaultWidth = if (saveWidth == -1) (screenWidth * 0.65).roundToInt() else saveWidth
            defaultHeight = if (saveHeight == -1) (screenHeight * 0.5).roundToInt() else saveHeight
        }
    }
}