package com.sunshine.freeform.hook.service

import android.annotation.SuppressLint
import android.app.Application
import android.content.Context
import android.os.Binder
import android.os.IBinder
import android.view.View
import com.sunshine.freeform.hook.view.FreeFormHookWindow
import com.sunshine.freeform.hook.view.FreeFormHookWindowAbs
import com.sunshine.freeform.hook.IMiFreeFormService
import com.sunshine.freeform.hook.utils.HookFailException

/**
 * @author sunshine
 * @date 2021/7/28
 */
@SuppressLint("PrivateApi")
class MiFreeFormService : IMiFreeFormService.Stub() {

    companion object {
        private const val MI_FREEFORM_PACKAGE_NAME = "com.sunshine.freeform"

        private var mClient: IMiFreeFormService? = null

        private var mUserContext: Context? = null

        @SuppressLint("DiscouragedPrivateApi", "PrivateApi")
        fun getClient(): IMiFreeFormService? {
            if (mClient != null) return mClient
            return try {
                val getServiceMethod =
                    Class.forName("android.os.ServiceManager").getDeclaredMethod(
                        "getService",
                        String::class.java
                    )
                asInterface(
                    getServiceMethod.invoke(null, "user.mifreeform") as IBinder
                )
            } catch (e: Exception) {
                return null
            }
        }

        private fun getUserContext(): Context{
            return if (null != mUserContext) mUserContext!!
            else {
                val activityThread = Class.forName("android.app.ActivityThread")
                val currentActivityThread = activityThread.getMethod("currentActivityThread").invoke(null)
                val application = activityThread.getMethod("getApplication").invoke(currentActivityThread) as Application
                mUserContext = application.createPackageContext(MI_FREEFORM_PACKAGE_NAME, Context.CONTEXT_INCLUDE_CODE or Context.CONTEXT_IGNORE_SECURITY)
                mUserContext!!
            }
        }
    }

    override fun startWithMiFreeForm(packageName: String, command: String) {
        try {
            FreeFormHookWindow(
                getUserContext(),
                packageName,
                command,
                0
            )
        } catch (e: HookFailException) {

        }
    }

}