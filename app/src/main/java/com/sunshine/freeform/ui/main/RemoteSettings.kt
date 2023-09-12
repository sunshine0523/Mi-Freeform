package com.sunshine.freeform.ui.main

import androidx.annotation.Keep

@Keep
data class RemoteSettings(
    var enableSideBar: Boolean = false,
    var showImeInFreeform: Boolean = false,
    var notification: Boolean = false
)