# Mi-Freeform open API (continuously updated)



The calling interface of Mi-Freeform is now open to the public. You can select the following methods to start the freeform provided by Mi-Freeform:



## 1. Open the application selection interface provided by Mi-Freeform

The activity provided by the Mi-Freeform to open the application selection interface is: com.sunshine.freeform.ui.floating.floatingactivity. You can call this activity through other applications to open the application selection interface.



<b>Please note: this activity currently requires the maintenance service of Mi-Freeform to be in operation</b>



## 2. Directly open the small window interface provided by meter window

In addition to the above methods, Mi-Freeform also provides a broadcast mode to receive the instruction to open a freeform sent by an external application. Specific examples are as follows:



```kotlin

val packageName: String = "com.sunshine.freeform"

val activityName: String = "com.sunshine.freeform.ui.main.MainActivity"

val userId: Int = 0

val intent = Intent("com.sunshine.freeform.start_freeform"). apply {

setPackage("com.sunshine.freeform")

//Package name to start the freeform: for example, com.sunshine.freeform

putExtra("packageName", packageName)

//The name of the activity to start the freeform. Please note that the activity may need to be exposed to the public before it can be started. For example, com.sunshine.freeform.ui.main.mainactivity

putExtra("activityName", activityName)

//Optional. It is 0 by default. For the case that the system has "application separation", you can specify userid

putExtra("userId", userId)

}

context.sendBroadcast(intent)

```
