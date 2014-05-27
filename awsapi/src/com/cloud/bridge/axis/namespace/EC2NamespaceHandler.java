package com.cloud.bridge.axis.namespace;

import java.io.ByteArrayOutputStream;
import java.util.Iterator;

import javax.xml.stream.XMLStreamException;

import org.apache.axiom.om.OMAbstractFactory;
import org.apache.axiom.om.OMNode;
import org.apache.axiom.om.impl.llom.util.AXIOMUtil;
import org.apache.axiom.soap.SOAPEnvelope;
import org.apache.axis2.AxisFault;
import org.apache.axis2.context.MessageContext;
import org.apache.axis2.description.HandlerDescription;
import org.apache.axis2.description.Parameter;
import org.apache.axis2.engine.Handler;
import org.apache.log4j.Logger;

public abstract class EC2NamespaceHandler implements Handler {
    protected final static Logger logger = Logger.getLogger(EC2NamespaceHandler.class);

    protected final String name = "EC2NamespaceHandler";
    private HandlerDescription handlerDesc = new HandlerDescription(name);

    @Override
    public void cleanup() {}

    @Override
    public HandlerDescription getHandlerDesc() {
        return handlerDesc;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public Parameter getParameter(String name) {
        return null;
    }

    @Override
    public void init(HandlerDescription handlerDesc) {
        this.handlerDesc = handlerDesc;
    }

    @Override
    public abstract InvocationResponse invoke(MessageContext msgContext) throws AxisFault;

    @Override
    public void flowComplete(MessageContext msgContext) {}

    @SuppressWarnings("unchecked")
    protected void substituteNamespace(MessageContext msgContext, Namespace oldNamespace, Namespace newNamespace) {
        try {
            SOAPEnvelope envelope = OMAbstractFactory.getSOAP12Factory().getDefaultEnvelope();
            Iterator<OMNode> i =  msgContext.getEnvelope().getBody().getChildren();

            while (i.hasNext()) {
                OMNode node = i.next();
                String xml = serializeOMNode(node).replace(oldNamespace.getUrl(), newNamespace.getUrl());;
                envelope.getBody().addChild(AXIOMUtil.stringToOM(xml));
            }

            logger.trace("Original SOAP Envelope: " + serializeOMNode(msgContext.getEnvelope()));
            logger.trace("New SOAP Envelope: " + serializeOMNode(envelope));

            msgContext.setEnvelope(envelope);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @SuppressWarnings("unchecked")
    protected Namespace registerNamespace(MessageContext msgContext) {
        for (Namespace n : Namespace.values()) {
            Iterator<OMNode> i = msgContext.getEnvelope().getBody().getChildren();
            while (i.hasNext()) {
                String xml = serializeOMNode(i.next());
                if (xml.contains(n.getUrl())) {
                    RequestContext.current().setNamespace(n);
                    return n;
                }
            }
        }

        RequestContext.current().setNamespace(Namespace.values()[0]);
        return Namespace.getCurrent();
    }

    private String serializeOMNode(OMNode node) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();

        try {
            node.serialize(out);
        } catch (XMLStreamException e) {
            e.printStackTrace();
        }

        return out.toString();
    }
}
