package com.sunshine.freeform.ui

import android.annotation.SuppressLint
import android.content.ComponentName
import android.content.Context
import android.content.ServiceConnection
import android.hardware.display.DisplayManager
import android.os.IBinder
import android.util.AttributeSet
import android.util.Log
import android.view.Surface
import android.view.SurfaceHolder
import android.view.SurfaceView
import org.lsposed.hiddenapibypass.HiddenApiBypass
import java.util.concurrent.atomic.AtomicBoolean

/**
 * 继承了 SurfaceView，提供跨进程渲染的 Surface。
 */
@SuppressLint("ClickableViewAccessibility")
class ProcessSurfaceView : SurfaceView, SurfaceHolder.Callback, ServiceConnection {
    private var surface: Surface? = null
    //private var iRemoteDraw: IRemoteDraw? = null
    private val isSetSurface = AtomicBoolean(false)

    private val displayManager: DisplayManager

    constructor(context: Context): super(context)
    constructor(context: Context, attrs: AttributeSet) : super(context, attrs)
    constructor(context: Context, attrs: AttributeSet, defStyle: Int) : super(context, attrs, defStyle)

    init {
        holder.addCallback(this@ProcessSurfaceView)
        holder.setFixedSize(480, 720)
        displayManager = context.getSystemService(Context.DISPLAY_SERVICE) as DisplayManager

        setOnTouchListener { _, motionEvent ->
            Log.i(TAG, "$motionEvent")
            true
        }
    }

    override fun surfaceCreated(p0: SurfaceHolder) {
        surface = p0.surface
        setSurfaceToRemote()
        Log.i(TAG, "surfaceCreated")
    }

    override fun surfaceChanged(p0: SurfaceHolder, p1: Int, p2: Int, p3: Int) {
        Log.i(TAG, "surfaceChanged")
    }

    override fun surfaceDestroyed(p0: SurfaceHolder) {
        Log.i(TAG, "surfaceDestroyed")
        isSetSurface.set(false)
    }

    override fun onServiceConnected(p0: ComponentName?, iBinder: IBinder?) {
        Log.i(TAG, "onServiceConnected")
        if (iBinder == null) {
            Log.e(TAG, "onServiceDisconnected: iBinder is null.")
            return
        }
        //iRemoteDraw = IRemoteDraw.Stub.asInterface(iBinder)
        setSurfaceToRemote()
    }

    override fun onServiceDisconnected(p0: ComponentName?) {
        Log.e(TAG, "onServiceDisconnected.")
    }




//    private fun bindService() {
//        val intent = Intent(context, RemoteDrawService::class.java)
//        context.bindService(intent, this@ProcessSurfaceView, Context.BIND_AUTO_CREATE or Context.BIND_IMPORTANT)
//    }

    private fun setSurfaceToRemote() {
        if (isSetSurface.get()) {
            Log.i(TAG, "setSurfaceToRemote: has set surface.")
            return
        }
        if (surface != null) {
            val serviceManager = Class.forName("android.os.ServiceManager")
            val r = HiddenApiBypass.invoke(serviceManager, null, "getService", "mi_freeform") as IBinder
            Log.e(TAG, "mfd $r")
            //val s = IMiFreeformService.Stub.asInterface(r)
//            val callback = object : IMiFreeformDisplayCallback.Stub() {
//                override fun onPaused() {
//
//                }
//
//                override fun onResumed() {
//
//                }
//
//                override fun onStopped() {
//
//                }
//
//                override fun onDisplayAdd(displayId: Int) {
//                    Log.i(TAG, "displayId $displayId")
//                }
//            }
//            s.createFreeform(
//                    "test3",
//                    callback,
//                    1080,
//                    1920,
//                    330,
//                    true,
//                    false,
//                    true,
//                    surface,
//                    120.0f,
//                    21666666L
//                )
//            //displayManager.createVirtualDisplay("name", 480, 720, 160, surface, DisplayManager.VIRTUAL_DISPLAY_FLAG_PRESENTATION)
//            isSetSurface.set(true)
        }
//        if (iRemoteDraw != null && surface != null) {
//            iRemoteDraw!!.setSurface(surface!!)
//            isSetSurface.set(true)
//        }
    }

    companion object {
        private const val TAG = "ProcessSurfaceView"
    }

}