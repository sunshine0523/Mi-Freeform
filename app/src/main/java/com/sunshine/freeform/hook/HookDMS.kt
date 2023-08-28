//package com.sunshine.freeform.hook
//
//import android.content.Context
//import android.util.Log
//import com.android.server.display.MiFreeformDisplayAdapter
//import com.sunshine.freeform.hook.utils.XLog
//import de.robv.android.xposed.IXposedHookLoadPackage
//import de.robv.android.xposed.XC_MethodHook
//import de.robv.android.xposed.XposedHelpers
//import de.robv.android.xposed.callbacks.XC_LoadPackage
//import com.sunshine.freeform_server.MiFreeformService
//
//class HookDMS : IXposedHookLoadPackage {
//
//    private fun hookDMS(classLoader: ClassLoader) {
//        val dms = XposedHelpers.findClass("com.android.server.display.DisplayManagerService", classLoader)
//        XLog.d("find DisplayManagerService Class: $dms")
//        XposedHelpers.findAndHookConstructor(
//            dms,
//            Context::class.java,
//            object : XC_MethodHook() {
//                override fun afterHookedMethod(param: MethodHookParam?) {
//                    val dmsObj = param!!.thisObject
//
//                    val mSyncRoot = XposedHelpers.getObjectField(dmsObj, "mSyncRoot")
//                    val mContext = XposedHelpers.getObjectField(dmsObj, "mContext")
//                    val mHandler = XposedHelpers.getObjectField(dmsObj, "mHandler")
//                    val mDisplayDeviceRepo = XposedHelpers.getObjectField(dmsObj, "mDisplayDeviceRepo")
//                    val mUiHandler = XposedHelpers.getObjectField(dmsObj, "mUiHandler")
//
//                    XLog.d("DMS object: $dmsObj")
//                    initMiFreeformService(mSyncRoot, mContext, mHandler, mDisplayDeviceRepo, mUiHandler)
//                }
//            }
//        )
//    }
//
//    private fun initMiFreeformService(
//        mSyncRoot: Any,
//        mContext: Any,
//        mHandler: Any,
//        mDisplayDeviceRepo: Any,
//        mUiHandler: Any
//    ) {
//        val miFreeformService = MiFreeformService()
//        XLog.d("Mi-Freeform Service: $miFreeformService")
//
//        val adapter =
//            MiFreeformDisplayAdapter::class.java.constructors[0].newInstance(
//                mSyncRoot,
//                mContext,
//                mHandler,
//                mDisplayDeviceRepo,
//                mUiHandler
//            ) as MiFreeformDisplayAdapter
//        XLog.e("MiFreeformDisplayAdapter $adapter")
//    }
//
//    override fun handleLoadPackage(param: XC_LoadPackage.LoadPackageParam) {
//        if (param.packageName == "android") {
//            hookDMS(param.classLoader)
//        }
//    }
//}