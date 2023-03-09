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

import java.security.Principal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.security.auth.Subject;

import org.opensaml.messaging.context.BaseContext;
import org.opensaml.messaging.context.MessageContext;
import org.opensaml.messaging.context.navigate.ChildContextLookup;
import org.opensaml.messaging.context.navigate.RecursiveTypedParentContextLookup;
import org.opensaml.profile.action.ActionSupport;
import org.opensaml.profile.action.EventIds;
import org.opensaml.profile.context.ProfileRequestContext;
import org.opensaml.saml.metadata.resolver.MetadataResolver;
import org.opensaml.saml.saml2.core.Assertion;
import org.opensaml.saml.saml2.core.Attribute;
import org.opensaml.saml.saml2.core.AttributeStatement;
import org.opensaml.saml.saml2.core.Audience;
import org.opensaml.saml.saml2.core.AuthenticatingAuthority;
import org.opensaml.saml.saml2.core.AuthnContext;
import org.opensaml.saml.saml2.core.AuthnStatement;
import org.opensaml.saml.saml2.core.NameID;
import org.opensaml.saml.saml2.core.ProxyRestriction;
import org.opensaml.saml.saml2.core.Response;
import org.slf4j.Logger;

import com.google.common.base.Strings;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;

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
import net.shibboleth.idp.authn.AuthenticationResult;
import net.shibboleth.idp.authn.AuthnEventIds;
import net.shibboleth.idp.authn.context.AuthenticationContext;
import net.shibboleth.idp.authn.principal.IdPAttributePrincipal;
import net.shibboleth.idp.authn.principal.ProxyAuthenticationPrincipal;
import net.shibboleth.idp.profile.IdPEventIds;
import net.shibboleth.idp.saml.authn.principal.AuthnContextClassRefPrincipal;
import net.shibboleth.idp.saml.authn.principal.AuthnContextDeclRefPrincipal;
import net.shibboleth.idp.saml.authn.principal.NameIDPrincipal;
import net.shibboleth.idp.saml.saml2.profile.config.BrowserSSOProfileConfiguration;
import net.shibboleth.profile.context.RelyingPartyContext;
import net.shibboleth.saml.profile.context.navigate.SAMLMetadataContextLookupFunction;
import net.shibboleth.shared.annotation.constraint.Live;
import net.shibboleth.shared.annotation.constraint.NonnullElements;
import net.shibboleth.shared.annotation.constraint.NotEmpty;
import net.shibboleth.shared.logic.Constraint;
import net.shibboleth.shared.primitive.LoggerFactory;
import net.shibboleth.shared.primitive.StringSupport;
import net.shibboleth.shared.service.ReloadableService;
import net.shibboleth.shared.service.ServiceException;
import net.shibboleth.shared.service.ServiceableComponent;

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
 * @event {@link IdPEventIds#INVALID_RELYING_PARTY_CTX}
 * @event {@link IdPEventIds#INVALID_PROFILE_CONFIG}
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

    /** Optional supplemental metadata source for filtering. */
    @Nullable private MetadataResolver metadataResolver;

    /** Strategy used to look up a {@link RelyingPartyContext} for configuration options. */
    @Nonnull private Function<ProfileRequestContext,RelyingPartyContext> relyingPartyContextLookupStrategy;

    /** Pluggable strategy function for generalized extraction of data. */
    @Nullable private Function<ProfileRequestContext,Collection<IdPAttribute>> attributeExtractionStrategy;

    /** An IdPAttribute ID to log as a "name" in place of the NameID for "info" purposes. */
    @Nullable @NotEmpty private String loggedAttributeId;

    /** Context containing the result to validate. */
    @Nullable private SAMLAuthnContext samlAuthnContext;
    
    /** Store off profile config. */
    @Nullable private BrowserSSOProfileConfiguration profileConfiguration;
    
    /** Incoming context translation function. */
    @Nullable private Function<AuthnContext,Collection<Principal>> authnContextTranslator;

    /** Incoming context extended translation function. */
    @Nullable private Function<ProfileRequestContext,Collection<Principal>> authnContextTranslatorEx;
    
    /** Context for externally supplied inbound attributes. */
    @Nullable private AttributeContext attributeContext;
        
    /** Constructor. */
    public ValidateSAMLAuthentication() {
        setMetricName(DEFAULT_METRIC_NAME);
        relyingPartyContextLookupStrategy = new ChildContextLookup<>(RelyingPartyContext.class);
    }

    /**
     * Sets the registry of transcoding rules to apply to encode attributes.
     * 
     * @param registry registry service interface
     */
    public void setTranscoderRegistry(@Nullable final ReloadableService<AttributeTranscoderRegistry> registry) {
        checkSetterPreconditions();
        transcoderRegistry = registry;
    }
    
    /**
     * Sets the filter service to use for inbound attributes.
     *
     * @param filterService optional filter service for inbound attributes
     */
    public void setAttributeFilter(@Nullable final ReloadableService<AttributeFilter> filterService) {
        checkSetterPreconditions();
        attributeFilterService = filterService;
    }
    
    /**
     * Set a metadata source to use during filtering.
     * 
     * @param resolver metadata resolver
     */
    public void setMetadataResolver(@Nullable final MetadataResolver resolver) {
        checkSetterPreconditions();
        metadataResolver = resolver;
    }
    
    /**
     * Set the strategy used to return the {@link RelyingPartyContext} for configuration options.
     * 
     * @param strategy lookup strategy
     */
    public void setRelyingPartyContextLookupStrategy(
            @Nonnull final Function<ProfileRequestContext,RelyingPartyContext> strategy) {
        checkSetterPreconditions();
        relyingPartyContextLookupStrategy =
                Constraint.isNotNull(strategy, "RelyingPartyContext lookup strategy cannot be null");
    }
    
    /**
     * Sets the strategy function to invoke for generalized extraction of data into
     * {@link IdPAttribute} objects for inclusion in the
     * {@link net.shibboleth.idp.authn.AuthenticationResult}.
     * 
     * @param strategy extraction strategy
     */
    public void setAttributeExtractionStrategy(
            @Nullable final Function<ProfileRequestContext,Collection<IdPAttribute>> strategy) {
        checkSetterPreconditions();
        attributeExtractionStrategy = strategy;
    }
    
    /**
     * An attribute ID to pull a "name" from for logging purposes.
     * 
     * @param id attribute ID
     * 
     * @since 4.2.0
     */
    public void setLoggedAttributeId(@Nullable @NotEmpty final String id) {
        checkSetterPreconditions();
        loggedAttributeId = StringSupport.trimOrNull(id);
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
            return false;
        }
        
        samlAuthnContext = authenticationContext.getSubcontext(SAMLAuthnContext.class);
        if (samlAuthnContext == null) {
            log.debug("{} No SAMLAuthnContext available within authentication context", getLogPrefix());
            ActionSupport.buildEvent(profileRequestContext, AuthnEventIds.INVALID_CREDENTIALS);
            return false;
        }

        final RelyingPartyContext rpContext = relyingPartyContextLookupStrategy.apply(profileRequestContext);
        if (rpContext == null) {
            log.error("{} Unable to locate RelyingPartyContext", getLogPrefix());
            ActionSupport.buildEvent(profileRequestContext, IdPEventIds.INVALID_RELYING_PARTY_CTX);
            return false;
        } else if (rpContext.getProfileConfig() == null) {
            log.error("{} Unable to locate profile configuration", getLogPrefix());
            ActionSupport.buildEvent(profileRequestContext, IdPEventIds.INVALID_PROFILE_CONFIG);
            return false;
        } else if (!(rpContext.getProfileConfig() instanceof BrowserSSOProfileConfiguration)) {
            log.error("{} Not a SAML 2 profile configuration", getLogPrefix());
            ActionSupport.buildEvent(profileRequestContext, IdPEventIds.INVALID_PROFILE_CONFIG);
            return false;
        }
        
        profileConfiguration = (BrowserSSOProfileConfiguration) rpContext.getProfileConfig();
        
        return true;
    }
    
    /** {@inheritDoc} */
    @Override
    protected void doExecute(@Nonnull final ProfileRequestContext profileRequestContext,
            @Nonnull final AuthenticationContext authenticationContext) {

        recordSuccess(profileRequestContext);
        
        if (transcoderRegistry != null) {
            processAttributes(profileRequestContext);
        }
        final Function<ProfileRequestContext,Collection<IdPAttribute>> aes = attributeExtractionStrategy;
        AttributeContext ac = attributeContext;
        
        if (aes != null) {
            log.debug("{} Applying custom extraction strategy function", getLogPrefix());
            if (ac == null) {
                final RelyingPartyContext rpcCtx = profileRequestContext.getSubcontext(RelyingPartyContext.class);
                assert rpcCtx!= null;
                ac = attributeContext = rpcCtx.getOrCreateSubcontext(AttributeContext.class);
            }
            final Collection<IdPAttribute> attributes = new ArrayList<>(ac.getIdPAttributes().values());
            final Collection<IdPAttribute> newAttributes = aes.apply(profileRequestContext);
            if (newAttributes != null) {
                if (log.isDebugEnabled()) {
                    log.debug("{} Extracted attributes with custom strategy: {}", getLogPrefix(),
                            newAttributes.stream().map(IdPAttribute::getId).collect(Collectors.toUnmodifiableList()));
                }
                attributes.addAll(newAttributes);
                ac.setIdPAttributes(attributes);
            }
        }

        logSuccess();
        
        final BrowserSSOProfileConfiguration prConfig = profileConfiguration;
        assert prConfig!=null;
        authnContextTranslator = prConfig.getAuthnContextTranslationStrategy(profileRequestContext);
        authnContextTranslatorEx = prConfig.getAuthnContextTranslationStrategyEx(profileRequestContext);
        
        buildAuthenticationResult(profileRequestContext, authenticationContext);
        final AuthenticationResult ar = authenticationContext.getAuthenticationResult();
        if (ar != null && prConfig.isProxiedAuthnInstant(profileRequestContext)) {
            assert samlAuthnContext != null;
            final AuthnStatement as = samlAuthnContext.getAuthnStatement();
            assert as != null;
            final Instant ai = as.getAuthnInstant();
            assert ai != null;
            log.debug("{} Resetting authentication time to proxied value: {}", getLogPrefix(),ai);
            if (as.getAuthnInstant() != null) {
                ar.setAuthenticationInstant(ai);
            }
        }
    }
    
    /**
     * Log a successful authentication based on a designated attribute ID or the NameID value.
     */
    protected void logSuccess() {
        String nameToLog = null;
        if (loggedAttributeId != null && attributeContext != null) {
            final IdPAttribute attrToLog = attributeContext.getIdPAttributes().get(loggedAttributeId);
            if (attrToLog != null && !attrToLog.getValues().isEmpty()) {
                nameToLog = attrToLog.getValues().get(0).getDisplayValue();
            }
        }

        assert samlAuthnContext != null;
        final org.opensaml.saml.saml2.core.Subject samlSubject = samlAuthnContext.getSubject();
        
        if (nameToLog == null && samlSubject != null && samlSubject .getNameID() != null) {
            nameToLog = samlSubject.getNameID().getValue();
        }

        log.info("{} SAML authentication succeeded for '{}'", getLogPrefix(), nameToLog);
    }
        
// Checkstyle: CyclomaticComplexity|MethodLength OFF
    /** {@inheritDoc} */
    @Override
    @Nonnull protected Subject populateSubject(@Nonnull final Subject subject) {
        
        final SAMLAuthnContext localSamlAuthnContext = samlAuthnContext;
        final AttributeContext localAttributeContext = attributeContext;
        assert localSamlAuthnContext != null && localAttributeContext!=null;;
        final BaseContext localSamlAuthnContextParent = localSamlAuthnContext.getParent();
        assert localSamlAuthnContextParent!=null;
        
        final org.opensaml.saml.saml2.core.Subject samlSubject = localSamlAuthnContext.getSubject();
        final NameID nameID = samlSubject==null ? null : samlSubject.getNameID(); 
        if (nameID!= null) {
            subject.getPrincipals().add(new NameIDPrincipal(nameID));
        }
        final AuthnStatement authnStatement = localSamlAuthnContext.getAuthnStatement();
        assert authnStatement!=null;
        final AuthnContext authnContext = Constraint.isNotNull(authnStatement.getAuthnContext(), "No Authn Context");

        boolean principalsAdded = false;
        
        if (authnContextTranslatorEx != null) {
            // PRC is up (AuthenticationContext) and then down (to nested PRC).
            final Collection<Principal> translated = authnContextTranslatorEx.apply(
                    localSamlAuthnContextParent.getSubcontext(ProfileRequestContext.class));
            if (translated != null && !translated.isEmpty()) {
                subject.getPrincipals().addAll(translated);
                if (log.isDebugEnabled()) {
                    log.debug("{} Added translated Principals: {}", getLogPrefix(),
                            translated.stream().map(Principal::getName).collect(Collectors.toUnmodifiableList()));
                }
                principalsAdded = true;
            }
        }
        
        if (!principalsAdded && authnContextTranslator != null) {
            final Collection<Principal> translated = authnContextTranslator.apply(authnContext);
            if (translated != null && !translated.isEmpty()) {
                subject.getPrincipals().addAll(translated);
                if (log.isDebugEnabled()) {
                    log.debug("{} Added translated AuthnContext Principals: {}", getLogPrefix(),
                            translated.stream().map(Principal::getName).collect(Collectors.toUnmodifiableList()));
                }
                principalsAdded = true;
            }
        }
        
        if (!principalsAdded) {
            if (authnContext.getAuthnContextClassRef() != null) {
                final String classRef = authnContext.getAuthnContextClassRef().getURI();
                if (classRef != null) {
                    subject.getPrincipals().add(new AuthnContextClassRefPrincipal(classRef));
                    log.debug("{} Added AuthnContextClassRef from assertion: {}", getLogPrefix(), classRef);
                }
                principalsAdded = true;
            }
            
            if (authnContext.getAuthnContextDeclRef() != null) {
                final String declRef = authnContext.getAuthnContextDeclRef().getURI();
                if (declRef != null) {
                    subject.getPrincipals().add(new AuthnContextDeclRefPrincipal(declRef));
                    log.debug("{} Added AuthnContextDeclRef from assertion: {}", getLogPrefix(), declRef);
                }
                principalsAdded = true;
            }
        }
        
        if (!principalsAdded) {
            log.warn("{} No AuthnContext information usable from assertion", getLogPrefix());
        }
        
        subject.getPrincipals().add(buildProxyPrincipal(authnContext));
        
        if (localAttributeContext != null && !localAttributeContext.getIdPAttributes().isEmpty()) {
            log.debug("{} Adding filtered inbound attributes to Subject", getLogPrefix());
            subject.getPrincipals().addAll(
                localAttributeContext.getIdPAttributes().values()
                    .stream()
                    .map(a -> {assert a != null; return new IdPAttributePrincipal(a);})
                    .collect(Collectors.toUnmodifiableList()));
        }
        
        return subject;
    }
// Checkstyle: CyclomaticComplexity|MethodLength ON
    
    /**
     * Construct a populated {@link ProxyAuthenticationPrincipal} based on the inbound assertion.
     * 
     * @param authnContext the SAML {@link AuthnContext} issued by the proxied IdP
     * 
     * @return a constructed {@link ProxyAuthenticationPrincipal} to include in the {@link Subject}
     */
    @Nonnull private ProxyAuthenticationPrincipal buildProxyPrincipal(@Nonnull final AuthnContext authnContext) {
        
        final ProxyAuthenticationPrincipal proxied = new ProxyAuthenticationPrincipal();
        
        assert samlAuthnContext != null;
        AuthnStatement authnStatement = samlAuthnContext.getAuthnStatement();
        assert authnStatement != null;
        final Assertion assertion = (Assertion) authnStatement.getParent();
        assert assertion != null;
        if (!authnContext.getAuthenticatingAuthorities().isEmpty()) {
            proxied.getAuthorities().addAll(
                    authnContext.getAuthenticatingAuthorities()
                        .stream()
                        .map(AuthenticatingAuthority::getURI)
                        .filter(aa -> !Strings.isNullOrEmpty(aa))
                        .collect(Collectors.toUnmodifiableList()));
        }
        proxied.getAuthorities().add(assertion.getIssuer().getValue());
                
        final ProxyRestriction condition = assertion.getConditions().getProxyRestriction();
        if (condition != null) {
            proxied.setProxyCount(condition.getProxyCount());
            final List<Audience> audiences = condition.getAudiences() ; 
            if (audiences!= null) {
                proxied.getAudiences().addAll(audiences
                            .stream()
                            .map(Audience::getURI)
                            .filter(a -> !Strings.isNullOrEmpty(a))
                            .collect(Collectors.toUnmodifiableList()));
            }
        }
        
        return proxied;
    }
    
    /**
     * Process the inbound SAML Attributes.
     * 
     * @param profileRequestContext current profile request context
     */
    private void processAttributes(@Nonnull final ProfileRequestContext profileRequestContext) {
        
        log.debug("{} Decoding incoming SAML Attributes", getLogPrefix());
        
        final Multimap<String,IdPAttribute> mapped = HashMultimap.create();
        assert mapped != null;

        assert transcoderRegistry!= null;
        try (final ServiceableComponent<AttributeTranscoderRegistry> component =
                transcoderRegistry.getServiceableComponent()) {
            final MessageContext imc = profileRequestContext.getInboundMessageContext();
            assert imc != null;
            final Response response = (Response) imc.getMessage();
            assert response != null;
            for (final Assertion assertion : response.getAssertions()) {
                for (final AttributeStatement statement : assertion.getAttributeStatements()) {
                    for (final Attribute designator : statement.getAttributes()) {
                        assert designator!=null;
                        try {
                            decodeAttribute(component.getComponent(), profileRequestContext, designator, mapped);
                        } catch (final AttributeDecodingException e) {
                            log.error("{} Error decoding inbound Attribute", getLogPrefix(), e);
                        }
                    }
                }
            }
        } catch (final ServiceException e) {
            log.error("Attribute transcoder service unavailable", e);
            return;
        }

        log.debug("{} Incoming SAML Attributes mapped to attribute IDs: {}", getLogPrefix(), mapped.keySet());

        if (!mapped.isEmpty()) {
            final RelyingPartyContext rpCtx = profileRequestContext.getSubcontext(RelyingPartyContext.class);
            assert rpCtx != null;
            final AttributeContext ac = attributeContext = rpCtx.getOrCreateSubcontext(AttributeContext.class);
            ac.setUnfilteredIdPAttributes(mapped.values());
            ac.setIdPAttributes(null);
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
            log.info("{} No transcoding rule for Attribute (Name '{}', NameFormat: '{}')", getLogPrefix(),
                    input.getName(), input.getNameFormat() != null ? input.getNameFormat() : Attribute.UNSPECIFIED);
            return;
        }
        
        for (final TranscodingRule rules : transcodingRules) {
            assert rules != null;
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
        final ReloadableService<AttributeFilter> service = attributeFilterService;
        if (service == null) {
            log.warn("{} No AttributeFilter service provided", getLogPrefix());
            return;
        }

        final AttributeFilterContext filterContext =
                profileRequestContext.getOrCreateSubcontext(AttributeFilterContext.class);

        populateFilterContext(profileRequestContext, filterContext);
        try (final ServiceableComponent<AttributeFilter> component = service.getServiceableComponent()) {
            final AttributeFilter filter = component.getComponent();
            filter.filterAttributes(filterContext);
            filterContext.removeFromParent();
            assert attributeContext!=null;
            attributeContext.setIdPAttributes(filterContext.getFilteredIdPAttributes().values());
        } catch (final AttributeFilterException e) {
            log.error("{} Error while filtering inbound attributes", getLogPrefix(), e);
        } catch (final ServiceException e) {
            log.error("{} Invalid AttributeFilter configuration", getLogPrefix(), e);
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
        final AttributeContext ac = attributeContext;
        assert ac != null;
        
        filterContext.setDirection(Direction.INBOUND)
            .setPrefilteredIdPAttributes(ac.getUnfilteredIdPAttributes().values())
            .setMetadataResolver(metadataResolver)
            .setRequesterMetadataContextLookupStrategy(null)
            .setIssuerMetadataContextLookupStrategy(
                    new SAMLMetadataContextLookupFunction().compose(
                            new RecursiveTypedParentContextLookup<>(ProfileRequestContext.class)))
            .setProxiedRequesterContextLookupStrategy(null)
            .setAttributeIssuerID(
                    Constraint.isNotNull(getResponderLookupStrategy(), "No responder Strategy").
                    apply(profileRequestContext))
            .setAttributeRecipientID(
                    Constraint.isNotNull(getRequesterLookupStrategy(), "No requester strategy").
                    apply(profileRequestContext));
    }

}