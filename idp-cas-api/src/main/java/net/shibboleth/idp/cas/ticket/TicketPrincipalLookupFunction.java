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

package net.shibboleth.idp.cas.ticket;

import java.util.function.Function;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import net.shibboleth.idp.cas.protocol.ProtocolContext;
import net.shibboleth.shared.logic.Constraint;

import org.opensaml.messaging.context.navigate.ChildContextLookup;
import org.opensaml.profile.context.ProfileRequestContext;

/**
 * Looks up a principal name stored in a CAS ticket:
 *
 * {@link ProfileRequestContext} -&gt; {@link ProtocolContext} -&gt; {@link TicketContext} -&gt;
 * {@link Ticket#getTicketState()} -&gt; {@link TicketState#getPrincipalName()}.
 *
 * @author Marvin S. Addison
 * @since 3.3.0
 */
public class TicketPrincipalLookupFunction implements Function<ProfileRequestContext, String> {

    /** Ticket context lookup function. */
    @Nonnull private Function<ProfileRequestContext,TicketContext> ticketContextLookupFunction =
            new ChildContextLookup<>(TicketContext.class).compose(
                    new ChildContextLookup<>(ProtocolContext.class));


    /**
     * Sets the function used to retrieve a {@link TicketContext} from the {@link ProfileRequestContext}.
     *
     * @param function Ticket context lookup function.
     */
    public void setTicketContextLookupFunction(@Nonnull final Function<ProfileRequestContext,TicketContext> function) {
        ticketContextLookupFunction = Constraint.isNotNull(function, "Ticket lookup function cannot be null");
    }

    /** {@inheritDoc} */
    @Nullable public String apply(@Nullable final ProfileRequestContext profileRequestContext) {
        final TicketContext tc = ticketContextLookupFunction.apply(profileRequestContext);
        if (tc != null) {
            final TicketState state = tc.getTicket().getTicketState();
            if (state != null) {
                return state.getPrincipalName();
            }
        }
        return null;
    }
    
}