package com.cloud.utils.threading;

import java.lang.ref.WeakReference;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

public class TaskManager {
    private static final Map<String, WeakReference<ExecutorService>> pools =
            new WeakHashMap<String, WeakReference<ExecutorService>>();

    public static <T> Future<T> submit(final String token, final Callable<T> task) {
        ExecutorService pool = getPool(token);        
        return pool.submit(task);
    }
    
    private static ExecutorService getPool(String token) {
        synchronized (pools) {
            WeakReference<ExecutorService> svc = pools.get(token);
            
            if (svc == null || svc.get() == null) {
                svc = new WeakReference<ExecutorService>(Executors.newSingleThreadExecutor());
                pools.put(token, svc);
            }
            
            return svc.get();
        }
    }
}
