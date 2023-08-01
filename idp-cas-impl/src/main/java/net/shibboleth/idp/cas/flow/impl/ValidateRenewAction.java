/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package net.shibboleth.idp.cas.flow.impl;

import javax.annotation.Nonnull;

import org.opensaml.profile.action.ActionSupport;
import org.opensaml.profile.action.EventException;
import org.opensaml.profile.context.ProfileRequestContext;
import org.slf4j.Logger;

import net.shibboleth.idp.cas.protocol.ProtocolError;
import net.shibboleth.idp.cas.protocol.TicketValidationRequest;
import net.shibboleth.idp.cas.protocol.TicketValidationResponse;
import net.shibboleth.idp.cas.ticket.ServiceTicket;
import net.shibboleth.idp.cas.ticket.Ticket;
import net.shibboleth.shared.annotation.constraint.NonnullBeforeExec;
import net.shibboleth.shared.primitive.LoggerFactory;
/**
 * Ensures that a service ticket validation request that specifies renew=true matches the renew flag on the ticket
 * that is presented for validation. Possible outcomes:
 * <ul>
 *     <li><code>null</code> on success</li>
 *     <li>{@link ProtocolError#TicketNotFromRenew ticketNotFromRenew}</li>
 *     <li>{@link ProtocolError#RenewIncompatibleWithProxy renewIncompatibleWithProxy}</li>
 * </ul>
 *
 * @author Marvin S. Addison
 */
public class ValidateRenewAction extends AbstractCASProtocolAction<TicketValidationRequest, TicketValidationResponse> {

    /** Class logger. */
    @Nonnull private final Logger log = LoggerFactory.getLogger(ValidateRenewAction.class);

    /** CAS ticket. */
    @NonnullBeforeExec private Ticket ticket;

    /** CAS request. */
    @NonnullBeforeExec private TicketValidationRequest request;

    /** {@inheritDoc} */
    @Override
    protected boolean doPreExecute(@Nonnull final ProfileRequestContext profileRequestContext) {
        if (!super.doPreExecute(profileRequestContext)) {
            return false;
        }
        
        try {
            ticket = getCASTicket(profileRequestContext);
            request = getCASRequest(profileRequestContext);
        } catch (final EventException e) {
            ActionSupport.buildEvent(profileRequestContext, e.getEventID());
            return false;
        }

        return true;
    }
    
    /** {@inheritDoc} */
    @Override
    protected void doExecute(@Nonnull final ProfileRequestContext profileRequestContext) {

        if (ticket instanceof ServiceTicket) {
            if (request.isRenew() != ((ServiceTicket) ticket).isRenew()) {
                log.debug("{} Renew=true requested at validation time but ticket not issued with renew=true",
                        getLogPrefix());
                ActionSupport.buildEvent(profileRequestContext, ProtocolError.TicketNotFromRenew.event(this));
                return;
            }
        } else {
            // Proxy ticket validation
            if (request.isRenew()) {
                ActionSupport.buildEvent(profileRequestContext, ProtocolError.RenewIncompatibleWithProxy.event(this));
                return;
            }
        }
    }

}