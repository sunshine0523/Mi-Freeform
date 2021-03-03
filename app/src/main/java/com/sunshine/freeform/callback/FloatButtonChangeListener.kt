package com.sunshine.freeform.callback

/**
 * @author sunshine
 * @date 2021/2/24
 * 监听悬浮按钮大小或者透明度变化，为了设置大小时能及时更新
 */
interface FloatButtonChangeListener {
    fun onChanged(size: Int)
}