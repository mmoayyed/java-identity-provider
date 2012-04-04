package net.shibboleth.idp.attribute.resolver.impl.ad;

import java.security.Principal;

import net.shibboleth.idp.session.AuthenticationEvent;
import net.shibboleth.idp.session.IdPSession;
import net.shibboleth.idp.session.IdPSessionContext;
import net.shibboleth.idp.session.ServiceSession;

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
        
        AuthenticationEvent event = new AuthenticationEvent(authnMethod, new Principal() {   
            public String getName() {
                return principalName;
            }
        });
        
        final IdPSession idpSession = new IdPSession("sessionId", new byte[2]);
        final ServiceSession serviceSession = new ServiceSession("serviceSession", event);
        idpSession.addServiceSession(serviceSession);
        
        new IdPSessionContext(idpSession);
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
    
    
    private class MyMessageContext extends MessageContext {

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