package com.sunshine.freeform.ui.app_list

import android.content.ComponentName
import android.graphics.drawable.Drawable

data class AppInfo(
    val label: String,
    val icon: Drawable,
    val componentName: ComponentName,
    val userId: Int
)
