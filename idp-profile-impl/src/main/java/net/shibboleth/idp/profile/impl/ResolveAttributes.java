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

import java.util.Collection;
import java.util.Collections;
import java.util.function.Function;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.opensaml.messaging.context.navigate.ChildContextLookup;
import org.opensaml.profile.action.ActionSupport;
import org.opensaml.profile.context.ProfileRequestContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.shibboleth.idp.attribute.context.AttributeContext;
import net.shibboleth.idp.attribute.resolver.AttributeResolver;
import net.shibboleth.idp.attribute.resolver.ResolutionException;
import net.shibboleth.idp.attribute.resolver.context.AttributeResolutionContext;
import net.shibboleth.idp.attribute.transcoding.AttributeTranscoderRegistry;
import net.shibboleth.idp.authn.context.SubjectContext;
import net.shibboleth.idp.authn.context.navigate.SubjectContextPrincipalLookupFunction;
import net.shibboleth.idp.profile.AbstractProfileAction;
import net.shibboleth.idp.profile.IdPEventIds;
import net.shibboleth.idp.profile.context.RelyingPartyContext;
import net.shibboleth.idp.profile.context.SpringRequestContext;
import net.shibboleth.idp.profile.context.navigate.RelyingPartyIdLookupFunction;
import net.shibboleth.idp.profile.context.navigate.ResponderIdLookupFunction;
import net.shibboleth.utilities.java.support.annotation.constraint.NonnullAfterInit;
import net.shibboleth.utilities.java.support.annotation.constraint.NonnullElements;
import net.shibboleth.utilities.java.support.component.ComponentSupport;
import net.shibboleth.utilities.java.support.logic.Constraint;
import net.shibboleth.utilities.java.support.logic.FunctionSupport;
import net.shibboleth.utilities.java.support.primitive.StringSupport;
import net.shibboleth.utilities.java.support.service.ReloadableService;
import net.shibboleth.utilities.java.support.service.ServiceableComponent;

/**
 * Action that invokes the {@link AttributeResolver} for the current request.
 * 
 * @event {@link org.opensaml.profile.action.EventIds#PROCEED_EVENT_ID}
 * @event {@link IdPEventIds#UNABLE_RESOLVE_ATTRIBS}
 * 
 * @post If resolution is successful, an AttributeContext is created with the results.
 */
public final class ResolveAttributes extends AbstractProfileAction {

    /** Class logger. */
    @Nonnull private final Logger log = LoggerFactory.getLogger(ResolveAttributes.class);

    /** Service used to get the resolver used to fetch attributes. */
    @Nonnull private final ReloadableService<AttributeResolver> attributeResolverService;

    /** Transcoder registry service object. */
    @NonnullAfterInit private ReloadableService<AttributeTranscoderRegistry> transcoderRegistry;
    
    /** Strategy used to locate the identity of the issuer associated with the attribute resolution. */
    @Nullable private Function<ProfileRequestContext,String> issuerLookupStrategy;

    /** Strategy used to locate the identity of the recipient associated with the attribute resolution. */
    @Nullable private Function<ProfileRequestContext,String> recipientLookupStrategy;
    
    /** Strategy used to locate the principal name associated with the attribute resolution. */
    @Nullable private Function<ProfileRequestContext,String> principalNameLookupStrategy;

    /** Strategy used to locate or create the {@link AttributeContext} to populate. */
    @Nonnull private Function<ProfileRequestContext,AttributeContext> attributeContextCreationStrategy;
    
    /** Strategy used to determine the attributes to resolve. */
    @Nonnull private Function<ProfileRequestContext,Collection<String>> attributesLookupStrategy;
    
    /** Whether to treat resolver errors as equivalent to resolving no attributes. */
    private boolean maskFailures;
    
    /** Label distinguishing different "types" of attribute resolution for use in resolver. */
    @Nullable private String resolutionLabel;
    
    /** Whether to create and populate {@link AttributeResolutionContext}. */
    private boolean createResolutionContext;

    /**
     * Constructor.
     * 
     * @param resolverService resolver used to fetch attributes
     */
    public ResolveAttributes(@Nonnull final ReloadableService<AttributeResolver> resolverService) {
        attributeResolverService = Constraint.isNotNull(resolverService, "AttributeResolver cannot be null");
        
        issuerLookupStrategy = new ResponderIdLookupFunction();
        recipientLookupStrategy = new RelyingPartyIdLookupFunction();
        
        principalNameLookupStrategy =
                new SubjectContextPrincipalLookupFunction().compose(
                        new ChildContextLookup<>(SubjectContext.class));
        
        // Defaults to ProfileRequestContext -> RelyingPartyContext -> AttributeContext.
        attributeContextCreationStrategy = new ChildContextLookup<>(AttributeContext.class, true).compose(
                new ChildContextLookup<>(RelyingPartyContext.class));
        
        attributesLookupStrategy = FunctionSupport.constant(Collections.emptyList());
        
        maskFailures = true;
        createResolutionContext = true;
    }

    /**
     * Sets the registry of transcoding rules to apply to supply attribute display metadata.
     * 
     * @param registry registry service interface
     */
    public void setTranscoderRegistry(@Nullable final ReloadableService<AttributeTranscoderRegistry> registry) {
        ComponentSupport.ifInitializedThrowUnmodifiabledComponentException(this);
        
        transcoderRegistry = registry;
    }
    
    /**
     * Set the strategy used to lookup the issuer for this attribute resolution.
     * 
     * @param strategy  lookup strategy
     */
    public void setIssuerLookupStrategy(@Nullable final Function<ProfileRequestContext,String> strategy) {
        ComponentSupport.ifInitializedThrowUnmodifiabledComponentException(this);

        issuerLookupStrategy = strategy;
    }

    /**
     * Set the strategy used to lookup the recipient for this attribute resolution.
     * 
     * @param strategy  lookup strategy
     */
    public void setRecipientLookupStrategy(@Nullable final Function<ProfileRequestContext,String> strategy) {
        ComponentSupport.ifInitializedThrowUnmodifiabledComponentException(this);

        recipientLookupStrategy = strategy;
    }
    
    /**
     * Set the strategy used to locate the principal name for this attribute resolution.
     * 
     * @param strategy lookup strategy
     */
    public void setPrincipalNameLookupStrategy(@Nullable final Function<ProfileRequestContext,String> strategy) {
        ComponentSupport.ifInitializedThrowUnmodifiabledComponentException(this);

        principalNameLookupStrategy = strategy;
    }
    
    /**
     * Set the strategy used to locate or create the {@link AttributeContext} to populate.
     * 
     * @param strategy lookup/creation strategy
     */
    public void setAttributeContextCreationStrategy(
            @Nonnull final Function<ProfileRequestContext,AttributeContext> strategy) {
        ComponentSupport.ifInitializedThrowUnmodifiabledComponentException(this);

        attributeContextCreationStrategy =
                Constraint.isNotNull(strategy, "AttributeContext creation strategy cannot be null");
    }
    
    /**
     * Set a strategy to use to obtain the names of the attributes to resolve.
     * 
     * @param strategy lookup strategy
     */
    public void setAttributesLookupStrategy(
            @Nonnull final Function<ProfileRequestContext,Collection<String>> strategy) {
        ComponentSupport.ifInitializedThrowUnmodifiabledComponentException(this);
        
        attributesLookupStrategy = Constraint.isNotNull(strategy, "Attributes lookup strategy cannot be null");
    }
    
    /**
     * Set the attribute IDs to pass into the resolver.
     * 
     * @param attributeIds  attribute ID collection
     */
    public void setAttributesToResolve(@Nonnull @NonnullElements final Collection<String> attributeIds) {
        ComponentSupport.ifInitializedThrowUnmodifiabledComponentException(this);
        
        Constraint.isNotNull(attributeIds, "Attribute ID collection cannot be null");
        attributesLookupStrategy = FunctionSupport.<ProfileRequestContext,Collection<String>>constant(
                StringSupport.normalizeStringCollection(attributeIds));
    }
    
    /**
     * Set whether to treat resolution failure as equivalent to resolving no attributes.
     * 
     * <p>This matches the behavior of V2.</p>
     * 
     * @param flag flag to set
     */
    public void setMaskFailures(final boolean flag) {
        ComponentSupport.ifInitializedThrowUnmodifiabledComponentException(this);
        
        maskFailures = flag;
    }
    
    
    /**
     * Set the optional "contextual" label associated with this attribute resolution.
     * 
     * @param label label to set
     * 
     * @since 3.4.0
     */
    public void setResolutionLabel(@Nullable final String label) {
        resolutionLabel = StringSupport.trimOrNull(label);
    }
    
    /**
     * Set whether to create the {@link AttributeResolutionContext} internally.
     * 
     * <p>Defaults to 'true', disable to allow external creation of the context.</p>
     * 
     * @param flag flag to set
     */
    public void setCreateResolutionContext(final boolean flag) {
        ComponentSupport.ifInitializedThrowUnmodifiabledComponentException(this);
        
        createResolutionContext = flag;
    }

    /** {@inheritDoc} */
    @Override
    protected boolean doPreExecute(@Nonnull final ProfileRequestContext profileRequestContext) {

        if (!super.doPreExecute(profileRequestContext)) {
            return false;
        }

        if (resolutionLabel == null) {
            final SpringRequestContext springContext = profileRequestContext.getSubcontext(SpringRequestContext.class);
            if (springContext != null && springContext.getRequestContext() != null) {
                resolutionLabel = springContext.getRequestContext().getActiveFlow().getId();
            }
        }
        
        return true;
    }

    /** {@inheritDoc} */
    @Override
    protected void doExecute(@Nonnull final ProfileRequestContext profileRequestContext) {

        final AttributeResolutionContext resolutionContext;
        if (createResolutionContext) {
            resolutionContext = profileRequestContext.getSubcontext(AttributeResolutionContext.class, true);
            populateResolutionContext(profileRequestContext, resolutionContext);
        } else {
            resolutionContext = profileRequestContext.getSubcontext(AttributeResolutionContext.class);
            if (resolutionContext == null) {
                log.error("{} Unable to locate AttributeResolutionContext", getLogPrefix());
                if (!maskFailures) {
                    ActionSupport.buildEvent(profileRequestContext, IdPEventIds.UNABLE_RESOLVE_ATTRIBS);
                }
                return;
            }
        }

        ServiceableComponent<AttributeResolver> component = null;
        try {
            component = attributeResolverService.getServiceableComponent();
            if (null == component) {
                log.error("{} Error resolving attributes: Invalid Attribute resolver configuration", getLogPrefix());
                if (!maskFailures) {
                    ActionSupport.buildEvent(profileRequestContext, IdPEventIds.UNABLE_RESOLVE_ATTRIBS);
                }
            } else {
                final AttributeResolver attributeResolver = component.getComponent();
                attributeResolver.resolveAttributes(resolutionContext);
                profileRequestContext.removeSubcontext(resolutionContext);

                final AttributeContext attributeCtx = attributeContextCreationStrategy.apply(profileRequestContext);
                if (null == attributeCtx) {
                    throw new ResolutionException("Unable to create or locate AttributeContext to populate");
                }
                attributeCtx.setIdPAttributes(resolutionContext.getResolvedIdPAttributes().values());
                attributeCtx.setUnfilteredIdPAttributes(resolutionContext.getResolvedIdPAttributes().values());
            }
        } catch (final ResolutionException e) {
            log.error("{} Error resolving attributes", getLogPrefix(), e);
            if (!maskFailures) {
                ActionSupport.buildEvent(profileRequestContext, IdPEventIds.UNABLE_RESOLVE_ATTRIBS);
            }
        } finally {
            if (null != component) {
                component.unpinComponent();
            }
        }
    }

    /**
     * Fill in the resolution context data.
     * 
     * @param profileRequestContext current profile request context
     * @param resolutionContext context to populate
     */
    private void populateResolutionContext(@Nonnull final ProfileRequestContext profileRequestContext,
            @Nonnull final AttributeResolutionContext resolutionContext) {

        resolutionContext
            .setResolutionLabel(resolutionLabel)
            .setTranscoderRegistry(transcoderRegistry);
        
        // Populate requested attributes, if not already set.
        if (resolutionContext.getRequestedIdPAttributeNames() == null
                || resolutionContext.getRequestedIdPAttributeNames().isEmpty()) {
            resolutionContext.setRequestedIdPAttributeNames(attributesLookupStrategy.apply(profileRequestContext));
        }
        
        if (null != principalNameLookupStrategy) {
            resolutionContext.setPrincipal(principalNameLookupStrategy.apply(profileRequestContext));
        } else {
            resolutionContext.setPrincipal(null);
        }
        
        if (recipientLookupStrategy != null) {
            resolutionContext.setAttributeRecipientID(recipientLookupStrategy.apply(profileRequestContext));
        } else {
            resolutionContext.setAttributeRecipientID(null);
        }

        if (issuerLookupStrategy != null) {
            resolutionContext.setAttributeIssuerID(issuerLookupStrategy.apply(profileRequestContext));
        } else {
            resolutionContext.setAttributeIssuerID(null);
        }
    }
    
}