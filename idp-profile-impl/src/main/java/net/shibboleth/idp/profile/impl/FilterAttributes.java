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

package net.shibboleth.idp.profile.impl;

import java.util.Collections;
import java.util.function.Function;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.opensaml.messaging.context.navigate.ChildContextLookup;
import org.opensaml.messaging.context.navigate.RootContextLookup;
import org.opensaml.profile.action.ActionSupport;
import org.opensaml.profile.context.ProfileRequestContext;
import org.opensaml.profile.context.ProxiedRequesterContext;
import org.opensaml.profile.context.navigate.InboundMessageContextLookup;
import org.opensaml.saml.common.messaging.context.SAMLMetadataContext;
import org.opensaml.saml.common.messaging.context.SAMLPeerEntityContext;
import org.opensaml.saml.metadata.resolver.MetadataResolver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.shibboleth.idp.attribute.context.AttributeContext;
import net.shibboleth.idp.attribute.filter.AttributeFilter;
import net.shibboleth.idp.attribute.filter.AttributeFilterException;
import net.shibboleth.idp.attribute.filter.context.AttributeFilterContext;
import net.shibboleth.idp.attribute.filter.context.AttributeFilterContext.Direction;
import net.shibboleth.idp.authn.context.SubjectContext;
import net.shibboleth.idp.authn.context.navigate.SubjectContextPrincipalLookupFunction;
import net.shibboleth.idp.profile.AbstractProfileAction;
import net.shibboleth.idp.profile.IdPEventIds;
import net.shibboleth.idp.profile.context.RelyingPartyContext;
import net.shibboleth.idp.profile.context.navigate.RelyingPartyIdLookupFunction;
import net.shibboleth.idp.profile.context.navigate.ResponderIdLookupFunction;
import net.shibboleth.shared.logic.Constraint;
import net.shibboleth.shared.service.ReloadableService;
import net.shibboleth.shared.service.ServiceException;
import net.shibboleth.shared.service.ServiceableComponent;

/**
 * Action that invokes the {@link AttributeFilter} for the current request.
 * 
 * @event {@link org.opensaml.profile.action.EventIds#PROCEED_EVENT_ID}
 * @event {@link IdPEventIds#UNABLE_FILTER_ATTRIBS}
 * 
 * @post If resolution is successful, the relevant RelyingPartyContext.getSubcontext(AttributeContext.class, false) !=
 *       null
 */
public class FilterAttributes extends AbstractProfileAction {

    /** Class logger. */
    @Nonnull private final Logger log = LoggerFactory.getLogger(FilterAttributes.class);

    /** Service used to get the engine used to filter attributes. */
    @Nonnull private final ReloadableService<AttributeFilter> attributeFilterService;

    /** Optional supplemental metadata source. */
    @Nullable private MetadataResolver metadataResolver;
    
    /** Strategy used to locate the identity of the issuer associated with the attribute filtering. */
    @Nullable private Function<ProfileRequestContext,String> issuerLookupStrategy;

    /** Strategy used to locate the identity of the recipient associated with the attribute filtering. */
    @Nullable private Function<ProfileRequestContext,String> recipientLookupStrategy;
    
    /** Strategy used to locate or create the {@link AttributeFilterContext}. */
    @Nonnull private Function<ProfileRequestContext,AttributeFilterContext> filterContextCreationStrategy;

    /** Strategy used to locate the {@link AttributeContext} to filter. */
    @Nonnull private Function<ProfileRequestContext,AttributeContext> attributeContextLookupStrategy;
    
    /** Strategy used to locate the principal name associated with the attribute filtering. */
    @Nonnull private Function<ProfileRequestContext,String> principalNameLookupStrategy;

    /**
     * Strategy to locate the effectively rooted {@link ProfileRequestContext} from the
     * {@link AttributeFilterContext}.
     */
    @Nonnull
    private Function<AttributeFilterContext,ProfileRequestContext> profileRequestContextFromFilterLookupStrategy;
    
    /**
     * Strategy used to locate the {@link SAMLMetadataContext} for the issuer
     * associated with a given {@link ProfileRequestContext}.
     */
    @Nullable private Function<ProfileRequestContext,SAMLMetadataContext> issuerMetadataContextLookupStrategy;
    
    /**
     * Strategy used to locate the {@link SAMLMetadataContext} for the issuer
     * associated with a given {@link AttributeFilterContext}.
     */
    @Nullable private Function<AttributeFilterContext,SAMLMetadataContext> issuerMetadataFromFilterLookupStrategy;
    
    /**
     * Strategy used to locate the {@link SAMLMetadataContext} for the recipient
     * associated with a given {@link ProfileRequestContext}.
     */
    @Nullable private Function<ProfileRequestContext,SAMLMetadataContext> metadataContextLookupStrategy;
    
    /**
     * Strategy used to locate the {@link SAMLMetadataContext} for the recipient
     * associated with a given {@link AttributeFilterContext}.
     */
    @Nullable private Function<AttributeFilterContext,SAMLMetadataContext> metadataFromFilterLookupStrategy;

    /**
     * Strategy used to locate the {@link ProxiedRequesterContext} associated with a given
     * {@link ProfileRequestContext}.
     */
    @Nullable private Function<ProfileRequestContext,ProxiedRequesterContext> proxiedRequesterContextLookupStrategy;

    /**
     * Strategy used to locate the {@link ProxiedRequesterContext} associated with a given
     * {@link AttributeFilterContext}.
     */
    @Nullable private Function<AttributeFilterContext,ProxiedRequesterContext> proxiesFromFilterLookupStrategy;
    
    /** Strategy used to locate the {@link SAMLMetadataContext} for the proxied requester. */
    @Nullable private Function<ProfileRequestContext,SAMLMetadataContext> proxiedRequesterMetadataLookupStrategy;
    
    /**
     * Strategy used to locate the {@link SAMLMetadataContext} for the proxied requester via the
     * {@link AttributeFilterContext}.
     */
    @Nullable private Function<AttributeFilterContext,SAMLMetadataContext> proxiedMetadataFromFilterLookupStrategy;
    
    /** Whether to treat resolver errors as equivalent to resolving no attributes. */
    private boolean maskFailures;

    /** AttributeContext to filter. */
    @Nullable private AttributeContext attributeContext;

    /**
     * Constructor.
     * 
     * @param filterService engine used to filter attributes
     */
    public FilterAttributes(@Nonnull final ReloadableService<AttributeFilter> filterService) {
        attributeFilterService = Constraint.isNotNull(filterService, "Service cannot be null");
        
        issuerLookupStrategy = new ResponderIdLookupFunction();
        recipientLookupStrategy = new RelyingPartyIdLookupFunction();
        
        attributeContextLookupStrategy = new ChildContextLookup<>(AttributeContext.class).compose(
                new ChildContextLookup<>(RelyingPartyContext.class));

        principalNameLookupStrategy =
                new SubjectContextPrincipalLookupFunction().compose(
                        new ChildContextLookup<>(SubjectContext.class));

        // Default is to locate the overall root.
        profileRequestContextFromFilterLookupStrategy = new RootContextLookup<>(ProfileRequestContext.class);
                
        // Default: inbound msg context -> SAMLPeerEntityContext -> SAMLMetadataContext
        metadataContextLookupStrategy =
                new ChildContextLookup<>(SAMLMetadataContext.class).compose(
                        new ChildContextLookup<>(SAMLPeerEntityContext.class).compose(
                                new InboundMessageContextLookup()));
        
        // This is always set to navigate to the PRC and then apply the previous function.
        metadataFromFilterLookupStrategy = metadataContextLookupStrategy.compose(
                profileRequestContextFromFilterLookupStrategy);

        // Default: inbound msg context -> child
        proxiedRequesterContextLookupStrategy =
                new ChildContextLookup<>(ProxiedRequesterContext.class).compose(new InboundMessageContextLookup());
        
        // This is always set to navigate to the PRC and then apply the previous function.
        proxiesFromFilterLookupStrategy = proxiedRequesterContextLookupStrategy.compose(
                profileRequestContextFromFilterLookupStrategy);
        
        // Defaults to ProfileRequestContext -> RelyingPartyContext -> AttributeFilterContext.
        filterContextCreationStrategy = new ChildContextLookup<>(AttributeFilterContext.class, true).compose(
                new ChildContextLookup<>(RelyingPartyContext.class));
        
        maskFailures = true;
    }
    
    /**
     * Set a metadata source to use during filtering.
     * 
     * @param resolver metadata resolver
     * 
     * @since 3.4.0
     */
    public void setMetadataResolver(@Nullable final MetadataResolver resolver) {
        checkSetterPreconditions();
        metadataResolver = resolver;
    }
    
    /**
     * Set the strategy used to lookup the issuer for this attribute filtering.
     * 
     * @param strategy  lookup strategy
     */
    public void setIssuerLookupStrategy(@Nullable final Function<ProfileRequestContext,String> strategy) {
        checkSetterPreconditions();
        issuerLookupStrategy = strategy;
    }

    /**
     * Set the strategy used to lookup the recipient for this attribute filtering.
     * 
     * @param strategy  lookup strategy
     */
    public void setRecipientLookupStrategy(@Nullable final Function<ProfileRequestContext,String> strategy) {
        checkSetterPreconditions();
        recipientLookupStrategy = strategy;
    }

    /**
     * Set the strategy used to locate or create the {@link AttributeFilterContext} to populate.
     * 
     * @param strategy lookup/creation strategy
     */
    public void setFilterContextCreationStrategy(
            @Nonnull final Function<ProfileRequestContext,AttributeFilterContext> strategy) {
        checkSetterPreconditions();
        filterContextCreationStrategy =
                Constraint.isNotNull(strategy, "AttributeContext creation strategy cannot be null");
    }

    /**
     * Set the strategy used to locate the {@link AttributeContext} associated with a given
     * {@link ProfileRequestContext}.
     * 
     * @param strategy strategy used to locate the {@link AttributeContext} associated with a given
     *            {@link ProfileRequestContext}
     */
    public void setAttributeContextLookupStrategy(
            @Nonnull final Function<ProfileRequestContext,AttributeContext> strategy) {
        checkSetterPreconditions();
        attributeContextLookupStrategy =
                Constraint.isNotNull(strategy, "AttributeContext lookup strategy cannot be null");
    }
    
    /**
     * Set the strategy used to locate the principal name for this attribute filtering.
     * 
     * @param strategy lookup strategy
     */
    public void setPrincipalNameLookupStrategy(@Nonnull final Function<ProfileRequestContext,String> strategy) {
        checkSetterPreconditions();
        principalNameLookupStrategy = Constraint.isNotNull(strategy, "Principal name lookup strategy cannot be null");
    }
    
    /**
     * Sets the strategy used to locate the {@link SAMLMetadataContext} for the issuer associated with a
     * given {@link ProfileRequestContext}. Also sets the strategy to find the {@link SAMLMetadataContext}
     * from the {@link AttributeFilterContext}.
     * 
     * @param strategy lookup strategy
     */
    public void setIssuerMetadataContextLookupStrategy(
            @Nullable final Function<ProfileRequestContext,SAMLMetadataContext> strategy) {
        checkSetterPreconditions();
        issuerMetadataContextLookupStrategy = strategy;
        issuerMetadataFromFilterLookupStrategy = strategy != null ?
                issuerMetadataContextLookupStrategy.compose(profileRequestContextFromFilterLookupStrategy) : null;
    }
    
    /**
     * Sets the strategy used to locate the {@link SAMLMetadataContext} for the recipient associated with a
     * given {@link ProfileRequestContext}. Also sets the strategy to find the {@link SAMLMetadataContext}
     * from the {@link AttributeFilterContext}.
     * 
     * @param strategy lookup strategy
     */
    public void setMetadataContextLookupStrategy(
            @Nullable final Function<ProfileRequestContext,SAMLMetadataContext> strategy) {
        checkSetterPreconditions();
        metadataContextLookupStrategy = strategy;
        metadataFromFilterLookupStrategy = strategy != null ?
                metadataContextLookupStrategy.compose(profileRequestContextFromFilterLookupStrategy) : null;
    }

    /**
     * Sets the strategy used to locate the {@link ProxiedRequesterContext} associated with a given
     * {@link ProfileRequestContext}. Also sets the strategy to find the {@link ProxiedRequesterContext}
     * from the {@link AttributeFilterContext}.
     * 
     * @param strategy lookup strategy
     * 
     * @since 3.4.0
     */
    public void setProxiedRequesterContextLookupStrategy(
            @Nullable final Function<ProfileRequestContext,ProxiedRequesterContext> strategy) {
        checkSetterPreconditions();
        proxiedRequesterContextLookupStrategy = strategy;
        proxiesFromFilterLookupStrategy = strategy != null ?
                proxiedRequesterContextLookupStrategy.compose(profileRequestContextFromFilterLookupStrategy) : null;
    }
    
    /**
     * Sets the strategy used to locate proxied requester metadata.
     * 
     * @param strategy lookup strategy
     * 
     * @since 4.2.0
     */
    public void setProxiedRequesterMetadataContextLookupStrategy(
            @Nullable final Function<ProfileRequestContext,SAMLMetadataContext> strategy) {
        checkSetterPreconditions();
        proxiedRequesterMetadataLookupStrategy = strategy;
        proxiedMetadataFromFilterLookupStrategy = strategy != null ?
                proxiedRequesterMetadataLookupStrategy.compose(profileRequestContextFromFilterLookupStrategy) : null;
    }
    
    /**
     * Set whether to treat resolution failure as equivalent to resolving no attributes.
     * 
     * <p>This matches the behavior of V2.</p>
     * 
     * @param flag flag to set
     */
    public void setMaskFailures(final boolean flag) {
        checkSetterPreconditions();
        maskFailures = flag;
    }

    /** {@inheritDoc} */
    @Override
    protected boolean doPreExecute(@Nonnull final ProfileRequestContext profileRequestContext) {
        
        if (!super.doPreExecute(profileRequestContext)) {
            return false;
        }
        
        attributeContext = attributeContextLookupStrategy.apply(profileRequestContext);
        if (attributeContext == null) {
            log.debug("{} No attribute context, no attributes to filter", getLogPrefix());
            return false;
        }

        if (attributeContext.getIdPAttributes().isEmpty()) {
            log.debug("{} No attributes to filter", getLogPrefix());
            return false;
        }

        return true;
    }
    
    /** {@inheritDoc} */
    @Override
    protected void doExecute(@Nonnull final ProfileRequestContext profileRequestContext) {

        // Get the filter context from the profile request
        // this may already exist but if not, auto-create it.
        final AttributeFilterContext filterContext = filterContextCreationStrategy.apply(profileRequestContext);
        if (filterContext == null) {
            log.error("{} Unable to locate or create AttributeFilterContext", getLogPrefix());
            if (maskFailures) {
                log.warn("Filter error masked, clearing resolved attributes");
                attributeContext.setIdPAttributes(null);
            } else {
                ActionSupport.buildEvent(profileRequestContext, IdPEventIds.UNABLE_FILTER_ATTRIBS);
            }
            return;
        }
        
        populateFilterContext(profileRequestContext, filterContext);

        try (final ServiceableComponent<AttributeFilter> component = attributeFilterService.getServiceableComponent()) {
            final AttributeFilter filter = component.getComponent();
            filter.filterAttributes(filterContext);
            filterContext.getParent().removeSubcontext(filterContext);
            attributeContext.setIdPAttributes(filterContext.getFilteredIdPAttributes().values());
        } catch (final AttributeFilterException e) {
            log.error("{} Error encountered while filtering attributes", getLogPrefix(), e);
            if (maskFailures) {
                log.warn("Filter error masked, clearing resolved attributes");
                attributeContext.setIdPAttributes(Collections.emptySet());
            } else {
                ActionSupport.buildEvent(profileRequestContext, IdPEventIds.UNABLE_FILTER_ATTRIBS);
            }
        } catch (final ServiceException e) {
            log.error("{} Invalid Attribute Filter service configuration", getLogPrefix(), e);
            if (maskFailures) {
                log.warn("Filter error masked, clearing resolved attributes");
                attributeContext.setIdPAttributes(null);
            } else {
                ActionSupport.buildEvent(profileRequestContext, IdPEventIds.UNABLE_FILTER_ATTRIBS);
            }
        }
    }
    
    /**
     * Fill in the filter context data.
     * 
     * @param profileRequestContext current profile request context
     * @param filterContext context to populate
     */
    private void populateFilterContext(@Nonnull final ProfileRequestContext profileRequestContext,
            @Nonnull final AttributeFilterContext filterContext) {
        
        filterContext.setDirection(Direction.OUTBOUND)
            .setMetadataResolver(metadataResolver)
            .setPrincipal(principalNameLookupStrategy.apply(profileRequestContext))
            .setAttributeRecipientID(
                    recipientLookupStrategy != null ? recipientLookupStrategy.apply(profileRequestContext) : null)
            .setAttributeIssuerID(
                    issuerLookupStrategy != null ? issuerLookupStrategy.apply(profileRequestContext) : null)
            .setIssuerMetadataContextLookupStrategy(issuerMetadataFromFilterLookupStrategy)
            .setRequesterMetadataContextLookupStrategy(metadataFromFilterLookupStrategy)
            .setProxiedRequesterContextLookupStrategy(proxiesFromFilterLookupStrategy)
            .setProxiedRequesterMetadataContextLookupStrategy(proxiedMetadataFromFilterLookupStrategy);

        // If the filter context doesn't have a set of attributes to filter already
        // then look for them in the AttributeContext.
        if (filterContext.getPrefilteredIdPAttributes().isEmpty()) {
            filterContext.setPrefilteredIdPAttributes(attributeContext.getIdPAttributes().values());
        }
    }

}