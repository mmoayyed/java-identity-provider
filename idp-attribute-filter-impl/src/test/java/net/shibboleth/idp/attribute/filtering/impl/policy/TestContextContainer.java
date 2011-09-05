package net.shibboleth.idp.attribute.filtering.impl.policy;

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

/** trivial context container to test the get something from a container.
 * We populate an incoming message context, an outgoing message context and an {@link IdPSession}
 * complete with {@link ServiceSession}
*/
class TestContextContainer extends AbstractSubcontextContainer implements InOutOperationContext {
    
    /** name used throughout the tests for the principal. */
    public static final String PRINCIPAL_NAME = "Chad";

    /** name used throughout the tests for the IdP. */
    public static final String IDP_ENTITY_ID = "http://example.com/IdP";

    /** name used throughout the tests for the SP. */
    public static final String SP_ENTITY_ID = "http://example.com/SP";

    /** name used throughout the tests for the authentication method. */
    public static final String METHOD_NAME = "http://example.com/MeThOd";

    
    final MessageContext inbound;
    final MessageContext outbound;
    
    /** constructor. */
    public TestContextContainer() {
        super();
        setAutoCreateSubcontexts(false);
        inbound = new MyMessageContext();
        
        BasicMessageMetadataSubcontext basic = new BasicMessageMetadataSubcontext(inbound);
        basic.setMessageIssuer(IDP_ENTITY_ID);

        outbound = new MyMessageContext();
        basic = new BasicMessageMetadataSubcontext(outbound);
        basic.setMessageIssuer(SP_ENTITY_ID);

        AuthenticationEvent event = new AuthenticationEvent(METHOD_NAME, new Principal() {   
            public String getName() {
                return PRINCIPAL_NAME;
            }
        });
        
        final IdPSession idpSession = new IdPSession("sessionId", new byte[2]);
        final ServiceSession serviceSession = new ServiceSession(IDP_ENTITY_ID);
        serviceSession.setAuthenticationEvent(event);
        idpSession.addServiceSession(serviceSession);
        
        new IdPSessionSubcontext(this, idpSession);
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