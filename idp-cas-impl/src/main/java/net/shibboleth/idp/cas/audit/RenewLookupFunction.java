package net.shibboleth.idp.cas.audit;

import com.google.common.base.Function;
import net.shibboleth.idp.cas.protocol.ProtocolContext;
import net.shibboleth.idp.cas.protocol.ServiceTicketRequest;
import net.shibboleth.utilities.java.support.logic.Constraint;
import org.opensaml.messaging.context.navigate.ChildContextLookup;
import org.opensaml.profile.context.ProfileRequestContext;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * Looks up the value of the CAS renew parameter from the request to the /login URI.
 *
 * @author Marvin S. Addison
 */
public class RenewLookupFunction implements Function<ProfileRequestContext, Boolean> {
    @Nonnull
    private final Function<ProfileRequestContext, ProtocolContext> protocolContextFunction;

    public RenewLookupFunction() {
        this(new ChildContextLookup<ProfileRequestContext, ProtocolContext>(ProtocolContext.class));
    }

    public RenewLookupFunction(@Nonnull final Function<ProfileRequestContext, ProtocolContext> protocolLookup) {
        protocolContextFunction = Constraint.isNotNull(protocolLookup, "ProtocolContext lookup cannot be null");
    }

    @Nullable
    @Override
    public Boolean apply(@Nonnull final ProfileRequestContext input) {
        final ProtocolContext protocolContext = protocolContextFunction.apply(input);
        if (protocolContext == null || protocolContext.getRequest() ==  null) {
            return null;
        }
        final Object request = protocolContext.getRequest();
        if (request instanceof ServiceTicketRequest) {
            return ((ServiceTicketRequest) request).isRenew();
        }
        throw new IllegalArgumentException("Unsupported request type: " + request);
    }
}
