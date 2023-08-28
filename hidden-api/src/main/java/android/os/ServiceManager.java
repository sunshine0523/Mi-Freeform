package android.os;

import java.util.Map;

public class ServiceManager {

    /**
     * Returns a reference to a service with the given name.
     *
     * @param name the name of the service to get
     * @return a reference to the service, or <code>null</code> if the service doesn't exist
     */
    public static IBinder getService(String name) {
        throw new RuntimeException("Stub!");
    }

    /**
     * Place a new @a service called @a name into the service
     * manager.
     *
     * @param name the name of the new service
     * @param service the service object
     */
    public static void addService(String name, IBinder service) {
        throw new RuntimeException("Stub!");
    }

    /**
     * This is only intended to be called when the process is first being brought
     * up and bound by the activity manager. There is only one thread in the process
     * at that time, so no locking is done.
     *
     * @param cache the cache of service references
     * @hide
     */
    public static void initServiceCache(Map<String, IBinder> cache) {
        throw new RuntimeException("Stub!");
    }
}
