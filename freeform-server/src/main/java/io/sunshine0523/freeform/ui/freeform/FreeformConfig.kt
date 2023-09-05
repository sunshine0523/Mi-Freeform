package io.sunshine0523.freeform.ui.freeform

data class FreeformConfig(
    var width: Int,
    var height: Int,
    var densityDpi: Int,
    var secure: Boolean = true,
    var ownContentOnly: Boolean = true,
    var shouldShowSystemDecorations: Boolean = true,
    var refreshRate: Float,
    var hangUpWidth: Int = 300,
    var hangUpHeight: Int = 400,
    var isHangUp: Boolean = false,
    // 记录挂起前的位置，以便恢复
    var notInHangUpX: Int = 0,
    var notInHangUpY: Int = 0,
    //小窗屏幕宽高，与view的比例
    var freeformWidth: Int,
    var freeformHeight: Int,
    //小窗屏幕尺寸/小窗界面尺寸
    var scale: Float
) {
    constructor(width: Int, height: Int, densityDpi: Int, secure: Boolean, ownContentOnly: Boolean, shouldShowSystemDecorations: Boolean, refreshRate: Float) : this(width, height, densityDpi, secure, ownContentOnly, shouldShowSystemDecorations, refreshRate, 300, 400, false, 0, 0, 1080, 1920, 1.0f) {

    }
}