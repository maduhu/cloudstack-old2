package com.cloud.utils.threading;


public class SharedLock {
    private static LockMap<String> locks = new LockMap<String>();
    
    public static LockMap<String> getInstance() {
        return locks;
    }
    
    public static Object get(String lock) {
        return locks.get(lock);
    }
}
