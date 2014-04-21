
package net.shibboleth.idp.attribute.resolver.ad.impl;

import org.joda.time.DateTime;
import org.opensaml.messaging.context.BasicMessageMetadataContext;
import org.opensaml.messaging.context.InOutOperationContext;
import org.opensaml.messaging.context.MessageContext;

/** trivial context container to test the get something from a container. */
class TestContextContainer extends InOutOperationContext {

    final MessageContext inbound;

    /** constructor. */
    public TestContextContainer(final String relyingParty, final String principalName, final String authnMethod) {
        super();
        setAutoCreateSubcontexts(false);
        inbound = new MyMessageContext();

        final BasicMessageMetadataContext basic = new BasicMessageMetadataContext();
        basic.setMessageIssuer(relyingParty);
    }

    public TestContextContainer() {
        super();
        inbound = null;
        setAutoCreateSubcontexts(false);
    }

    /** Not used. */
    @Override public DateTime getCreationTime() {
        return null;
    }

    public String getId() {
        return "TestContainerContextid";
    }

    /** {@inheritDoc} */
    @Override public MessageContext getInboundMessageContext() {
        return inbound;
    }

    /** Not used. */
    @Override public MessageContext getOutboundMessageContext() {
        return null;
    }

    private class MyMessageContext extends MessageContext {

        /** Not used. */
        @Override public DateTime getCreationTime() {
            return null;
        }

        /** Not used. */
        @Override public Object getMessage() {
            return null;
        }

        /** Not used. */
        @Override public void setMessage(Object message) {
        }

    }
}