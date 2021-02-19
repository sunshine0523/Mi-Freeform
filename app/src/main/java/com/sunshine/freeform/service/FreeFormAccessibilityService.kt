package com.sunshine.freeform.service

import android.accessibilityservice.AccessibilityService
import android.content.res.Configuration
import android.graphics.Rect
import android.view.accessibility.AccessibilityEvent
import com.sunshine.freeform.service.floating.FloatingService
import com.sunshine.freeform.service.floating.FreeFormConfig
import com.sunshine.freeform.service.floating.FreeFormMediaCodecView
import com.sunshine.freeform.utils.PermissionUtils
import com.sunshine.freeform.utils.ServiceUtils
import java.util.concurrent.TimeUnit

/**
 * 无障碍去监听屏幕方向的变化
 * 因为后台是无法监听的，所以想到了用boundsInScreen的方式来监听
 */
class FreeFormAccessibilityService : AccessibilityService() {

    //布局，用于获取宽高，判断屏幕方向
    private var rect = Rect()
    //屏幕方向
    private var orientation = Configuration.ORIENTATION_UNDEFINED

    /**
     * 监听服务是否开启
     */
    override fun onServiceConnected() {
        super.onServiceConnected()
        if (PermissionUtils.accessibilityStateListener != null) {
            PermissionUtils.accessibilityStateListener!!.onStart()
        }
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        //只有服务开启时才需要监听
        if (FloatingService.orientationChangeListener != null && rootInActiveWindow != null) {
            try {
                rootInActiveWindow.getBoundsInScreen(rect)
                //rect.bottom相当于y，rect.right相当于x，如果y>=x，认为是竖屏，否则就是横屏
                val o = if (rect.bottom >= rect.right) Configuration.ORIENTATION_PORTRAIT else Configuration.ORIENTATION_LANDSCAPE
                //相当于屏幕方向发生了变化
                if (o != orientation) {
                    FreeFormConfig.orientation = o
                    FloatingService.orientation = o
                    //如果是undefined说明是第一次或者重启无障碍，如果通知改变的话，会导致当前开启的小窗重启或关闭
                    if (orientation != Configuration.ORIENTATION_UNDEFINED) FloatingService.orientationChangeListener?.onChanged()
                    orientation = o
                }
            } catch (e: Exception) {
                //出错一般是屏幕旋转时界面还在更新，那么就直接判断旋转了
//                orientation = if (orientation == 1) 2 else 1
//                FreeFormConfig.orientation = orientation
//                FloatingService.orientation = orientation
//                FloatingService.orientationChangeListener?.onChanged()
                println("onAccessibilityEvent $e")
            }
        }

    }

    override fun onInterrupt() {

    }

//    private fun dfsnode(node: AccessibilityNodeInfo, num: Int) {
//        val stringBuilder = StringBuilder()
//        for (i in 0 until num) {
//            stringBuilder.append("__ ") //父子节点之间的缩进
//        }
//        Log.i("####", stringBuilder.toString() + node.toString()) //打印
//        for (i in 0 until node.childCount) { //遍历子节点
//            if (node.getChild(i) != null)
//                dfsnode(node.getChild(i), num + 1)
//        }
//    }

    /**
     * 监听服务是否关闭
     */
    override fun onDestroy() {
        super.onDestroy()
        if (PermissionUtils.accessibilityStateListener != null) {
            PermissionUtils.accessibilityStateListener!!.onStop()
        }
    }
}