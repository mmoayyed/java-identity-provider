package net.shibboleth.idp.attribute.resolver.impl;

import java.security.Principal;

import net.shibboleth.idp.session.AuthenticationEvent;
import net.shibboleth.idp.session.IdPSession;
import net.shibboleth.idp.session.IdPSessionSubcontext;
import net.shibboleth.idp.session.ServiceSession;

import org.joda.time.DateTime;
import org.opensaml.messaging.context.AbstractSubcontextContainer;
import org.opensaml.messaging.context.BasicMessageMetadataSubcontext;
import org.opensaml.messaging.context.InOutOperationContext;
import org.opensaml.messaging.context.MessageContext;

/** trivial context container to test the get something from a container. */
class TestContextContainer extends AbstractSubcontextContainer implements InOutOperationContext {
    
    final MessageContext inbound;
    
    /** constructor. */
    public TestContextContainer(final String relyingParty, final String principalName, final String authnMethod) {
        super();
        setAutoCreateSubcontexts(false);
        inbound = new MyMessageContext();
        
        final BasicMessageMetadataSubcontext basic = new BasicMessageMetadataSubcontext(inbound);
        basic.setMessageIssuer(relyingParty);
        
        AuthenticationEvent event = new AuthenticationEvent(authnMethod, new Principal() {   
            public String getName() {
                return principalName;
            }
        });
        
        final IdPSession idpSession = new IdPSession("sessionId", new byte[2]);
        final ServiceSession serviceSession = new ServiceSession(relyingParty);
        serviceSession.setAuthenticationEvent(event);
        idpSession.addServiceSession(serviceSession);
        
        new IdPSessionSubcontext(this, idpSession);
    }

    public TestContextContainer() {
        super();
        inbound = null;
        setAutoCreateSubcontexts(false);
    }

    /** Not used. */
    public DateTime getCreationTime() {
        return null;
    }

    /** Not used. */
    public String getId() {
        return null;
    }

    /** {@inheritDoc} */
    public MessageContext getInboundMessageContext() {
        return inbound;
    }

    /** Not used. */
    public MessageContext getOutboundMessageContext() {
        return null;
    }
    
    
    private class MyMessageContext extends AbstractSubcontextContainer implements MessageContext {

        /** Not used. */
        public DateTime getCreationTime() {
            return null;
        }

        /** Not used. */
        public String getId() {
            return null;
        }

        /** Not used. */
        public Object getMessage() {
            return null;
        }

        /** Not used. */
        public void setMessage(Object message) {
        }
        
    }
}