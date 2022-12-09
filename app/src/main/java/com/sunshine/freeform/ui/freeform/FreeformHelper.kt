package com.sunshine.freeform.ui.freeform

import android.content.Context
import android.content.res.Configuration
import android.hardware.display.DisplayManager
import android.util.DisplayMetrics
import android.util.Log
import android.view.Display
import android.view.Surface
import android.view.WindowManager
import com.sunshine.freeform.app.MiFreeform
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

/**
 * @date 2022/8/22
 * @author sunshine0523
 * 小窗帮助类
 */
object FreeformHelper {
    //小窗默认高度
    const val PORTRAIT_HEIGHT = 1200
    const val LANDSCAPE_HEIGHT = 800

    //小窗默认DPI
    const val PORTRAIT_DPI = 500
    const val LANDSCAPE_DPI = 500

    //正在展示的小窗
    private val freeformStackSet = StackSet()
    //挂在后台的小窗
    private val miniFreeformStackSet = StackSet()

    //记录包名+userId，更加快速
    private val freeformPackageSet = HashSet<String>()
    private val miniFreeformPackageSet = HashSet<String>()

    fun getDefaultHeight(context: Context): Int {
        return getDefaultHeight(context, (context.getSystemService(Context.DISPLAY_SERVICE) as DisplayManager).getDisplay(Display.DEFAULT_DISPLAY))
    }

    fun getDefaultHeight(context: Context, defaultDisplay: Display): Int {
        return getDefaultHeight(context, defaultDisplay, 10f / 16f)
    }

    /**
     * @param defaultDisplay 默认屏幕，用于获取屏幕方向
     * @param widthHeightRadio 小窗的宽高比例
     */
    fun getDefaultHeight(context: Context, defaultDisplay: Display, widthHeightRadio: Float): Int {
        val screenWidth = min(context.resources.displayMetrics.heightPixels, context.resources.displayMetrics.widthPixels)
        val rotation = defaultDisplay.rotation
        return if (rotation == Surface.ROTATION_0 || rotation == Surface.ROTATION_180) (screenWidth / widthHeightRadio).roundToInt() else screenWidth
    }

    fun getDefaultDpi(context: Context): Int {
        val sp = context.getSharedPreferences(MiFreeform.APP_SETTINGS_NAME, Context.MODE_PRIVATE)
        val orientation = context.resources.configuration.orientation
        val freeformHeight = getDefaultHeight(context)
        val screenDpi = context.resources.displayMetrics.densityDpi
        val screenHeight = context.resources.displayMetrics.heightPixels
        return if (orientation == Configuration.ORIENTATION_LANDSCAPE) {
            (screenHeight * sp.getInt("freeform_landscape_dpi", 75) / 100.0f / freeformHeight * screenDpi).roundToInt()
        } else {
            //这个0.75就是测试出来的，就是感官上dpi合适的值
            (screenHeight * sp.getInt("freeform_dpi", 75) / 100.0f / freeformHeight * screenDpi).roundToInt()
        }
    }

    fun getScreenDpi(context: Context): Int {
        return context.resources.displayMetrics.densityDpi
    }

    /**
     * @param rotation 屏幕方向是否是竖屏
     * @see Surface.ROTATION_0
     * @return orientation
     */
    fun screenIsPortrait(rotation: Int): Boolean {
        return rotation == Surface.ROTATION_0 || rotation == Surface.ROTATION_180
    }

    /**
     * 判断该小窗是否在屏幕最上层，即可以操作的层
     */
    fun isShowingFirst(freeformView: FreeformViewAbs): Boolean {
        return freeformStackSet.peek() == freeformView
    }

    fun addFreeformToSet(freeformView: FreeformViewAbs) {
        freeformStackSet.push(freeformView)
        freeformPackageSet.add("${freeformView.config.packageName}/${freeformView.config.userId}")
    }

    fun removeFreeformFromSet(freeformView: FreeformViewAbs) {
        freeformStackSet.remove(freeformView)
        freeformPackageSet.remove("${freeformView.config.packageName}/${freeformView.config.userId}")
    }

    fun addMiniFreeformToSet(freeformView: FreeformViewAbs) {
        miniFreeformStackSet.push(freeformView)
        miniFreeformPackageSet.add("${freeformView.config.packageName}/${freeformView.config.userId}")
    }

    fun removeMiniFreeformFromSet(freeformView: FreeformViewAbs) {
        miniFreeformStackSet.remove(freeformView)
        miniFreeformPackageSet.remove("${freeformView.config.packageName}/${freeformView.config.userId}")
    }

    fun getFreeformStackSet(): StackSet {
        return freeformStackSet
    }

    fun getMiniFreeformStackSet(): StackSet {
        return miniFreeformStackSet
    }

    //检查要启动的小窗是否正在小窗中运行
    fun isAppInFreeform(packageName: String, userId: Int): Boolean {
        return freeformPackageSet.contains("$packageName/$userId")
    }
    //检查要启动的小窗是否正在小窗中后台运行
    fun isAppInMiniFreeform(packageName: String, userId: Int): Boolean {
        return miniFreeformPackageSet.contains("$packageName/$userId")
    }
}