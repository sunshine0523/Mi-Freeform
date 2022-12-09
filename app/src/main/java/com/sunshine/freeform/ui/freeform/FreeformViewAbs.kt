package com.sunshine.freeform.ui.freeform

abstract class FreeformViewAbs(open val config: FreeformConfig) {
    abstract fun toScreenCenter()
    abstract fun moveToFirst()
    abstract fun fromBackstage()
}