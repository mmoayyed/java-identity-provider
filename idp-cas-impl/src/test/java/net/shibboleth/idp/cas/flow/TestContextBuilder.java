package net.shibboleth.idp.cas.flow;

import net.shibboleth.idp.cas.protocol.ProtocolContext;
import net.shibboleth.idp.cas.service.Service;
import net.shibboleth.idp.cas.service.ServiceContext;
import net.shibboleth.idp.cas.ticket.Ticket;
import net.shibboleth.idp.cas.ticket.TicketContext;
import net.shibboleth.idp.profile.config.ProfileConfiguration;
import net.shibboleth.idp.profile.context.RelyingPartyContext;
import net.shibboleth.idp.session.IdPSession;
import net.shibboleth.idp.session.context.SessionContext;
import org.opensaml.profile.context.ProfileRequestContext;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.webflow.execution.RequestContext;
import org.springframework.webflow.test.MockExternalContext;
import org.springframework.webflow.test.MockRequestContext;

/**
 * Helper component to build the Webflow {@link RequestContext} needed for flow action tests.
 *
 * @author Marvin S. Addison
 */
public class TestContextBuilder {

    /** Root of context tree for our tests. */
    private ProfileRequestContext profileRequestContext = new ProfileRequestContext();

    public TestContextBuilder(final String profileId) {
        profileRequestContext.setProfileId(profileId);
    }

    public TestContextBuilder addSessionContext(final IdPSession session) {
        final SessionContext sessionContext = new SessionContext();
        sessionContext.setIdPSession(session);
        profileRequestContext.addSubcontext(sessionContext);
        return this;
    }

    public TestContextBuilder addProtocolContext(final Object request, final Object response) {
        final ProtocolContext context = new ProtocolContext();
        context.setRequest(request);
        context.setResponse(response);
        profileRequestContext.addSubcontext(context);
        return this;
    }

    public TestContextBuilder addTicketContext(final Ticket ticket) {
        final ProtocolContext context = profileRequestContext.getSubcontext(ProtocolContext.class, true);
        context.addSubcontext(new TicketContext(ticket));
        return this;
    }

    public TestContextBuilder addRelyingPartyContext(
            final String serviceURL, final boolean verified, final ProfileConfiguration config) {
        final RelyingPartyContext rpc = new RelyingPartyContext();
        rpc.setVerified(verified);
        rpc.setRelyingPartyId(serviceURL);
        rpc.setProfileConfig(config);
        profileRequestContext.addSubcontext(rpc);
        return this;
    }

    public TestContextBuilder addServiceContext(final Service service) {
        final ProtocolContext context = profileRequestContext.getSubcontext(ProtocolContext.class, true);
        context.addSubcontext(new ServiceContext(service));
        return this;
    }

    public RequestContext build() {
        final MockRequestContext requestContext = new MockRequestContext();
        final MockExternalContext externalContext = new MockExternalContext();
        externalContext.setNativeRequest(new MockHttpServletRequest());
        externalContext.setNativeResponse(new MockHttpServletResponse());
        requestContext.setExternalContext(externalContext);
        requestContext.getConversationScope().put(ProfileRequestContext.BINDING_KEY, profileRequestContext);
        return requestContext;
    }
}
