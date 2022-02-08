package com.sunshine.freeform.callback

/**
 * @author sunshine
 * @date 2022/1/27
 */
interface ServiceDataListener {
    fun onDataChanged(key: String, newValue: Any)
}