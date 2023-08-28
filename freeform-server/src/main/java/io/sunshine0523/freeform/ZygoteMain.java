package com.sunshine.freeform;

import android.os.Binder;
import android.util.Log;

import com.github.kr328.magic.services.ServiceManagerProxy;
import com.github.kr328.zloader.ZygoteLoader;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;

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
                    Log.e(TAG, name + " " + service);
                    if (name.equals(DISPLAY)) {
                        Log.i(TAG, "find display service: " + service);
                        try {
                            instanceMFService(service);
                        } catch (Exception e) {
                            Log.e(TAG, "instanceMiFreeformService failed: " + e);
                            throw new RuntimeException(e);
                        }
                    }

                    return super.addService(name, service);
                }
            });
        } catch (final Exception e) {
            Log.e(TAG, "Inject: " + e, e);
        }
    }

    private static void instanceMFService(Binder service) throws NoSuchFieldException, ClassNotFoundException, IllegalAccessException, NoSuchMethodException, InvocationTargetException, InstantiationException {
        // get out class field
        Field field = service.getClass().getDeclaredField("this$0");
        ClassLoader classLoader = service.getClass().getClassLoader();
        assert classLoader != null;
        //for find dms, we need service`s classloader
        Class<?> dmsClass = classLoader.loadClass("com.android.server.display.DisplayManagerService");

        // get DisplayManagerService
        Object displayManagerServiceObj = field.get(service);
        Field mSyncRootField = dmsClass.getDeclaredField("mSyncRoot");
        Field mContextField = dmsClass.getDeclaredField("mContext");
        Field mHandlerField = dmsClass.getDeclaredField("mHandler");
        Field mDisplayDeviceRepoField = dmsClass.getDeclaredField("mDisplayDeviceRepo");
        Field mLogicalDisplayMapperField = dmsClass.getDeclaredField("mLogicalDisplayMapper");
        Field mUiHandlerField = dmsClass.getDeclaredField("mUiHandler");
        mSyncRootField.setAccessible(true);
        mContextField.setAccessible(true);
        mHandlerField.setAccessible(true);
        mDisplayDeviceRepoField.setAccessible(true);
        mLogicalDisplayMapperField.setAccessible(true);
        mUiHandlerField.setAccessible(true);
        Object mSyncRoot = mSyncRootField.get(displayManagerServiceObj);
        Object mContext = mContextField.get(displayManagerServiceObj);
        Object mHandler = mHandlerField.get(displayManagerServiceObj);
        Object mDisplayDeviceRepo = mDisplayDeviceRepoField.get(displayManagerServiceObj);
        Object mLogicalDisplayMapper = mLogicalDisplayMapperField.get(displayManagerServiceObj);
        Object mUiHandler = mUiHandlerField.get(displayManagerServiceObj);

        //add MiFreeformServer dex to path
        classLoader.getClass().getMethod("addDexPath", String.class).invoke(classLoader, "/system/framework/freeform.dex");

        Class<?> mfdaClass = classLoader.loadClass("com.android.server.display.MiFreeformDisplayAdapter");
        Object miFreeformDisplayAdapterObj = mfdaClass.getConstructors()[0].newInstance(mSyncRoot, mContext, mHandler, mDisplayDeviceRepo, mLogicalDisplayMapper, mUiHandler);
        mfdaClass.getMethod("registerLocked").invoke(miFreeformDisplayAdapterObj);
        Log.i(TAG, "instance MiFreeformDisplayAdapter: " + miFreeformDisplayAdapterObj);

        Class<?> mfsClass = classLoader.loadClass("io.sunshine0523.freeform.service.MiFreeformService");
        Object miFreeformServiceObj = mfsClass.getConstructors()[0].newInstance(miFreeformDisplayAdapterObj);
        Log.i(TAG, "instance MiFreeformService: " + miFreeformServiceObj);

        Class<?> mfuisClass = classLoader.loadClass("io.sunshine0523.freeform.service.MiFreeformUIService");
        Object miFreeformUIServiceObj = mfuisClass.getConstructors()[0].newInstance(mContext, miFreeformDisplayAdapterObj, miFreeformServiceObj, mUiHandler);
        Log.i(TAG, "instance MiFreeformUIService: " + miFreeformUIServiceObj);

        Class<?> mfsmClass = classLoader.loadClass("io.sunshine0523.freeform.service.MiFreeformServiceHolder");
        mfsmClass.getMethod("init", mfuisClass, mfsClass).invoke(null, miFreeformUIServiceObj, miFreeformServiceObj);
    }
}
