package com.cloud.exception;

public class RouterUnavailableException extends ResourceUnavailableException {
    public RouterUnavailableException(String msg, Class<?> scope, long resourceId) {
        this(msg, scope, resourceId, null);
    }

    public RouterUnavailableException(String msg, Class<?> scope, long resourceId, Throwable cause) {
        super(msg, scope, resourceId, cause);
    }
    
    public RouterUnavailableException(String msg, Class<?> scope, long resourceId, boolean abort, Throwable cause) {
        super(msg, scope, resourceId, abort, cause);
    }
}
