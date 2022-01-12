package com.sunshine.freeform.systemapi;

import android.annotation.SuppressLint;
import android.os.IBinder;
import android.os.IInterface;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

@SuppressLint("PrivateApi")
public final class ServiceManager {
    private Method getServiceMethod;

    private InputManager inputManager;

    private WindowManager windowManager;

    private DisplayManager displayManager;

    @SuppressLint("DiscouragedPrivateApi")
    public ServiceManager() {
        try {
            getServiceMethod = Class.forName("android.os.ServiceManager").getDeclaredMethod("getService", String.class);
        } catch (Exception e) {
            System.out.println("ServerManager " + e);
            getServiceMethod = null;
        }
    }

    private IInterface getService(String service, String type) {
        try {
            IBinder binder = (IBinder) getServiceMethod.invoke(null, service);
            Method asInterfaceMethod = Class.forName(type + "$Stub").getMethod("asInterface", IBinder.class);
            return (IInterface) asInterfaceMethod.invoke(null, binder);
        } catch (Exception e) {
            System.out.println("getServer " + e);
            return null;
        }
    }

    public void addService(String name, IBinder binder) {
        try {
            Method addServiceMethod = Class.forName("android.os.ServiceManager").getMethod("AddService", String.class, IBinder.class);
            addServiceMethod.invoke(null, name, binder);
            System.out.println("success");
        } catch (NoSuchMethodException | ClassNotFoundException | IllegalAccessException | InvocationTargetException e) {
            System.out.println(e);
            e.printStackTrace();
        }
    }

    public InputManager getInputManager() {
        if (inputManager == null) {
            inputManager = new InputManager(getService("input", "android.hardware.input.IInputManager"));
        }
        return inputManager;
    }

    public WindowManager getWindowManager() {
        if (windowManager == null) {
            windowManager = new WindowManager(getService("window", "android.view.IWindowManager"));
        }
        return windowManager;
    }

    public DisplayManager getDisplayManager() {
        if (displayManager == null) {
            displayManager = new DisplayManager(getService("display", "android.hardware.display.IDisplayManager"));
        }
        return displayManager;
    }
}
