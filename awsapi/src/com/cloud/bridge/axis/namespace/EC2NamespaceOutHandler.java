package com.cloud.bridge.axis.namespace;

import org.apache.axis2.AxisFault;
import org.apache.axis2.context.MessageContext;
import org.apache.axis2.engine.Handler;

public class EC2NamespaceOutHandler extends EC2NamespaceHandler implements Handler {

    protected final String name = "EC2NamespaceOutHandler";

    @Override
    public InvocationResponse invoke(MessageContext msgContext) throws AxisFault {
        if (RequestContext.current().getNamespace() != Namespace.getCurrent()) {
            substituteNamespace(msgContext, Namespace.getCurrent(), RequestContext.current().getNamespace());
        }

        return InvocationResponse.CONTINUE;
    }
}
