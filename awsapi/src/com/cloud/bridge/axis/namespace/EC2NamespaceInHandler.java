package com.cloud.bridge.axis.namespace;

import org.apache.axis2.AxisFault;
import org.apache.axis2.context.MessageContext;
import org.apache.axis2.engine.Handler;

public class EC2NamespaceInHandler extends EC2NamespaceHandler implements Handler {

    protected final String name = "EC2NamespaceInHandler";

    @Override
    public InvocationResponse invoke(MessageContext msgContext) throws AxisFault {
        Namespace currentNamespace = registerNamespace(msgContext);

        if (currentNamespace != Namespace.getCurrent()) {
            substituteNamespace(msgContext, currentNamespace, Namespace.getCurrent());
        }

        return InvocationResponse.CONTINUE;
    }
}
