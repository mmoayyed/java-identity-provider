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

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.xml.namespace.QName;

import org.opensaml.core.xml.XMLObjectBuilder;
import org.opensaml.core.xml.util.XMLObjectSupport;
import org.opensaml.messaging.context.MessageChannelSecurityContext;
import org.opensaml.messaging.context.MessageContext;
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
import org.opensaml.saml.criterion.BestMatchLocationCriterion;
import org.opensaml.saml.criterion.BindingCriterion;
import org.opensaml.saml.criterion.EndpointCriterion;
import org.opensaml.saml.criterion.RoleDescriptorCriterion;
import org.opensaml.saml.saml2.core.AuthnRequest;
import org.opensaml.saml.saml2.metadata.Endpoint;
import org.opensaml.saml.saml2.metadata.IndexedEndpoint;
import org.opensaml.saml.saml2.metadata.RoleDescriptor;
import org.slf4j.Logger;

import net.shibboleth.idp.profile.AbstractProfileAction;
import net.shibboleth.idp.saml.profile.config.SAMLProfileConfiguration;
import net.shibboleth.idp.saml.saml2.profile.config.BrowserSSOProfileConfiguration;
import net.shibboleth.profile.context.RelyingPartyContext;
import net.shibboleth.saml.profile.config.SAMLArtifactAwareProfileConfiguration;
import net.shibboleth.saml.profile.config.SAMLArtifactConfiguration;
import net.shibboleth.shared.annotation.constraint.NonnullAfterInit;
import net.shibboleth.shared.annotation.constraint.NonnullElements;
import net.shibboleth.shared.annotation.constraint.NotEmpty;
import net.shibboleth.shared.collection.CollectionSupport;
import net.shibboleth.shared.component.ComponentInitializationException;
import net.shibboleth.shared.logic.Constraint;
import net.shibboleth.shared.logic.FunctionSupport;
import net.shibboleth.shared.primitive.LoggerFactory;
import net.shibboleth.shared.resolver.CriteriaSet;
import net.shibboleth.shared.resolver.ResolverException;

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
 * @event {@link EventIds#PROCEED_EVENT_ID}
 * @event {@link EventIds#INVALID_MSG_CTX}
 * @event {@link SAMLEventIds#ENDPOINT_RESOLUTION_FAILED}
 */
public class PopulateBindingAndEndpointContexts extends AbstractProfileAction {

    /** Class logger. */
    @Nonnull private final Logger log = LoggerFactory.getLogger(PopulateBindingAndEndpointContexts.class);
    
    /** The type of endpoint to resolve. */
    @Nullable private QName endpointType;

    /** Endpoint resolver. */
    @NonnullAfterInit private EndpointResolver<?> endpointResolver;
    
    /** Lookup strategy for bindings. */
    @Nonnull private Function<ProfileRequestContext,List<BindingDescriptor>> bindingDescriptorsLookupStrategy;
    
    /** Strategy function for access to {@link RelyingPartyContext}. */
    @Nonnull private Function<ProfileRequestContext,RelyingPartyContext> relyingPartyContextLookupStrategy;
    
    /** Strategy function for access to {@link SAMLMetadataContext} for input to resolver. */
    @Nonnull private Function<ProfileRequestContext,SAMLMetadataContext> metadataContextLookupStrategy;

    /** Strategy function for access to {@link SAMLBindingContext} to populate. */
    @Nonnull private Function<ProfileRequestContext,SAMLBindingContext> bindingContextLookupStrategy;

    /** Strategy function for access to {@link SAMLEndpointContext} to populate. */
    @Nonnull private Function<ProfileRequestContext,SAMLEndpointContext> endpointContextLookupStrategy;

    /** Strategy function for access to {@link SAMLArtifactContext} to populate. */
    @Nonnull private Function<ProfileRequestContext,SAMLArtifactContext> artifactContextLookupStrategy;
    
    /** Optional strategy function to obtain a {@link BestMatchLocationCriterion} to inject. */
    @Nullable private Function<ProfileRequestContext,BestMatchLocationCriterion> bestMatchCriterionLookupStrategy;
    
    /** List of possible bindings, in preference order. */
    @Nullable @NonnullElements private List<BindingDescriptor> bindingDescriptors;
    
    /** Whether an artifact-based binding implies the use of a secure channel. */
    private boolean artifactImpliesSecureChannel;
    
    /** Builder for template endpoints. */
    @NonnullAfterInit private XMLObjectBuilder<?> endpointBuilder;
    
    /** Artifact configuration. */
    @Nullable private SAMLArtifactConfiguration artifactConfiguration;
    
    /** Optional inbound message. */
    @Nullable private Object inboundMessage;
    
    /** Optional RP name for logging. */
    @Nullable private String relyingPartyId;
    
    /** Optional metadata for use in endpoint derivation/validation. */
    @Nullable private SAMLMetadataContext mdContext;

    /** Is the relying party "verified" in SAML terms? */
    private boolean verified;
    
    /** Whether to bypass endpoint validation because message is signed. */
    private boolean skipValidationSinceSigned;
    
    /** Constructor. */
    public PopulateBindingAndEndpointContexts() {
        bindingDescriptorsLookupStrategy = FunctionSupport.constant(CollectionSupport.emptyList());
        
        relyingPartyContextLookupStrategy = new ChildContextLookup<>(RelyingPartyContext.class);
        
        // Default: outbound msg context -> SAMLPeerEntityContext -> SAMLMetadataContext
        metadataContextLookupStrategy =
                new ChildContextLookup<>(SAMLMetadataContext.class).compose(
                        new ChildContextLookup<>(SAMLPeerEntityContext.class).compose(
                                new OutboundMessageContextLookup()));
        
        // Default: outbound msg context -> SAMLBindingContext
        bindingContextLookupStrategy =
                new ChildContextLookup<>(SAMLBindingContext.class, true).compose(new OutboundMessageContextLookup());

        // Default: outbound msg context -> SAMLArtifactContext
        artifactContextLookupStrategy =
                new ChildContextLookup<>(SAMLArtifactContext.class, true).compose(new OutboundMessageContextLookup());
        
        // Default: outbound msg context -> SAMLPeerEntityContext -> SAMLEndpointContext
        endpointContextLookupStrategy =
                new ChildContextLookup<>(SAMLEndpointContext.class, true).compose(
                        new ChildContextLookup<>(SAMLPeerEntityContext.class, true).compose(
                                new OutboundMessageContextLookup()));
        
        artifactImpliesSecureChannel = true;
    }
    
    /**
     * Set the type of endpoint to resolve, defaults to <code>&lt;AssertionConsumerService&gt;</code>.
     * 
     * @param type  type of endpoint to resolve
     */
    public void setEndpointType(@Nullable final QName type) {
        checkSetterPreconditions();
        endpointType = type;
    }
    
    /**
     * Set a custom {@link EndpointResolver} to use.
     * 
     * @param resolver endpoint resolver to use  
     */
    public void setEndpointResolver(@Nonnull final EndpointResolver<?> resolver) {
        checkSetterPreconditions();
        endpointResolver = Constraint.isNotNull(resolver, "EndpointResolver cannot be null");
    }
    
    /**
     * Set lookup strategy to return the bindings to evaluate for use, in preference order.
     * 
     * @param strategy lookup strategy
     * 
     * @since 4.0.0
     */
    public void setBindingDescriptorsLookupStrategy(
            @Nonnull final Function<ProfileRequestContext,List<BindingDescriptor>> strategy) {
        checkSetterPreconditions();
        bindingDescriptorsLookupStrategy =
                Constraint.isNotNull(strategy, "Binding descriptors lookup strategy cannot be null");
    }
    
    /**
     * Set lookup strategy for {@link RelyingPartyContext}.
     * 
     * @param strategy  lookup strategy
     */
    public void setRelyingPartyContextLookupStrategy(
            @Nonnull final Function<ProfileRequestContext,RelyingPartyContext> strategy) {
        checkSetterPreconditions();
        relyingPartyContextLookupStrategy = Constraint.isNotNull(strategy,
                "RelyingPartyContext lookup strategy cannot be null");
    }
    
    /**
     * Set lookup strategy for {@link SAMLMetadataContext} for input to resolution.
     * 
     * @param strategy  lookup strategy
     */
    public void setMetadataContextLookupStrategy(
            @Nonnull final Function<ProfileRequestContext,SAMLMetadataContext> strategy) {
        checkSetterPreconditions();
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
        checkSetterPreconditions();
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
        checkSetterPreconditions();
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
        checkSetterPreconditions();
        artifactContextLookupStrategy = Constraint.isNotNull(strategy,
                "SAMLArtifactContext lookup strategy cannot be null");
    }
    
    /**
     * Set lookup strategy for {@link BestMatchLocationCriterion} to inject.
     * 
     * @param strategy lookup strategy
     */
    public void setBestMatchCriterionLookupStrategy(
            @Nullable final Function<ProfileRequestContext,BestMatchLocationCriterion> strategy) {
        checkSetterPreconditions();
        bestMatchCriterionLookupStrategy = strategy;
    }
    
    /**
     * Set whether an artifact-based binding implies that the eventual channel for SAML message exchange
     * will be secured, overriding the integrity and confidentiality properties of the current channel.
     * 
     * <p>This has the effect of suppressing signing and encryption when an artifact binding is used,
     * which is normally desirable.</p>
     * 
     * <p>Defaults to true.</p>
     * 
     * @param flag flag to set
     */
    public void setArtifactImpliesSecureChannel(final boolean flag) {
        checkSetterPreconditions();
        artifactImpliesSecureChannel = flag;
    }
    
    /** {@inheritDoc} */
    @Override
    protected void doInitialize() throws ComponentInitializationException {
        super.doInitialize();

        if (endpointResolver == null) {
            throw new ComponentInitializationException("EndpointResolver cannot be null");
        }
        
        final QName et = endpointType;
        if (et != null) {
            endpointBuilder = XMLObjectSupport.getBuilder(endpointType);
            if (endpointBuilder == null) {
                throw new ComponentInitializationException("Unable to obtain builder for endpoint type "
                        + et);
            } else if (!(endpointBuilder.buildObject(et) instanceof Endpoint)) {
                throw new ComponentInitializationException("Builder for endpoint type " + endpointType
                        + " did not result in Endpoint object");
            }
        }
    }
    
 // Checkstyle: CyclomaticComplexity|MethodLength OFF
    /** {@inheritDoc} */
    @Override
    protected boolean doPreExecute(@Nonnull final ProfileRequestContext profileRequestContext) {
        
        if (!super.doPreExecute(profileRequestContext)) {
            return false;
        }
        
        final MessageContext imc = profileRequestContext.getInboundMessageContext();
        if (imc != null) {
            inboundMessage = imc.getMessage();
        }
        
        final RelyingPartyContext rpContext = relyingPartyContextLookupStrategy.apply(profileRequestContext);
        if (rpContext != null) {
            relyingPartyId = rpContext.getRelyingPartyId();
            verified = rpContext.isVerified();
            if (rpContext.getProfileConfig() != null
                    && rpContext.getProfileConfig() instanceof SAMLProfileConfiguration) {
                final SAMLProfileConfiguration profileConfiguration =
                        (SAMLProfileConfiguration) rpContext.getProfileConfig();
                if (profileConfiguration instanceof SAMLArtifactAwareProfileConfiguration) {
                    artifactConfiguration =
                            ((SAMLArtifactAwareProfileConfiguration) profileConfiguration).getArtifactConfiguration(
                                    profileRequestContext);
                }
                if (profileConfiguration instanceof BrowserSSOProfileConfiguration) {
                    final BrowserSSOProfileConfiguration ssoConfig =
                            (BrowserSSOProfileConfiguration) profileConfiguration;
                    skipValidationSinceSigned =
                            inboundMessage instanceof AuthnRequest
                            && ssoConfig.isSkipEndpointValidationWhenSigned(profileRequestContext)
                            && !ssoConfig.isIgnoreRequestSignatures(profileRequestContext)
                            && SAMLBindingSupport.isMessageSigned(Constraint.isNotNull(imc, "No Inboud Message Context")); 
                }
            }
        }
        
        if (profileRequestContext.getOutboundMessageContext() == null) {
            log.debug("{} No outbound message context", getLogPrefix());
            ActionSupport.buildEvent(profileRequestContext, EventIds.INVALID_MSG_CTX);
            return false;
        }
       
        bindingDescriptors = bindingDescriptorsLookupStrategy.apply(profileRequestContext);
        if (bindingDescriptors == null) {
            bindingDescriptors = CollectionSupport.emptyList();
        }
        
        mdContext = metadataContextLookupStrategy.apply(profileRequestContext);
        
        return true;
    }

//CheckStyle: ReturnCount OFF
    /** {@inheritDoc} */
    @Override protected void doExecute(@Nonnull final ProfileRequestContext profileRequestContext) {
        
        if (handleSynchronousRequest(profileRequestContext)) {
            return;
        } else if (endpointType == null) {
            log.error("Front-channel binding used, but no endpoint type set");
            ActionSupport.buildEvent(profileRequestContext, SAMLEventIds.ENDPOINT_RESOLUTION_FAILED);
            return;
        }
        
        log.debug("{} Attempting to resolve endpoint of type {} for outbound message", getLogPrefix(), endpointType);

        // Compile binding list.  binding descriptors were checked for being non null in pre
        final List<BindingDescriptor> bds = bindingDescriptors;
        assert bds != null; 
        @Nonnull final List<String> bindings = new ArrayList<>(bds.size());
        for (final BindingDescriptor bindingDescriptor : bds) {
            if (bindingDescriptor.test(profileRequestContext)) {
                bindings.add(bindingDescriptor.getId());
            }
        }
        if (bindings.isEmpty()) {
            log.warn("{} No outbound bindings are eligible for use", getLogPrefix());
            ActionSupport.buildEvent(profileRequestContext, SAMLEventIds.ENDPOINT_RESOLUTION_FAILED);
            return;
        }
        
        log.trace("{} Candidate outbound bindings: {}", getLogPrefix(), bindings);
        
        // Build criteria for the resolver.
        final CriteriaSet criteria = new CriteriaSet(new BindingCriterion(bindings),
                buildEndpointCriterion(bindings.get(0)));
        
        if (bestMatchCriterionLookupStrategy != null) {
            final BestMatchLocationCriterion bestMatch = bestMatchCriterionLookupStrategy.apply(profileRequestContext);
            if (bestMatch != null) {
                criteria.add(bestMatch);
            }
        }
        
        final RoleDescriptor rdc;
        if (mdContext != null) {
            rdc =  mdContext.getRoleDescriptor();
        } else {
            rdc = null;
        }
        if (rdc != null) {
            criteria.add(new RoleDescriptorCriterion(rdc));
        } else {
            log.debug("{} No metadata available for endpoint resolution", getLogPrefix());
        }
        
        // Attempt resolution.
        Endpoint resolvedEndpoint = null;
        try {
            resolvedEndpoint = endpointResolver.resolveSingle(criteria);
        } catch (final ResolverException e) {
            log.error("{} Error resolving outbound message endpoint", getLogPrefix(), e);
        }
        
        if (resolvedEndpoint == null) {
            log.warn("{} Unable to resolve outbound message endpoint for relying party '{}': {}",
                    getLogPrefix(), relyingPartyId, criteria.get(EndpointCriterion.class));
            ActionSupport.buildEvent(profileRequestContext, SAMLEventIds.ENDPOINT_RESOLUTION_FAILED);
            return;
        }
        
        final String bindingURI = resolvedEndpoint.getBinding();
        
        log.debug("{} Resolved endpoint at location {} using binding {}",
                new Object[] {getLogPrefix(), resolvedEndpoint.getLocation(), bindingURI,});
        
        // Transfer results to contexts.
        
        final SAMLEndpointContext endpointContext = endpointContextLookupStrategy.apply(profileRequestContext);
        endpointContext.setEndpoint(resolvedEndpoint);
        
        final SAMLBindingContext bindingCtx = bindingContextLookupStrategy.apply(profileRequestContext);
        final MessageContext imc = profileRequestContext.getInboundMessageContext();
        assert imc != null;
        bindingCtx.setRelayState(SAMLBindingSupport.getRelayState(imc));
        
        final Optional<BindingDescriptor> bindingDescriptor =
                bds.stream().filter(b -> b.getId().equals(bindingURI)).findFirst();

        if (bindingDescriptor.isPresent()) {
            bindingCtx.setBindingDescriptor(bindingDescriptor.orElseThrow());
        } else {
            bindingCtx.setBindingUri(resolvedEndpoint.getBinding());
        }
        
        // Handle artifact details.
        if (bindingDescriptor.isPresent() && bindingDescriptor.get().isArtifact()) {
            final SAMLArtifactConfiguration artifactCfg = artifactConfiguration;
            if (artifactCfg != null) {
                final SAMLArtifactContext artifactCtx = artifactContextLookupStrategy.apply(profileRequestContext);
                artifactCtx.setArtifactType(artifactCfg.getArtifactType());
                artifactCtx.setSourceArtifactResolutionServiceEndpointURL(
                        artifactCfg.getArtifactResolutionServiceURL());
                artifactCtx.setSourceArtifactResolutionServiceEndpointIndex(
                        artifactCfg.getArtifactResolutionServiceIndex());
            }
            
            if (artifactImpliesSecureChannel) {
                log.debug("{} Use of artifact binding implies the channel will be secure, "
                        + "overriding MessageChannelSecurityContext flags", getLogPrefix());
                final MessageChannelSecurityContext channelCtx =
                        profileRequestContext.getOrCreateSubcontext(MessageChannelSecurityContext.class);
                channelCtx.setIntegrityActive(true);
                channelCtx.setConfidentialityActive(true);
            }            
        }
    }
// Checkstyle: CyclomaticComplexity|MethodLength|ReturnCount ON
    
    /**
     * Check for an inbound request binding that is synchronous and handle appropriately.
     * 
     * @param profileRequestContext profile request context
     * 
     * @return  true iff a synchronous binding was handled
     */
    private boolean handleSynchronousRequest(@Nonnull final ProfileRequestContext profileRequestContext) {
        if (inboundMessage != null) {
            final MessageContext imc = profileRequestContext.getInboundMessageContext();
            assert imc != null;
            final SAMLBindingContext bindingCtx = imc.getSubcontext(SAMLBindingContext.class);
            if (bindingCtx != null && bindingCtx.getBindingUri() != null) {
                assert bindingDescriptors!=null;
                final Optional<BindingDescriptor> binding =
                        bindingDescriptors.stream().filter(
                                b -> b.getId().equals(bindingCtx.getBindingUri())
                                ).findFirst();
                if (binding.isPresent() && binding.orElseThrow().isSynchronous()) {
                    log.debug("{} Handling request via synchronous binding, preparing outbound binding context for {}",
                            getLogPrefix(), binding.orElseThrow().getId());
                    
                    final SAMLBindingContext outboundCtx = bindingContextLookupStrategy.apply(profileRequestContext);
                    outboundCtx.setRelayState(SAMLBindingSupport.getRelayState(imc));
                    outboundCtx.setBindingDescriptor(binding.orElseThrow());
                    return true;
                }
            }
        }
        
        return false;
    }

    /**
     * Build a template Endpoint object to use as input criteria to the resolution process and wrap it in
     * a criterion object.
     * 
     * @param unverifiedBinding default binding to use for an unverified requester with no Binding specified
     * 
     * @return criterion to give to resolver
     */
    @Nonnull private EndpointCriterion<?> buildEndpointCriterion(@Nonnull @NotEmpty final String unverifiedBinding) {
        assert endpointType!=null;
        final Endpoint endpoint = (Endpoint) endpointBuilder.buildObject(endpointType);
        
        final Object inbound = inboundMessage;
        if (inbound instanceof IdPInitiatedSSORequest) {
            log.debug("{} Populating template endpoint for resolution from IdP-initiated SSO request", getLogPrefix());
            endpoint.setLocation(((IdPInitiatedSSORequest) inbound).getAssertionConsumerServiceURL());
        } else if (inbound instanceof AuthnRequest) {
            log.debug("{} Populating template endpoint for resolution from SAML AuthnRequest", getLogPrefix());
            
            endpoint.setLocation(((AuthnRequest) inbound).getAssertionConsumerServiceURL());
            endpoint.setBinding(((AuthnRequest) inbound).getProtocolBinding());
            if (endpoint instanceof IndexedEndpoint) {
                ((IndexedEndpoint) endpoint).setIndex(
                        ((AuthnRequest) inbound).getAssertionConsumerServiceIndex());
            }
        }

        if (!verified) {
            // This is a bit paradoxical, but for an unverified request, we actually "trust" the endpoint
            // implicitly, because if we didn't, we'd have no way to validate it. We'd only get this far if
            // the profile is explicitly enabled for anonymous use, and once you do that, there's no verification
            // in practical terms because any SP can simply pretend to be any name it likes. In fact, the
            // metadata you do have becomes the set of names the SP can't pretend to be since those names
            // would lead to verification.
            if (endpoint.getBinding() == null) {
                endpoint.setBinding(unverifiedBinding);
                log.debug("{} Defaulting binding in \"unverified\" request to {}", getLogPrefix(), unverifiedBinding);
            }
            return new EndpointCriterion<>(endpoint, true);
        }
        
        // Here we only skip endpoint validation if the skip flag has been set.
        return new EndpointCriterion<>(endpoint, skipValidationSinceSigned);
    }
    
}