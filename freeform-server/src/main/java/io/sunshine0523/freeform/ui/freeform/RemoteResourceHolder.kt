package io.sunshine0523.freeform.ui.freeform

import android.annotation.SuppressLint
import android.content.Context
import android.content.Context.CONTEXT_IGNORE_SECURITY
import android.content.Context.CONTEXT_INCLUDE_CODE
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import io.sunshine0523.freeform.util.MLog


@SuppressLint("WrongConstant", "DiscouragedApi")
class RemoteResourceHolder(context: Context, private val resPkg: String) {

    private lateinit var remoteContext: Context

    companion object {
        private const val TAG = "Mi-Freeform/RemoteResourceHolder"
    }

    init {
        try {
            remoteContext = context.createPackageContext(resPkg, CONTEXT_INCLUDE_CODE or CONTEXT_IGNORE_SECURITY)
        } catch (e: Exception) {
            e.printStackTrace()
            MLog.e(TAG, "init", e)
            runCatching { remoteContext = context.createPackageContext(resPkg, CONTEXT_INCLUDE_CODE or CONTEXT_IGNORE_SECURITY) }
        }
    }

    fun getLayout(layoutName: String): ViewGroup? {
        return try {
            val freeformLayoutId = remoteContext.resources.getIdentifier(layoutName, "layout", resPkg)
                val r = LayoutInflater.from(remoteContext).inflate(freeformLayoutId, null, false)
            if (null == r) Log.e(TAG, "can not find layout $layoutName")
            r as ViewGroup
        } catch (e: Exception) {
            e.printStackTrace()
            MLog.e(TAG, "getLayout", e)
            null
        }
    }

    fun <T : View> getLayoutChildViewByTag(layout: ViewGroup, tagName: String): T? {
        return try {
            val r = layout.findViewWithTag<T>(tagName)
            if (null == r) MLog.e(TAG, "can not find tag $tagName in $layout")
            r
        } catch (e: Exception) {
            e.printStackTrace()
            MLog.e(TAG, "getLayoutChildViewByTag", e)
            null
        }
    }
}