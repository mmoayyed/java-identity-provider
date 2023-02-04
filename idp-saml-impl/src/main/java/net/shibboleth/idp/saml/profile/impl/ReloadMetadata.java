/*
 * Licensed to the University Corporation for Advanced Internet Development,
 * Inc. (UCAID) under one or more contributor license agreements.  See the
 * NOTICE file distributed with this work for additional information regarding
 * copyright ownership. The UCAID licenses this file to You under the Apache
 * License, Version 2.0 (the "License"); you may not use this file except in
 * compliance with the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package net.shibboleth.idp.saml.profile.impl;

import java.io.IOException;
import java.util.Objects;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.opensaml.profile.action.ActionSupport;
import org.opensaml.profile.action.EventIds;
import org.opensaml.profile.context.ProfileRequestContext;
import org.opensaml.saml.metadata.resolver.ChainingMetadataResolver;
import org.opensaml.saml.metadata.resolver.ClearableMetadataResolver;
import org.opensaml.saml.metadata.resolver.MetadataResolver;
import org.opensaml.saml.metadata.resolver.RefreshableMetadataResolver;
import org.slf4j.Logger;
import net.shibboleth.shared.primitive.LoggerFactory;
import org.springframework.webflow.execution.RequestContext;

import jakarta.servlet.http.HttpServletResponse;
import net.shibboleth.idp.profile.AbstractProfileAction;
import net.shibboleth.idp.profile.context.SpringRequestContext;
import net.shibboleth.shared.annotation.constraint.NonnullAfterInit;
import net.shibboleth.shared.annotation.constraint.NotEmpty;
import net.shibboleth.shared.component.ComponentInitializationException;
import net.shibboleth.shared.logic.Constraint;
import net.shibboleth.shared.resolver.ResolverException;
import net.shibboleth.shared.service.ReloadableService;
import net.shibboleth.shared.service.ServiceException;
import net.shibboleth.shared.service.ServiceableComponent;

/**
 * Action that refreshes or clears a {@link MetadataResolver} manually.
 * 
 * <p>The {@link MetadataResolver} to reload is indicated by supplying {@link #RESOLVER_ID} as a flow variable.</p>
 * 
 * <p>On success, a 200 HTTP status with a simple response body is returned. On failure, a non-successful
 * HTTP status is returned.</p>
 * 
 * @event {@link EventIds#PROCEED_EVENT_ID}
 * @event {@link EventIds#INVALID_MESSAGE}
 * @event {@link EventIds#INVALID_PROFILE_CTX}
 * @event {@link EventIds#IO_ERROR}
 */
public class ReloadMetadata extends AbstractProfileAction {

    /** Flow variable indicating ID of metadata provider bean to reload. */
    @Nonnull @NotEmpty public static final String RESOLVER_ID = "resolverId";

    /** Class logger. */
    @Nonnull private final Logger log = LoggerFactory.getLogger(ReloadMetadata.class);

    /** The service that contains the metadata. */
    @NonnullAfterInit private ReloadableService<MetadataResolver> metadataResolverService;
    
    /** Identifies bean to refresh. */
    @Nullable private String id;

    /**
     * Set the service that describes the metadata.
     * 
     * @param service what to set.
     */
    public void setMetadataResolver(@Nonnull final ReloadableService<MetadataResolver> service) {
        checkSetterPreconditions();
        metadataResolverService = Constraint.isNotNull(service, "MetadataResolver service cannot be null");
    }
    
    /** {@inheritDoc} */
    @Override protected void doInitialize() throws ComponentInitializationException {
        super.doInitialize();
        
        if (metadataResolverService == null) {
            throw new ComponentInitializationException("MetadataResolver service cannot be null");
        }
    }

    /** {@inheritDoc} */
    @Override protected boolean doPreExecute(@Nonnull final ProfileRequestContext profileRequestContext) {
        
        if (!super.doPreExecute(profileRequestContext)) {
            return false;
        } else if (getHttpServletResponse() == null) {
            log.debug("{} No HttpServletResponse available", getLogPrefix());
            ActionSupport.buildEvent(profileRequestContext, EventIds.INVALID_PROFILE_CTX);
            return false;
        }
        
        final SpringRequestContext springRequestContext =
                profileRequestContext.getSubcontext(SpringRequestContext.class);
        if (springRequestContext == null) {
            log.warn("{} Spring request context not found in profile request context", getLogPrefix());
            return false;
        }

        final RequestContext requestContext = springRequestContext.getRequestContext();
        if (requestContext == null) {
            log.warn("{} Web Flow request context not found in Spring request context", getLogPrefix());
            return false;
        }

        id = (String) requestContext.getFlowScope().get(RESOLVER_ID);
        if (id == null) {
            log.warn("{} No '{}' flow variable found", getLogPrefix(), RESOLVER_ID);
            try {
                getHttpServletResponse().sendError(HttpServletResponse.SC_NOT_FOUND, "Metadata source not found.");
            } catch (final IOException e) {
                ActionSupport.buildEvent(profileRequestContext, EventIds.IO_ERROR);
            }
            return false;
        }
        
        return true;
    }
    
    /** Iterate over all providers to find the one with the name, recursing into
     * chaining providers.
     * @param rootResolver where to start
     * @return the resolver, or null if none found.
     */
    @Nullable private MetadataResolver findProvider(final MetadataResolver rootResolver) {
        if (Objects.equals(id, rootResolver.getId())
                && (rootResolver instanceof RefreshableMetadataResolver
                        || rootResolver instanceof ClearableMetadataResolver)) {
            return rootResolver;
        } else if (rootResolver instanceof ChainingMetadataResolver) {
            for (final MetadataResolver childResolver : ((ChainingMetadataResolver) rootResolver).getResolvers()) {
                final MetadataResolver result = findProvider(childResolver);
                if (result != null) {
                    return result;
                }
            }
        }
        return null;
    }

    /** {@inheritDoc} */
    @Override protected void doExecute(@Nonnull final ProfileRequestContext profileRequestContext) {
        log.debug("{} Reloading metadata from '{}'", getLogPrefix(), id);

        try (final ServiceableComponent<MetadataResolver> component =
                metadataResolverService.getServiceableComponent()) {

            final MetadataResolver toProcess = findProvider(component.getComponent());

            if (toProcess != null) {
                if (toProcess instanceof RefreshableMetadataResolver) {
                    ((RefreshableMetadataResolver)toProcess).refresh();
                    log.debug("{} Refreshed metadata resolver: '{}'", getLogPrefix(), id);
                } else if (toProcess instanceof ClearableMetadataResolver) {
                    ((ClearableMetadataResolver)toProcess).clear();
                    log.debug("{} Cleared metadata resolver: '{}'", getLogPrefix(), id);
                }
                getHttpServletResponse().setStatus(HttpServletResponse.SC_OK);
                getHttpServletResponse().getWriter().println("Metadata reloaded for '" + id + "'");
            } else {
                log.warn("{} Unable to locate refreshable or clearable metadata resolver: '{}'", getLogPrefix(), id);
                getHttpServletResponse().sendError(HttpServletResponse.SC_NOT_FOUND, "Metadata source not found.");
            }
            
        } catch (final ResolverException e) {
            log.error("{} Error refreshing/clearing metadata resolver: '{}'", getLogPrefix(), id, e);
            try {
                getHttpServletResponse().sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, e.getMessage());
            } catch (final IOException e2) {
                log.error("{} I/O error responding to request", getLogPrefix(), e2);
                ActionSupport.buildEvent(profileRequestContext, EventIds.IO_ERROR);
            }
        } catch (final ServiceException e) {
            log.error("{} Invalid metadata resolver configuration: '{}'", getLogPrefix(), id, e);
            try {
                getHttpServletResponse().sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, e.getMessage());
            } catch (final IOException e2) {
                log.error("{} I/O error responding to request", getLogPrefix(), e2);
                ActionSupport.buildEvent(profileRequestContext, EventIds.IO_ERROR);
            }
        } catch (final IOException e) {
            log.error("{} I/O error responding to request", getLogPrefix(), e);
            ActionSupport.buildEvent(profileRequestContext, EventIds.IO_ERROR);
        }
    }
    
}