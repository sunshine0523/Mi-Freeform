# 米窗开放API（持续更新）

[English](https://github.com/sunshine0523/Mi-FreeForm/edit/master/open_api.md)

米窗调用接口现已对外开放，你可以选择以下方式启动米窗提供的小窗：

## 1.打开米窗提供的应用选择界面
米窗提供的打开应用选择界面的活动为：com.sunshine.freeform.ui.floating.FloatingActivity。你可以通过其他应用调用该活动以打开应用选择界面。

<b>请注意：该活动目前需要米窗的保活服务处于运行状态</b>

## 2.直接打开米窗提供的小窗界面
除上述方式外，米窗还提供广播方式接收外部应用发送的打开小窗指令。具体例子如下：

```kotlin
val packageName: String = "com.sunshine.freeform"
val activityName: String = "com.sunshine.freeform.ui.main.MainActivity"
val userId: Int = 0
val intent = Intent("com.sunshine.freeform.start_freeform").apply {
                    setPackage("com.sunshine.freeform")
                    //要启动小窗程序的包名：如com.sunshine.freeform
                    putExtra("packageName", packageName)
                    //要启动小窗的活动名称，请注意，该活动可能需要对外暴露才可启动。如com.sunshine.freeform.ui.main.MainActivity
                    putExtra("activityName", activityName)
                    //可选，默认为0。对于系统存在“应用分身”等情况，可以指定userId
                    putExtra("userId", userId)
                }
context.sendBroadcast(intent)
```
