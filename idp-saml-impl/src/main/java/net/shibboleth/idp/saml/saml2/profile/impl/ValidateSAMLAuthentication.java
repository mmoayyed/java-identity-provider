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

package net.shibboleth.idp.saml.saml2.profile.impl;

import java.util.Collection;
import java.util.function.Function;
import java.util.stream.Collectors;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.security.auth.Subject;

import net.shibboleth.idp.attribute.AttributeDecodingException;
import net.shibboleth.idp.attribute.IdPAttribute;
import net.shibboleth.idp.attribute.context.AttributeContext;
import net.shibboleth.idp.attribute.filter.AttributeFilter;
import net.shibboleth.idp.attribute.filter.AttributeFilterException;
import net.shibboleth.idp.attribute.filter.context.AttributeFilterContext;
import net.shibboleth.idp.attribute.filter.context.AttributeFilterContext.Direction;
import net.shibboleth.idp.attribute.transcoding.AttributeTranscoder;
import net.shibboleth.idp.attribute.transcoding.AttributeTranscoderRegistry;
import net.shibboleth.idp.attribute.transcoding.TranscoderSupport;
import net.shibboleth.idp.attribute.transcoding.TranscodingRule;
import net.shibboleth.idp.authn.AbstractValidationAction;
import net.shibboleth.idp.authn.AuthnEventIds;
import net.shibboleth.idp.authn.context.AuthenticationContext;
import net.shibboleth.idp.authn.principal.IdPAttributePrincipal;
import net.shibboleth.idp.authn.principal.ProxyAuthenticationPrincipal;
import net.shibboleth.idp.profile.context.navigate.ResponderIdLookupFunction;
import net.shibboleth.idp.saml.profile.context.navigate.SAMLMetadataContextLookupFunction;
import net.shibboleth.utilities.java.support.annotation.constraint.Live;
import net.shibboleth.utilities.java.support.annotation.constraint.NonnullElements;
import net.shibboleth.utilities.java.support.annotation.constraint.NotEmpty;
import net.shibboleth.utilities.java.support.component.ComponentSupport;
import net.shibboleth.utilities.java.support.service.ReloadableService;
import net.shibboleth.utilities.java.support.service.ServiceableComponent;

import org.opensaml.messaging.context.navigate.RecursiveTypedParentContextLookup;
import org.opensaml.profile.action.ActionSupport;
import org.opensaml.profile.action.EventIds;
import org.opensaml.profile.context.ProfileRequestContext;
import org.opensaml.saml.metadata.resolver.MetadataResolver;
import org.opensaml.saml.saml2.core.Assertion;
import org.opensaml.saml.saml2.core.Attribute;
import org.opensaml.saml.saml2.core.AttributeStatement;
import org.opensaml.saml.saml2.core.AuthenticatingAuthority;
import org.opensaml.saml.saml2.core.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Strings;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;

/**
 * An action that produces an {@link net.shibboleth.idp.authn.AuthenticationResult} based on an inbound
 * SAML 2.0 SSO response.
 * 
 * <p>A {@link SAMLAuthnContext} is used as the basis of the result and the lack of a context is a signal
 * to record a failure. Actual validation is all upstream of this action, but the use of the ValidationAction
 * subclass is a convenience for auditing and handling the result.</p>
 *  
 * @event {@link EventIds#PROCEED_EVENT_ID}
 * @event {@link EventIds#INVALID_PROFILE_CTX}
 * @pre <pre>ProfileRequestContext.getSubcontext(AuthenticationContext.class).getAttemptedFlow() != null</pre>
 * @post If AuthenticationContext.getSubcontext(SAMLAuthnContext.class) != null, then
 * an {@link net.shibboleth.idp.authn.AuthenticationResult} is saved to the {@link AuthenticationContext}.
 */
public class ValidateSAMLAuthentication extends AbstractValidationAction {

    /** Default prefix for metrics. */
    @Nonnull @NotEmpty private static final String DEFAULT_METRIC_NAME = "net.shibboleth.idp.authn.saml"; 

    /** Class logger. */
    @Nonnull private final Logger log = LoggerFactory.getLogger(ValidateSAMLAuthentication.class);

    /** Transcoder registry service object. */
    @Nullable private ReloadableService<AttributeTranscoderRegistry> transcoderRegistry;

    /** Service used to get the engine used to filter attributes. */
    @Nullable private ReloadableService<AttributeFilter> attributeFilterService;

    /** Strategy used to obtain our identity. */
    @Nullable private Function<ProfileRequestContext,String> recipientIdLookupStrategy;
    
    /** Optional supplemental metadata source for filtering. */
    @Nullable private MetadataResolver metadataResolver;
    
    /** Context containing the result to validate. */
    @Nullable private SAMLAuthnContext samlAuthnContext;
    
    /** Free-standing context for externally supplied inbound attributes. */
    @Nullable private AttributeContext attributeContext;
        
    /** Constructor. */
    public ValidateSAMLAuthentication() {
        setMetricName(DEFAULT_METRIC_NAME);
        recipientIdLookupStrategy = new ResponderIdLookupFunction();
    }

    /**
     * Sets the registry of transcoding rules to apply to encode attributes.
     * 
     * @param registry registry service interface
     */
    public void setTranscoderRegistry(@Nullable final ReloadableService<AttributeTranscoderRegistry> registry) {
        ComponentSupport.ifInitializedThrowUnmodifiabledComponentException(this);
        
        transcoderRegistry = registry;
    }
    

    /**
     * Sets the filter service to use for inbound attributes.
     *
     * @param filterService optional filter service for inbound attributes
     */
    public void setAttributeFilter(@Nullable final ReloadableService<AttributeFilter> filterService) {
        ComponentSupport.ifInitializedThrowUnmodifiabledComponentException(this);

        attributeFilterService = filterService;
    }
    
    /**
     * Set a metadata source to use during filtering.
     * 
     * @param resolver metadata resolver
     */
    public void setMetadataResolver(@Nullable final MetadataResolver resolver) {
        ComponentSupport.ifInitializedThrowUnmodifiabledComponentException(this);
        
        metadataResolver = resolver;
    }
    
    /**
     * Set the strategy used to locate the attribute recipient value to use.
     * 
     * @param strategy lookup strategy
     */
    public void setRecipientIdLookupStrategy(@Nullable final Function<ProfileRequestContext,String> strategy) {
        ComponentSupport.ifInitializedThrowUnmodifiabledComponentException(this);

        recipientIdLookupStrategy = strategy;
    }

    /** {@inheritDoc} */
    @Override
    protected boolean doPreExecute(@Nonnull final ProfileRequestContext profileRequestContext,
            @Nonnull final AuthenticationContext authenticationContext) {
        
        if (!super.doPreExecute(profileRequestContext, authenticationContext)) {
            return false;
        }
        
        if (authenticationContext.getAttemptedFlow() == null) {
            log.debug("{} No attempted flow within authentication context", getLogPrefix());
            ActionSupport.buildEvent(profileRequestContext, EventIds.INVALID_PROFILE_CTX);
            recordFailure();
            return false;
        }
        
        samlAuthnContext = authenticationContext.getSubcontext(SAMLAuthnContext.class);
        if (samlAuthnContext == null) {
            log.debug("{} No SAMLAuthnContext available within authentication context", getLogPrefix());
            ActionSupport.buildEvent(profileRequestContext, AuthnEventIds.INVALID_CREDENTIALS);
            recordFailure();
            return false;
        }
        
        return true;
    }
    
    /** {@inheritDoc} */
    @Override
    protected void doExecute(@Nonnull final ProfileRequestContext profileRequestContext,
            @Nonnull final AuthenticationContext authenticationContext) {

        recordSuccess();
        
        if (transcoderRegistry != null) {
            processAttributes(profileRequestContext);
        }
        
        buildAuthenticationResult(profileRequestContext, authenticationContext);
        
        if (authenticationContext.getAuthenticationResult() != null) {
            if (samlAuthnContext.getAuthnStatement().getAuthnInstant() != null) {
                authenticationContext.getAuthenticationResult().setAuthenticationInstant(
                        samlAuthnContext.getAuthnStatement().getAuthnInstant());
            }
        }
    }
    
    /** {@inheritDoc} */
    @Override
    @Nonnull protected Subject populateSubject(@Nonnull final Subject subject) {
        
        final Collection<AuthenticatingAuthority> authorities =
                samlAuthnContext.getAuthnStatement().getAuthnContext().getAuthenticatingAuthorities();
        if (!authorities.isEmpty()) {
            final ProxyAuthenticationPrincipal proxied = new ProxyAuthenticationPrincipal(
                    authorities
                        .stream()
                        .filter(aa -> !Strings.isNullOrEmpty(aa.getURI()))
                        .map(AuthenticatingAuthority::getURI)
                        .collect(Collectors.toUnmodifiableList()));
            subject.getPrincipals().add(proxied);
        }
        
        if (attributeContext != null && !attributeContext.getIdPAttributes().isEmpty()) {
            log.debug("{} Adding filtered inbound attributes to Subject", getLogPrefix());
            subject.getPrincipals().addAll(
                attributeContext.getIdPAttributes().values()
                    .stream()
                    .map(a -> new IdPAttributePrincipal(a))
                    .collect(Collectors.toUnmodifiableList()));
        }
        
        return subject;
    }
    
    /**
     * Process the inbound SAML Attributes.
     * 
     * @param profileRequestContext current profile request context
     */
    private void processAttributes(@Nonnull final ProfileRequestContext profileRequestContext) {
        
        log.debug("{} Decoding incoming SAML Attributes", getLogPrefix());
        
        final Multimap<String,IdPAttribute> mapped = HashMultimap.create();

        ServiceableComponent<AttributeTranscoderRegistry> component = null;
        try {
            component = transcoderRegistry.getServiceableComponent();
            if (component == null) {
                log.error("Attribute transcoder service unavailable");
                return;
            }

            final Response response = (Response) profileRequestContext.getInboundMessageContext().getMessage();
            for (final Assertion assertion : response.getAssertions()) {
                for (final AttributeStatement statement : assertion.getAttributeStatements()) {
                    for (final Attribute designator : statement.getAttributes()) {
                        try {
                            decodeAttribute(component.getComponent(), profileRequestContext, designator, mapped);
                        } catch (final AttributeDecodingException e) {
                            log.error("{} Error decoding inbound Attribute", getLogPrefix(), e);
                        }
                    }
                }
            }
        } finally {
            if (component != null) {
                component.unpinComponent();
            }
        }
                
        log.debug("{} Incoming SAML Attributes mapped to attribute IDs: {}", getLogPrefix(), mapped.keySet());
        
        if (!mapped.isEmpty()) {
            attributeContext = new AttributeContext();
            attributeContext.setUnfilteredIdPAttributes(mapped.values());
            filterAttributes(profileRequestContext);
        }
    }
    
    /**
     * Access the registry of transcoding rules to decode the input {@link Attribute}.
     * 
     * @param registry  registry of transcoding rules
     * @param profileRequestContext current profile request context
     * @param input input object
     * @param results collection to add results to
     * 
     * @throws AttributeDecodingException if an error occurs or no results were obtained
     */
    private void decodeAttribute(@Nonnull final AttributeTranscoderRegistry registry,
            @Nonnull final ProfileRequestContext profileRequestContext, @Nonnull final Attribute input,
            @Nonnull @NonnullElements @Live final Multimap<String,IdPAttribute> results)
                    throws AttributeDecodingException {
        
        final Collection<TranscodingRule> transcodingRules = registry.getTranscodingRules(input);
        if (transcodingRules.isEmpty()) {
            log.info("{} No transcoding rule for Attribute '{}'", getLogPrefix(), input.getName());
            return;
        }
        
        for (final TranscodingRule rules : transcodingRules) {
            final AttributeTranscoder<Attribute> transcoder = TranscoderSupport.getTranscoder(rules);
            final IdPAttribute decodedAttribute = transcoder.decode(profileRequestContext, input, rules);
            if (decodedAttribute != null) {
                results.put(decodedAttribute.getId(), decodedAttribute);
            }
        }
    }
    
    /**
     * Check for inbound attributes and apply filtering.
     * 
     * @param profileRequestContext current profile request context
     */
    private void filterAttributes(@Nonnull final ProfileRequestContext profileRequestContext) {
        if (attributeFilterService == null) {
            log.warn("{} No AttributeFilter service provided", getLogPrefix());
            attributeContext.setIdPAttributes(null);
            return;
        }
        
        
        final AttributeFilterContext filterContext = samlAuthnContext.getSubcontext(AttributeFilterContext.class, true);
        
        populateFilterContext(profileRequestContext, filterContext);
        
        ServiceableComponent<AttributeFilter> component = null;

        try {
            component = attributeFilterService.getServiceableComponent();
            if (null == component) {
                log.error("{} Error while filtering inbound attributes: Invalid Attribute Filter configuration",
                        getLogPrefix());
                attributeContext.setIdPAttributes(null);
            } else {
                final AttributeFilter filter = component.getComponent();
                filter.filterAttributes(filterContext);
                filterContext.getParent().removeSubcontext(filterContext);
                attributeContext.setIdPAttributes(filterContext.getFilteredIdPAttributes().values());
            }
        } catch (final AttributeFilterException e) {
            log.error("{} Error while filtering inbound attributes", getLogPrefix(), e);
            attributeContext.setIdPAttributes(null);
        } finally {
            if (null != component) {
                component.unpinComponent();
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
        
        filterContext.setDirection(Direction.INBOUND)
            .setPrefilteredIdPAttributes(attributeContext.getUnfilteredIdPAttributes().values())
            .setMetadataResolver(metadataResolver)
            .setRequesterMetadataContextLookupStrategy(null)
            .setIssuerMetadataContextLookupStrategy(
                    new SAMLMetadataContextLookupFunction().compose(
                            new RecursiveTypedParentContextLookup<>(ProfileRequestContext.class)))
            .setProxiedRequesterContextLookupStrategy(null)
            .setAttributeIssuerID(
                    ((Assertion) samlAuthnContext.getAuthnStatement().getParent()).getIssuer().getValue())
            .setAttributeRecipientID(recipientIdLookupStrategy.apply(profileRequestContext));
    }

}