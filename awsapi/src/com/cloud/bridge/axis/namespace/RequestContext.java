package com.cloud.bridge.axis.namespace;

public class RequestContext {
    private static ThreadLocal<RequestContext> threadRequestContext = new ThreadLocal<RequestContext>();
    private Namespace namespace;

    public static RequestContext current() {
        RequestContext context = threadRequestContext.get();

        if (context == null) {
            context = new RequestContext();
            threadRequestContext.set(context);
        }

        return context;
    }

    public Namespace getNamespace() {
        return namespace;
    }

    public void setNamespace(Namespace namespace) {
        this.namespace = namespace;
    }
}
