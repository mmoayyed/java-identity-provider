package net.shibboleth.idp.cas.flow;

import net.shibboleth.idp.profile.ActionSupport;
import org.opensaml.profile.context.ProfileRequestContext;
import org.springframework.webflow.execution.Event;
import org.springframework.webflow.execution.RequestContext;

import javax.annotation.Nonnull;

/**
 * Action to publish the CAS protocol response message, i.e.
 * {@link net.shibboleth.idp.cas.protocol.ProtocolContext#getResponse()}, in Spring Webflow
 * request scope to make available in views. The key in request scope is the protocol response simple class name
 * converted to variable case, e.g. <code>TicketValidationResponse</code> is accessible as
 * <code>requestScope.ticketValidationResponse</code>.
 *
 * @author Marvin S. Addison
 */
public class PublishProtocolResponseAction extends AbstractCASProtocolAction {

    @Override
    protected Event doExecute(
            final @Nonnull RequestContext springRequestContext,
            final @Nonnull ProfileRequestContext profileRequestContext) {

        final Object response = getCASResponse(profileRequestContext);
        final String className = response.getClass().getSimpleName();
        final String keyName = className.substring(0, 1).toLowerCase() + className.substring(1);
        springRequestContext.getRequestScope().put(keyName, response);
        return ActionSupport.buildProceedEvent(this);
    }
}
