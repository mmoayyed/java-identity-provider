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

import java.util.List;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.opensaml.profile.action.ActionSupport;
import org.opensaml.profile.action.EventException;
import org.opensaml.profile.action.EventIds;
import org.opensaml.profile.context.ProfileRequestContext;
import org.slf4j.Logger;

import net.shibboleth.idp.cas.protocol.ProxyTicketRequest;
import net.shibboleth.idp.cas.protocol.ServiceTicketRequest;
import net.shibboleth.idp.cas.protocol.TicketValidationRequest;
import net.shibboleth.idp.cas.service.Service;
import net.shibboleth.idp.cas.service.ServiceRegistry;
import net.shibboleth.profile.context.RelyingPartyContext;
import net.shibboleth.shared.annotation.constraint.NonnullBeforeExec;
import net.shibboleth.shared.annotation.constraint.NotEmpty;
import net.shibboleth.shared.collection.CollectionSupport;
import net.shibboleth.shared.logic.Constraint;
import net.shibboleth.shared.primitive.LoggerFactory;

/**
 * Creates the {@link RelyingPartyContext} as a child of the {@link ProfileRequestContext}. The component queries
 * a configured list of {@link ServiceRegistry} until a result is found, otherwise the relying party is treated as
 * unverified.
 * 
 * @param <RequestType> request
 * @param <ResponseType> response
 *
 * @author Marvin S. Addison
 */
public class BuildRelyingPartyContextAction<RequestType,ResponseType>
        extends AbstractCASProtocolAction<RequestType,ResponseType> {

    /** Name of group to which unverified services belong. */
    @Nonnull @NotEmpty public static final String UNVERIFIED_GROUP = "unverified";

    /** Class logger. */
    @Nonnull private final Logger log = LoggerFactory.getLogger(BuildRelyingPartyContextAction.class);

    /** List of registries to query for verified CAS services (relying parties). */
    @Nonnull @NotEmpty private final List<ServiceRegistry> serviceRegistries;
    
    /** Request. */
    @NonnullBeforeExec private Object request;

    /**
     * Creates a new instance.
     *
     * @param registries One or more service registries to query for CAS services.
     */
    public BuildRelyingPartyContextAction(@Nonnull @NotEmpty final ServiceRegistry ... registries) {
        serviceRegistries = CollectionSupport.listOf(
                Constraint.isNotEmpty(registries, "Service registries cannot be null"));
    }
    
    /** {@inheritDoc} */
    @Override
    protected boolean doPreExecute(@Nonnull final ProfileRequestContext profileRequestContext) {
        if (!super.doPreExecute(profileRequestContext)) {
            return false;
        }
        
        try {
            request = getCASRequest(profileRequestContext);
        } catch (final EventException e) {
            ActionSupport.buildEvent(profileRequestContext, e.getEventID());
            return false;
        }
        
        return true;
    }
    

    /** {@inheritDoc} */
    @Override
    protected void doExecute(final @Nonnull ProfileRequestContext profileRequestContext) {

        final String serviceURL;
        
        if (request instanceof ServiceTicketRequest) {
            serviceURL = ((ServiceTicketRequest) request).getService();
        } else if (request instanceof ProxyTicketRequest) {
            serviceURL = ((ProxyTicketRequest) request).getTargetService();
        } else if (request instanceof TicketValidationRequest) {
            serviceURL = ((TicketValidationRequest) request).getService();
        } else {
            log.warn("{} Service URL not found in flow state", getLogPrefix());
            ActionSupport.buildEvent(profileRequestContext, EventIds.INVALID_MESSAGE);
            return;
        }
        
        Service service = query(serviceURL);
        final RelyingPartyContext rpc = new RelyingPartyContext();
        rpc.setVerified(service != null);
        rpc.setRelyingPartyId(serviceURL);
        if (service != null) {
            log.debug("{} Setting up RP context for verified relying party {}", getLogPrefix(), service);
        } else {
            service = new Service(serviceURL, UNVERIFIED_GROUP, false);
            log.debug("{} Setting up RP context for unverified relying party {}", getLogPrefix(), service);
        }
        log.debug("{} Relying party context created for {}", getLogPrefix(), service);
        
        profileRequestContext.addSubcontext(rpc);
        
        try {
            setCASService(profileRequestContext, service);
        } catch (final EventException e) {
            ActionSupport.buildEvent(profileRequestContext, e.getEventID());
        }
    }

    /**
     * Query service registry based on URL.
     * 
     * @param serviceURL the URL
     * 
     * @return the result of the lookup or null
     */
    @Nullable private Service query(@Nonnull final String serviceURL) {
        
        for (final ServiceRegistry registry : serviceRegistries) {
            log.debug("Querying {} for CAS service URL {}", registry.getClass().getName(), serviceURL);
            final Service service = registry.lookup(serviceURL);
            if (service != null) {
                return service;
            }
        }
        
        return null;
    }
    
}