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

import java.util.Collections;
import java.util.List;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.xml.namespace.QName;

import net.shibboleth.idp.profile.AbstractProfileAction;
import net.shibboleth.idp.profile.context.RelyingPartyContext;
import net.shibboleth.idp.profile.context.navigate.ResponderIdLookupFunction;
import net.shibboleth.idp.saml.profile.config.SAMLArtifactConfiguration;
import net.shibboleth.idp.saml.profile.config.SAMLProfileConfiguration;
import net.shibboleth.idp.saml.saml2.profile.config.BrowserSSOProfileConfiguration;
import net.shibboleth.utilities.java.support.annotation.constraint.NonnullAfterInit;
import net.shibboleth.utilities.java.support.annotation.constraint.NonnullElements;
import net.shibboleth.utilities.java.support.component.ComponentInitializationException;
import net.shibboleth.utilities.java.support.component.ComponentSupport;
import net.shibboleth.utilities.java.support.logic.Constraint;
import net.shibboleth.utilities.java.support.resolver.CriteriaSet;
import net.shibboleth.utilities.java.support.resolver.ResolverException;

import org.opensaml.core.xml.XMLObjectBuilder;
import org.opensaml.core.xml.util.XMLObjectSupport;
import org.opensaml.messaging.context.navigate.ChildContextLookup;
import org.opensaml.profile.action.ActionSupport;
import org.opensaml.profile.action.EventIds;
import org.opensaml.profile.context.ProfileRequestContext;
import org.opensaml.profile.context.navigate.OutboundMessageContextLookup;
import org.opensaml.saml.common.binding.BindingDescriptor;
import org.opensaml.saml.common.binding.EndpointResolver;
import org.opensaml.saml.common.binding.SAMLBindingSupport;
import org.opensaml.saml.common.messaging.context.SAMLArtifactContext;
import org.opensaml.saml.common.messaging.context.SAMLBindingContext;
import org.opensaml.saml.common.messaging.context.SAMLEndpointContext;
import org.opensaml.saml.common.messaging.context.SAMLMetadataContext;
import org.opensaml.saml.common.messaging.context.SAMLPeerEntityContext;
import org.opensaml.saml.common.profile.SAMLEventIds;
import org.opensaml.saml.criterion.BindingCriterion;
import org.opensaml.saml.criterion.EndpointCriterion;
import org.opensaml.saml.criterion.RoleDescriptorCriterion;
import org.opensaml.saml.criterion.SignedRequestCriterion;
import org.opensaml.saml.saml2.core.AuthnRequest;
import org.opensaml.saml.saml2.metadata.AssertionConsumerService;
import org.opensaml.saml.saml2.metadata.Endpoint;
import org.opensaml.saml.saml2.metadata.IndexedEndpoint;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Function;
import com.google.common.base.Functions;
import com.google.common.base.Optional;
import com.google.common.base.Predicate;
import com.google.common.base.Predicates;
import com.google.common.collect.Collections2;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;

/**
 * Action that populates the outbound {@link SAMLBindingContext} and when appropriate the
 * {@link SAMLEndpointContext} based on the inbound request.
 * 
 * <p>If the inbound binding is found in the set of supported bindings, and it is "synchronous",
 * then there is no endpoint (the response is sent directly back to the requester), and an
 * endpoint context is not created. A binding context is created based on the inbound binding.</p>
 * 
 * <p>Otherwise, the endpoint context is populated by constructing a "template" endpoint,
 * with content based on the inbound request, and relying on an injected {@link EndpointResolver}
 * and an injected list of acceptable bindings.</p>
 * 
 * <p>The binding context is populated based on the computed endpoint's binding, and the
 * inbound {@link SAMLBindingContext}'s relay state.</p>
 * 
 * <p>If the outbound binding is an artifact-based binding, then the action also creates
 * a {@link SAMLArtifactContext} populated by settings from the {@link SAMLArtifactConfiguration}.</p> 
 * 
 * <p>The base action understands SAML 2 {@link AuthnRequest} messages. Subclasses may
 * override the {@link #supplementResolverCriteria(ProfileRequestContext, CriteriaSet)} method
 * to implement support for other message types or advanced criteria.</p>
 * 
 * @event {@link EventIds#PROCEED_EVENT_ID}
 * @event {@link EventIds#INVALID_MSG_CTX}
 * @event {@link SAMLEventIds#ENDPOINT_RESOLUTION_FAILED}
 */
public class PopulateBindingAndEndpointContexts extends AbstractProfileAction {

    /** Class logger. */
    @Nonnull private final Logger log = LoggerFactory.getLogger(PopulateBindingAndEndpointContexts.class);
    
    /** The type of endpoint to resolve. */
    @Nonnull private QName endpointType;

    /** Endpoint resolver. */
    @NonnullAfterInit private EndpointResolver<?> endpointResolver;
    
    /** List of possible bindings, in preference order. */
    @Nonnull @NonnullElements private List<BindingDescriptor> bindingDescriptors;

    /** Strategy function for access to {@link RelyingPartyContext}. */
    @Nonnull private Function<ProfileRequestContext,RelyingPartyContext> relyingPartyContextLookupStrategy;

    /** Strategy used to obtain the self identity value. */
    @Nullable private Function<ProfileRequestContext,String> selfIdentityLookupStrategy;
    
    /** Strategy function for access to {@link SAMLMetadataContext} for input to resolver. */
    @Nonnull private Function<ProfileRequestContext,SAMLMetadataContext> metadataContextLookupStrategy;

    /** Strategy function for access to {@link SAMLBindingContext} to populate. */
    @Nonnull private Function<ProfileRequestContext,SAMLBindingContext> bindingContextLookupStrategy;

    /** Strategy function for access to {@link SAMLEndpointContext} to populate. */
    @Nonnull private Function<ProfileRequestContext,SAMLEndpointContext> endpointContextLookupStrategy;

    /** Strategy function for access to {@link SAMLArtifactContext} to populate. */
    @Nonnull private Function<ProfileRequestContext,SAMLArtifactContext> artifactContextLookupStrategy;
    
    /** Builder for template endpoints. */
    @NonnullAfterInit private XMLObjectBuilder<?> endpointBuilder;
    
    /** Artifact configuration. */
    @Nullable private SAMLArtifactConfiguration artifactConfiguration;
    
    /** Optional inbound message. */
    @Nullable private Object inboundMessage;
    
    /** Optional metadata for use in endpoint derivation/validation. */
    @Nullable private SAMLMetadataContext mdContext;
    
    /** Whether to bypass endpoint validation when message is signed. */
    private boolean skipValidationWhenSigned;
    
    /** Constructor. */
    public PopulateBindingAndEndpointContexts() {
        endpointType = AssertionConsumerService.DEFAULT_ELEMENT_NAME;
        bindingDescriptors = Collections.emptyList();
        
        relyingPartyContextLookupStrategy = new ChildContextLookup<>(RelyingPartyContext.class);
        selfIdentityLookupStrategy = new ResponderIdLookupFunction();
        
        // Default: outbound msg context -> SAMLPeerEntityContext -> SAMLMetadataContext
        metadataContextLookupStrategy = Functions.compose(
                new ChildContextLookup<>(SAMLMetadataContext.class),
                Functions.compose(new ChildContextLookup<>(SAMLPeerEntityContext.class),
                        new OutboundMessageContextLookup()));
        
        // Default: outbound msg context -> SAMLBindingContext
        bindingContextLookupStrategy = Functions.compose(
                new ChildContextLookup<>(SAMLBindingContext.class, true), new OutboundMessageContextLookup());

        // Default: outbound msg context -> SAMLArtifactContext
        artifactContextLookupStrategy = Functions.compose(
                new ChildContextLookup<>(SAMLArtifactContext.class, true), new OutboundMessageContextLookup());
        
        // Default: outbound msg context -> SAMLPeerEntityContext -> SAMLEndpointContext
        endpointContextLookupStrategy = Functions.compose(
                new ChildContextLookup<>(SAMLEndpointContext.class, true),
                Functions.compose(new ChildContextLookup<>(SAMLPeerEntityContext.class, true),
                        new OutboundMessageContextLookup()));
    }
    
    /**
     * Set the type of endpoint to resolve, defaults to {@link AssertionConsumerService}.
     * 
     * @param type  type of endpoint to resolve
     */
    public void setEndpointType(@Nonnull final QName type) {
        ComponentSupport.ifInitializedThrowUnmodifiabledComponentException(this);
        
        endpointType = Constraint.isNotNull(type, "Endpoint type cannot be null");
    }
    
    /**
     * Set a custom {@link EndpointResolver} to use.
     * 
     * @param resolver endpoint resolver to use  
     */
    public void setEndpointResolver(@Nonnull final EndpointResolver<?> resolver) {
        ComponentSupport.ifInitializedThrowUnmodifiabledComponentException(this);
        
        endpointResolver = Constraint.isNotNull(resolver, "EndpointResolver cannot be null");
    }
    
    /**
     * Set the bindings to evaluate for use, in preference order.
     * 
     * @param bindings bindings to consider
     */
    public void setBindings(@Nonnull @NonnullElements final List<BindingDescriptor> bindings) {
        ComponentSupport.ifInitializedThrowUnmodifiabledComponentException(this);
        Constraint.isNotNull(bindings, "Binding descriptor list cannot be null");
        
        bindingDescriptors = Lists.newArrayList(Collections2.filter(bindings, Predicates.notNull()));
    }

    /**
     * Set lookup strategy for {@link RelyingPartyContext}.
     * 
     * @param strategy  lookup strategy
     */
    public void setRelyingPartyContextLookupStrategy(
            @Nonnull final Function<ProfileRequestContext,RelyingPartyContext> strategy) {
        ComponentSupport.ifInitializedThrowUnmodifiabledComponentException(this);
        
        relyingPartyContextLookupStrategy = Constraint.isNotNull(strategy,
                "RelyingPartyContext lookup strategy cannot be null");
    }

    /**
     * Set the strategy used to locate the self identity value to use.
     * 
     * @param strategy lookup strategy
     */
    public void setSelfIdentityLookupStrategy(@Nonnull final Function<ProfileRequestContext,String> strategy) {
        ComponentSupport.ifInitializedThrowUnmodifiabledComponentException(this);

        selfIdentityLookupStrategy = Constraint.isNotNull(strategy, "Self identity lookup strategy cannot be null");
    }
    
    /**
     * Set lookup strategy for {@link SAMLMetadataContext} for input to resolution.
     * 
     * @param strategy  lookup strategy
     */
    public void setMetadataContextLookupStrategy(
            @Nonnull final Function<ProfileRequestContext,SAMLMetadataContext> strategy) {
        ComponentSupport.ifInitializedThrowUnmodifiabledComponentException(this);
        
        metadataContextLookupStrategy = Constraint.isNotNull(strategy,
                "SAMLMetadataContext lookup strategy cannot be null");
    }

    /**
     * Set lookup strategy for {@link SAMLBindingContext} to populate.
     * 
     * @param strategy  lookup strategy
     */
    public void setBindingContextLookupStrategy(
            @Nonnull final Function<ProfileRequestContext,SAMLBindingContext> strategy) {
        ComponentSupport.ifInitializedThrowUnmodifiabledComponentException(this);
        
        bindingContextLookupStrategy = Constraint.isNotNull(strategy,
                "SAMLBindingContext lookup strategy cannot be null");
    }

    /**
     * Set lookup strategy for {@link SAMLEndpointContext} to populate.
     * 
     * @param strategy  lookup strategy
     */
    public void setEndpointContextLookupStrategy(
            @Nonnull final Function<ProfileRequestContext,SAMLEndpointContext> strategy) {
        ComponentSupport.ifInitializedThrowUnmodifiabledComponentException(this);
        
        endpointContextLookupStrategy = Constraint.isNotNull(strategy,
                "SAMLEndpointContext lookup strategy cannot be null");
    }

    /**
     * Set lookup strategy for {@link SAMLArtifactContext} to populate.
     * 
     * @param strategy  lookup strategy
     */
    public void setArtifactContextLookupStrategy(
            @Nonnull final Function<ProfileRequestContext,SAMLArtifactContext> strategy) {
        ComponentSupport.ifInitializedThrowUnmodifiabledComponentException(this);
        
        artifactContextLookupStrategy = Constraint.isNotNull(strategy,
                "SAMLArtifactContext lookup strategy cannot be null");
    }
    
    /** {@inheritDoc} */
    @Override
    protected void doInitialize() throws ComponentInitializationException {
        super.doInitialize();

        if (endpointResolver == null) {
            throw new ComponentInitializationException("EndpointResolver cannot be null");
        }
        
        endpointBuilder = XMLObjectSupport.getBuilder(endpointType);
        if (endpointBuilder == null) {
            throw new ComponentInitializationException("Unable to obtain builder for endpoint type " + endpointType);
        } else if (!(endpointBuilder.buildObject(endpointType) instanceof Endpoint)) {
            throw new ComponentInitializationException("Builder for endpoint type " + endpointType
                    + " did not result in Endpoint object");
        }
    }
    
    /** {@inheritDoc} */
    @Override
    protected boolean doPreExecute(@Nonnull final ProfileRequestContext profileRequestContext) {
        if (profileRequestContext.getInboundMessageContext() != null) {
            inboundMessage = profileRequestContext.getInboundMessageContext().getMessage();
        }
        
        final RelyingPartyContext rpContext = relyingPartyContextLookupStrategy.apply(profileRequestContext);
        if (rpContext != null) {
            if (rpContext.getProfileConfig() != null
                    && rpContext.getProfileConfig() instanceof SAMLProfileConfiguration) {
                final SAMLProfileConfiguration profileConfiguration =
                        (SAMLProfileConfiguration) rpContext.getProfileConfig();
                artifactConfiguration = profileConfiguration.getArtifactConfiguration();
                if (profileConfiguration instanceof BrowserSSOProfileConfiguration) {
                    skipValidationWhenSigned =
                            ((BrowserSSOProfileConfiguration) profileConfiguration).skipEndpointValidationWhenSigned();
                }
            }
        }
        
        if (profileRequestContext.getOutboundMessageContext() == null) {
            log.debug("{} No outbound message context", getLogPrefix());
            ActionSupport.buildEvent(profileRequestContext, EventIds.INVALID_MSG_CTX);
            return false;
        }
        
        mdContext = metadataContextLookupStrategy.apply(profileRequestContext);

        return super.doPreExecute(profileRequestContext);
    }

// Checkstyle: CyclomaticComplexity|MethodLength OFF
    /** {@inheritDoc} */
    @Override protected void doExecute(@Nonnull final ProfileRequestContext profileRequestContext) {
        
        if (handleSynchronousRequest(profileRequestContext)) {
            return;
        }
        
        log.debug("{} Attempting to resolve endpoint of type {} for outbound message", getLogPrefix(), endpointType);

        // Compile binding list.
        final List<String> bindings = Lists.newArrayListWithExpectedSize(bindingDescriptors.size());
        for (final BindingDescriptor bindingDescriptor : bindingDescriptors) {
            if (bindingDescriptor.apply(profileRequestContext)) {
                bindings.add(bindingDescriptor.getId());
            }
        }
        if (bindings.isEmpty()) {
            log.warn("{} No outbound bindings are eligible for use", getLogPrefix());
            ActionSupport.buildEvent(profileRequestContext, SAMLEventIds.ENDPOINT_RESOLUTION_FAILED);
            return;
        } else {
            log.trace("{} Eligible outbound bindings: {}", getLogPrefix(), bindings);
        }
        
        // Build criteria for the resolver.
        final CriteriaSet criteria = new CriteriaSet(new EndpointCriterion(buildTemplateEndpoint()),
                new BindingCriterion(bindings));
        
        if (mdContext != null && mdContext.getRoleDescriptor() != null) {
            criteria.add(new RoleDescriptorCriterion(mdContext.getRoleDescriptor()));
        } else {
            log.debug("{} No metadata available for endpoint resolution", getLogPrefix());
        }
        
        if (skipValidationWhenSigned && inboundMessage instanceof AuthnRequest
                && ((AuthnRequest) inboundMessage).isSigned()) {
            criteria.add(new SignedRequestCriterion());
        }
        
        // Attempt resolution.
        Endpoint resolvedEndpoint = null;
        try {
            resolvedEndpoint = endpointResolver.resolveSingle(criteria);
        } catch (final ResolverException e) {
            log.error(getLogPrefix() + " Error resolving outbound message endpoint", e);
        }
        
        if (resolvedEndpoint == null) {
            log.warn("{} Unable to resolve outbound message endpoint", getLogPrefix());
            ActionSupport.buildEvent(profileRequestContext, SAMLEventIds.ENDPOINT_RESOLUTION_FAILED);
            return;
        }
        
        log.debug("{} Resolved endpoint at location {} using binding {}",
                new Object[] {getLogPrefix(), resolvedEndpoint.getLocation(), resolvedEndpoint.getBinding(),});
        
        // Transfer results to contexts.
        
        final SAMLEndpointContext endpointContext = endpointContextLookupStrategy.apply(profileRequestContext);
        endpointContext.setEndpoint(resolvedEndpoint);
        
        final SAMLBindingContext bindingCtx = bindingContextLookupStrategy.apply(profileRequestContext);
        bindingCtx.setRelayState(SAMLBindingSupport.getRelayState(profileRequestContext.getInboundMessageContext()));
        bindingCtx.setBindingUri(resolvedEndpoint.getBinding());
        
        // Handle artifact details.
        final Optional<BindingDescriptor> bindingDescriptor = Iterables.tryFind(bindingDescriptors,
                new Predicate<BindingDescriptor>() {
                    public boolean apply(BindingDescriptor input) {
                        return input.getId().equals(bindingCtx.getBindingUri());
                    }
        });
        if (bindingDescriptor.isPresent() && bindingDescriptor.get().isArtifact()) {
            final SAMLArtifactContext artifactCtx = artifactContextLookupStrategy.apply(profileRequestContext);
            artifactCtx.setSourceEntityId(selfIdentityLookupStrategy.apply(profileRequestContext));
            if (artifactConfiguration != null) {
                artifactCtx.setArtifactType(artifactConfiguration.getArtifactType());
                artifactCtx.setSourceArtifactResolutionServiceEndpointURL(
                        artifactConfiguration.getArtifactResolutionServiceURL());
                artifactCtx.setSourceArtifactResolutionServiceEndpointIndex(
                        artifactConfiguration.getArtifactResolutionServiceIndex());
            }
        }
    }
// Checkstyle: CyclomaticComplexity|MethodLength ON
    
    /**
     * Check for an inbound request binding that is synchronous and handle appropriately.
     * 
     * @param profileRequestContext profile request context
     * 
     * @return  true iff a synchronous binding was handled
     */
    private boolean handleSynchronousRequest(@Nonnull final ProfileRequestContext profileRequestContext) {
        if (inboundMessage != null) {
            final SAMLBindingContext bindingCtx =
                    profileRequestContext.getInboundMessageContext().getSubcontext(SAMLBindingContext.class);
            if (bindingCtx != null && bindingCtx.getBindingUri() != null) {
                final Optional<BindingDescriptor> binding = Iterables.tryFind(bindingDescriptors,
                        new Predicate<BindingDescriptor>() {
                            public boolean apply(BindingDescriptor input) {
                                return input.getId().equals(bindingCtx.getBindingUri());
                            }
                        });
                if (binding.isPresent() && binding.get().isSynchronous()) {
                    log.debug("{} Handling request via synchronous binding, preparing outbound binding context for {}",
                            getLogPrefix(), binding.get().getId());
                    
                    final SAMLBindingContext outboundCtx = bindingContextLookupStrategy.apply(profileRequestContext);
                    outboundCtx.setRelayState(SAMLBindingSupport.getRelayState(
                            profileRequestContext.getInboundMessageContext()));
                    outboundCtx.setBindingUri(binding.get().getId());
                    return true;
                }
            }
        }
        
        return false;
    }

    /**
     * Build a template Endpoint object to use as input criteria to the resolution process.
     * 
     * @return a template for the eventual Endpoint to resolve
     */
    @Nonnull private Endpoint buildTemplateEndpoint() {
        final Endpoint endpoint = (Endpoint) endpointBuilder.buildObject(endpointType);
        
        if (inboundMessage instanceof IdPInitiatedSSORequest) {
            log.debug("{} Populating template endpoint for resolution from IdP-initiated SSO request", getLogPrefix());
            endpoint.setLocation(((IdPInitiatedSSORequest) inboundMessage).getAssertionConsumerServiceURL());
        } else if (inboundMessage instanceof AuthnRequest) {
            log.debug("{} Populating template endpoint for resolution from SAML AuthnRequest", getLogPrefix());
            
            endpoint.setLocation(((AuthnRequest) inboundMessage).getAssertionConsumerServiceURL());
            endpoint.setBinding(((AuthnRequest) inboundMessage).getProtocolBinding());
            if (endpoint instanceof IndexedEndpoint) {
                ((IndexedEndpoint) endpoint).setIndex(
                        ((AuthnRequest) inboundMessage).getAssertionConsumerServiceIndex());
            }
        }
        
        return endpoint;
    }

}