package com.sunshine.freeform

import java.io.Serializable

/**
 * @author sunshine
 * @date 2021/2/14
 */
data class EventData(
    val action: Int,
    val xArray: FloatArray,
    val yArray: FloatArray,
    val deviceId: Int,
    val source: Int,
    val flags: Int,
    val displayId: Int
) : Serializable {

}