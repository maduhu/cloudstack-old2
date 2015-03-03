package com.cloud.bridge.axis.namespace;

import org.apache.commons.lang.StringUtils;

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

    public Namespace setNamespaceVersion(String version) {
		if (StringUtils.isNotEmpty(version)) {
			for (Namespace n : Namespace.values()) {
				if (version.equals(n.getVersion())) {
					RequestContext.current().setNamespace(n);
					return n;
				}
			}
		}
		return null;
    }
}
