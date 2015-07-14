package net.shibboleth.idp.cas.audit;

import com.google.common.base.Function;
import net.shibboleth.idp.cas.protocol.ProtocolContext;
import net.shibboleth.idp.cas.protocol.ProxyTicketRequest;
import net.shibboleth.idp.cas.protocol.ProxyTicketResponse;
import net.shibboleth.idp.cas.protocol.ServiceTicketRequest;
import net.shibboleth.idp.cas.protocol.ServiceTicketResponse;
import net.shibboleth.idp.cas.protocol.TicketValidationRequest;
import net.shibboleth.utilities.java.support.logic.Constraint;
import org.opensaml.messaging.context.navigate.ChildContextLookup;
import org.opensaml.profile.context.ProfileRequestContext;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * Looks up the service (proxy) ticket provided in a CAS protocol request or produced in a CAS protocol response.
 *
 * @author Marvin S. Addison
 */
public class TicketLookupFunction implements Function<ProfileRequestContext, String> {
    @Nonnull
    private final Function<ProfileRequestContext, ProtocolContext> protocolContextFunction;

    public TicketLookupFunction() {
        this(new ChildContextLookup<ProfileRequestContext, ProtocolContext>(ProtocolContext.class));
    }

    public TicketLookupFunction(@Nonnull final Function<ProfileRequestContext, ProtocolContext> protocolLookup) {
        protocolContextFunction = Constraint.isNotNull(protocolLookup, "ProtocolContext lookup cannot be null");
    }

    @Nullable
    @Override
    public String apply(@Nonnull final ProfileRequestContext input) {
        final ProtocolContext protocolContext = protocolContextFunction.apply(input);
        if (protocolContext == null || protocolContext.getRequest() ==  null) {
            return null;
        }
        final Object request = protocolContext.getRequest();
        final Object response = protocolContext.getResponse();
        final String ticket;
        if (response instanceof ServiceTicketResponse) {
            ticket = ((ServiceTicketResponse) response).getTicket();
        } else if (response instanceof ProxyTicketResponse) {
            ticket = ((ProxyTicketResponse) response).getPt();
        } else if (request instanceof TicketValidationRequest) {
            ticket = ((TicketValidationRequest) request).getTicket();
        } else {
            throw new IllegalArgumentException("Unsupported request/response type: " + request + '/' + response);
        }
        return ticket;
    }
}
