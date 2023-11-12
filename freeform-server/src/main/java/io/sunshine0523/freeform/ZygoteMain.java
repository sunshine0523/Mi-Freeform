package io.sunshine0523.freeform;

import android.content.Context;
import android.os.Binder;
import android.os.Build;
import android.util.Log;
import android.view.IWindowManager;

import com.android.server.SystemServer;
import com.android.server.wm.WindowManagerService;
import com.github.kr328.magic.services.ServiceManagerProxy;
import com.github.kr328.zloader.BinderInterceptors;
import com.github.kr328.zloader.ZygoteLoader;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.util.Arrays;

import io.sunshine0523.freeform.util.MLog;

public class ZygoteMain {
    private static final String TAG = "Mi-Freeform/ZygoteMain";

    private static final String DISPLAY = "display";

    //DO NOT use main(String[] args)
    public static void main() {
        if (!ZygoteLoader.PACKAGE_SYSTEM_SERVER.equals(ZygoteLoader.getPackageName())) {
            return;
        }

        try {
            ServiceManagerProxy.install(new ServiceManagerProxy.Interceptor() {
                @Override
                public Binder addService(final String name, final Binder service) {
                    if (name.equals(DISPLAY)) {
                        MLog.i(TAG, "find display service: " + service);
                        try {
                            instanceMFService(service);
                        } catch (Exception e) {
                            MLog.e(TAG, "instanceMiFreeformService failed: " + e);
                            throw new RuntimeException(e);
                        }
                    }
                    return super.addService(name, service);
                }
            });
        } catch (final Exception e) {
            MLog.e(TAG, "Inject: " + e);
        }
    }

    private static void instanceMFService(Binder service) throws NoSuchFieldException, ClassNotFoundException, IllegalAccessException, NoSuchMethodException, InvocationTargetException, InstantiationException {
        // get out class field
        Field field = service.getClass().getDeclaredField("this$0");
        field.setAccessible(true);
        ClassLoader classLoader = service.getClass().getClassLoader();
        assert classLoader != null;
        //for find dms, we need service`s classloader
        Class<?> dmsClass = classLoader.loadClass("com.android.server.display.DisplayManagerService");

        // get DisplayManagerService
        Object displayManagerServiceObj = field.get(service);
        Field mSyncRootField = dmsClass.getDeclaredField("mSyncRoot");
        Field mContextField = dmsClass.getDeclaredField("mContext");
        Field mHandlerField = dmsClass.getDeclaredField("mHandler");
        Field mUiHandlerField = dmsClass.getDeclaredField("mUiHandler");

        mSyncRootField.setAccessible(true);
        mContextField.setAccessible(true);
        mHandlerField.setAccessible(true);
        mUiHandlerField.setAccessible(true);
        Object mSyncRoot = mSyncRootField.get(displayManagerServiceObj);
        Object mContext = mContextField.get(displayManagerServiceObj);
        Object mHandler = mHandlerField.get(displayManagerServiceObj);
        Object mUiHandler = mUiHandlerField.get(displayManagerServiceObj);

        //add MiFreeformServer dex to path
        classLoader.getClass().getMethod("addDexPath", String.class).invoke(classLoader, "/data/system/mi_freeform/freeform.dex");

        Object miFreeformDisplayAdapterObj;
        // for Android U
        if (Build.VERSION.SDK_INT == Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            Field mDisplayDeviceRepoField = dmsClass.getDeclaredField("mDisplayDeviceRepo");
            Field mLogicalDisplayMapperField = dmsClass.getDeclaredField("mLogicalDisplayMapper");
            mDisplayDeviceRepoField.setAccessible(true);
            mLogicalDisplayMapperField.setAccessible(true);
            Object mDisplayDeviceRepo = mDisplayDeviceRepoField.get(displayManagerServiceObj);
            Object mLogicalDisplayMapper = mLogicalDisplayMapperField.get(displayManagerServiceObj);
            Class<?> mfdaClass = classLoader.loadClass("com.android.server.display.MiFreeformUDisplayAdapter");
            miFreeformDisplayAdapterObj = mfdaClass.getConstructors()[0].newInstance(mSyncRoot, mContext, mHandler, mDisplayDeviceRepo, mLogicalDisplayMapper, mUiHandler);
            mfdaClass.getMethod("registerLocked").invoke(miFreeformDisplayAdapterObj);
        }
        // for Android S,T
        else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            Field mDisplayDeviceRepoField = dmsClass.getDeclaredField("mDisplayDeviceRepo");
            Field mLogicalDisplayMapperField = dmsClass.getDeclaredField("mLogicalDisplayMapper");
            mDisplayDeviceRepoField.setAccessible(true);
            mLogicalDisplayMapperField.setAccessible(true);
            Object mDisplayDeviceRepo = mDisplayDeviceRepoField.get(displayManagerServiceObj);
            Object mLogicalDisplayMapper = mLogicalDisplayMapperField.get(displayManagerServiceObj);
            Class<?> mfdaClass = classLoader.loadClass("com.android.server.display.MiFreeformTDisplayAdapter");
            miFreeformDisplayAdapterObj = mfdaClass.getConstructors()[0].newInstance(mSyncRoot, mContext, mHandler, mDisplayDeviceRepo, mLogicalDisplayMapper, mUiHandler);
            mfdaClass.getMethod("registerLocked").invoke(miFreeformDisplayAdapterObj);
        }
        // for Android O,P,Q,R
        else {
            Field mListenerField = dmsClass.getDeclaredField("mDisplayAdapterListener");
            Field mLogicalDisplaysField = dmsClass.getDeclaredField("mLogicalDisplays");
            mListenerField.setAccessible(true);
            mLogicalDisplaysField.setAccessible(true);
            Object mListener = mListenerField.get(displayManagerServiceObj);
            Object mLogicalDisplays = mLogicalDisplaysField.get(displayManagerServiceObj);
            Class<?> mfdaClass = classLoader.loadClass("com.android.server.display.MiFreeformRDisplayAdapter");
            miFreeformDisplayAdapterObj = mfdaClass.getConstructors()[0].newInstance(mSyncRoot, mContext, mHandler, mListener, mLogicalDisplays, mUiHandler);
            mfdaClass.getMethod("registerLocked").invoke(miFreeformDisplayAdapterObj);
        }
        MLog.i(TAG, "instance MiFreeformDisplayAdapter: " + miFreeformDisplayAdapterObj);

        Class<?> mfsClass = classLoader.loadClass("io.sunshine0523.freeform.service.MiFreeformService");
        Object miFreeformServiceObj = mfsClass.getConstructors()[0].newInstance(miFreeformDisplayAdapterObj);
        MLog.i(TAG, "instance MiFreeformService: " + miFreeformServiceObj);

        Class<?> mfuisClass = classLoader.loadClass("io.sunshine0523.freeform.service.MiFreeformUIService");
        Object miFreeformUIServiceObj = mfuisClass.getConstructors()[0].newInstance(mContext, miFreeformDisplayAdapterObj, miFreeformServiceObj, mUiHandler, mHandler);
        MLog.i(TAG, "instance MiFreeformUIService: " + miFreeformUIServiceObj);

        Class<?> mfsmClass = classLoader.loadClass("io.sunshine0523.freeform.service.MiFreeformServiceHolder");
        mfsmClass.getMethod("init", mfuisClass, mfsClass).invoke(null, miFreeformUIServiceObj, miFreeformServiceObj);
    }
}
