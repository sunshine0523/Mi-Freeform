package com.sunshine.freeform.view.floating

import com.sunshine.freeform.utils.StackSet
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlin.collections.HashSet

/**
 * @author sunshine
 * @date 2022/1/6
 */
@DelicateCoroutinesApi
object FreeFormHelper {
    private const val TAG = "FreeFormHelper"

    //小窗的顺序
    var displayIdStackSet = StackSet<Int>()

    //所有小窗的集合，用于屏幕旋转时监听
    var freeFormViewSet = HashSet<FreeFormView>()

    /**
     * 显示的小窗中是否存在要打开的小窗，存在就不允许打开了
     */
    fun hasFreeFormWindow(command: String): Boolean {
        freeFormViewSet.forEach {
            if (command == it.command) return true
        }
        return false
    }

    fun onOrientationChanged(){
        freeFormViewSet.forEach {
            it.onOrientationChanged()
        }
    }
}