package com.sunshine.freeform.ui.freeform

data class FreeformConfig(
    //是否使用自定义配置而非在FreeformView中配置
    var useCustomConfig: Boolean = false,
    //运行程序包名
    var packageName: String,
    //运行程序活动名
    var activityName: String,
    //启动的userId
    var userId: Int,
    //叠加层最大高度
    var maxHeight: Int = 1,
    //分辨率
    var freeformDpi: Int = 1,
    //宽高比，默认9：16
    var widthHeightRatio: Float = 10f / 16f,
    //使用shizuku/sui阻止小窗跳出到全屏
    var useSuiRefuseToFullScreen: Boolean = false,
    //屏幕旋转时变化缩放比例
    var changeDpi: Boolean = false,
    //兼容模式启动
    @Deprecated("", ReplaceWith(""))
    var compatibleMode: Boolean = false,
    var rememberPosition: Boolean = false,
    //记录启动位置
    var rememberX: Int = 0,
    var rememberY: Int = 0,
    //手动调整小窗方向
    var manualAdjustFreeformRotation: Boolean = false,
    //自由比例缩放
    var autoScale: Boolean = false
) {

}