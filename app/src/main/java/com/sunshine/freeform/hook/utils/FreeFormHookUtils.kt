package com.sunshine.freeform.hook.utils

import android.content.res.Configuration
import com.sunshine.freeform.hook.view.FreeFormHookWindowAbs


/**
 * @author sunshine
 * @date 2021/8/1
 */
object FreeFormHookUtils {

    //小窗的顺序
    var displayIdStackSet = StackHookSet<Int>()

    var dpi = 300   //小窗默认分辨率为300dpi，可以自定义

    //屏幕方向，1 竖屏 2横屏 0未定义
    var orientation = Configuration.ORIENTATION_PORTRAIT

    //所有小窗的集合，用于屏幕旋转时监听
    var freeFormViewSet = HashSet<FreeFormHookWindowAbs>()
}