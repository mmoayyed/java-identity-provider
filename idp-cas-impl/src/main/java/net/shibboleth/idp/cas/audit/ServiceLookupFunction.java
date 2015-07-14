package net.shibboleth.idp.cas.audit;

import com.google.common.base.Function;
import net.shibboleth.idp.cas.protocol.ProtocolContext;
import net.shibboleth.idp.cas.protocol.ProxyTicketRequest;
import net.shibboleth.idp.cas.protocol.ServiceTicketRequest;
import net.shibboleth.idp.cas.protocol.TicketValidationRequest;
import net.shibboleth.utilities.java.support.logic.Constraint;
import org.opensaml.messaging.context.navigate.ChildContextLookup;
import org.opensaml.profile.context.ProfileRequestContext;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * Looks up the service URL from the CAS protocol request.
 *
 * @author Marvin S. Addison
 */
public class ServiceLookupFunction implements Function<ProfileRequestContext, String> {
    @Nonnull
    private final Function<ProfileRequestContext, ProtocolContext> protocolContextFunction;

    public ServiceLookupFunction() {
        this(new ChildContextLookup<ProfileRequestContext, ProtocolContext>(ProtocolContext.class));
    }

    public ServiceLookupFunction(@Nonnull final Function<ProfileRequestContext, ProtocolContext> protocolLookup) {
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
        final String service;
        if (request instanceof ServiceTicketRequest) {
            service = ((ServiceTicketRequest) request).getService();
        } else if (request instanceof ProxyTicketRequest) {
            service = ((ProxyTicketRequest) request).getTargetService();
        } else if (request instanceof TicketValidationRequest) {
            service = ((TicketValidationRequest) request).getService();
        } else {
            throw new IllegalArgumentException("Unsupported request type: " + request);
        }
        return service;
    }
}
