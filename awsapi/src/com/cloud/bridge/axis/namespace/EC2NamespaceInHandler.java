package com.cloud.bridge.axis.namespace;

import org.apache.axis2.AxisFault;
import org.apache.axis2.context.MessageContext;
import org.apache.axis2.engine.Handler;

/**
 * An Axis2 handler to be executed as an InFlow phase before general dispatching,
 * that saves the current request namespace and substitutes with the one used when
 * auto-generating the Axis classes from WSDL.
 */
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
