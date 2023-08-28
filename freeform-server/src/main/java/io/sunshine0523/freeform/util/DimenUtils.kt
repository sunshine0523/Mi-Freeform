package io.sunshine0523.freeform.util

import android.content.res.Resources
import kotlin.math.round

val Number.dp get() = round(toFloat() * Resources.getSystem().displayMetrics.density).toInt()
