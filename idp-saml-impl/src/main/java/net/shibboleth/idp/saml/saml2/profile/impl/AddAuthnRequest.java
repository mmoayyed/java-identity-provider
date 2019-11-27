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
import java.util.List;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import net.shibboleth.idp.authn.AbstractAuthenticationAction;
import net.shibboleth.idp.authn.context.AuthenticationContext;
import net.shibboleth.idp.profile.IdPEventIds;
import net.shibboleth.idp.profile.context.RelyingPartyContext;
import net.shibboleth.idp.saml.authn.principal.AuthnContextClassRefPrincipal;
import net.shibboleth.idp.saml.authn.principal.AuthnContextDeclRefPrincipal;
import net.shibboleth.idp.saml.saml2.profile.config.BrowserSSOProfileConfiguration;
import net.shibboleth.utilities.java.support.annotation.constraint.NonnullElements;
import net.shibboleth.utilities.java.support.component.ComponentSupport;
import net.shibboleth.utilities.java.support.logic.Constraint;
import net.shibboleth.utilities.java.support.security.IdentifierGenerationStrategy;
import net.shibboleth.utilities.java.support.security.impl.SecureRandomIdentifierGenerationStrategy;

import org.opensaml.core.xml.XMLObjectBuilderFactory;
import org.opensaml.core.xml.config.XMLObjectProviderRegistrySupport;
import org.opensaml.messaging.context.MessageContext;
import org.opensaml.messaging.context.navigate.ParentContextLookup;
import org.opensaml.profile.action.ActionSupport;
import org.opensaml.profile.action.EventIds;
import org.opensaml.profile.context.ProfileRequestContext;
import org.opensaml.saml.common.SAMLObjectBuilder;
import org.opensaml.saml.common.SAMLVersion;
import org.opensaml.saml.saml2.core.AuthnContextComparisonTypeEnumeration;
import org.opensaml.saml.saml2.core.AuthnRequest;
import org.opensaml.saml.saml2.core.IDPEntry;
import org.opensaml.saml.saml2.core.IDPList;
import org.opensaml.saml.saml2.core.Issuer;
import org.opensaml.saml.saml2.core.NameIDPolicy;
import org.opensaml.saml.saml2.core.RequestedAuthnContext;
import org.opensaml.saml.saml2.core.Scoping;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Action that creates an {@link AuthnRequest} and sets it as the message returned by
 * {@link ProfileRequestContext#getOutboundMessageContext()}.
 * 
 * <p>If an issuer value is returned via a lookup strategy, then it's set as the Issuer of the message.</p>
 * 
 * <p>Various other values are derived from the active configuration. The outbound relay state is also
 * set to the flow execution key.</p>
 * 
 * @event {@link EventIds#PROCEED_EVENT_ID}
 * @event {@link EventIds#INVALID_MSG_CTX}
 * @event {@link EventIds#INVALID_PROFILE_CTX}
 * @event {@link EventIds#MESSAGE_PROC_ERROR}
 * @event {@link IdPEventIds#INVALID_PROFILE_CONFIG}
 * 
 * @post ProfileRequestContext.getOutboundMessageContext().getMessage() != null
 */
public class AddAuthnRequest extends AbstractAuthenticationAction {

    /** Class logger. */
    @Nonnull private Logger log = LoggerFactory.getLogger(AddAuthnRequest.class);
    
    /** Overwrite an existing message? */
    private boolean overwriteExisting;

    /** Strategy used to locate the {@link IdentifierGenerationStrategy} to use. */
    @Nonnull private Function<ProfileRequestContext,IdentifierGenerationStrategy> idGeneratorLookupStrategy;

    /** Strategy used to obtain the relay state token to provide. */
    @Nonnull private Function<ProfileRequestContext,String> relayStateLookupStrategy;
    
    /** Strategy used to obtain the request issuer value. */
    @Nullable private Function<ProfileRequestContext,String> issuerLookupStrategy;
    
    /** The generator to use. */
    @Nullable private IdentifierGenerationStrategy idGenerator;
    
    /** Applicable profile configuration. */
    @Nullable private BrowserSSOProfileConfiguration profileConfiguration;

    /** EntityID to populate into Issuer element. */
    @Nullable private String issuerId;
    
    /** Constructor. */
    public AddAuthnRequest() {
        // Default strategy is a 16-byte secure random source.
        idGeneratorLookupStrategy = prc -> new SecureRandomIdentifierGenerationStrategy();
        
        // Fool the parent class into looking above instead of below the PRC for the context.
        setAuthenticationContextLookupStrategy(new ParentContextLookup<>(AuthenticationContext.class));
    }
    
    /**
     * Set whether to overwrite an existing message.
     * 
     * @param flag flag to set
     */
    public void setOverwriteExisting(final boolean flag) {
        ComponentSupport.ifInitializedThrowUnmodifiabledComponentException(this);
        
        overwriteExisting = flag;
    }

    /**
     * Set the strategy used to locate the {@link IdentifierGenerationStrategy} to use.
     * 
     * @param strategy lookup strategy
     */
    public void setIdentifierGeneratorLookupStrategy(
            @Nonnull final Function<ProfileRequestContext,IdentifierGenerationStrategy> strategy) {
        ComponentSupport.ifInitializedThrowUnmodifiabledComponentException(this);

        idGeneratorLookupStrategy =
                Constraint.isNotNull(strategy, "IdentifierGenerationStrategy lookup strategy cannot be null");
    }
    
    /**
     * Set the strategy used to obtain the RelayState value to supply for flow restoration.
     * 
     * @param strategy lookup strategy
     */
    public void setRelayStateLookupStrategy(@Nonnull final Function<ProfileRequestContext,String> strategy) {
        ComponentSupport.ifInitializedThrowUnmodifiabledComponentException(this);
        
        relayStateLookupStrategy = Constraint.isNotNull(strategy, "RelayState lookup srategy cannot be null");
    }

    /**
     * Set the strategy used to locate the issuer value to use.
     * 
     * @param strategy lookup strategy
     */
    public void setIssuerLookupStrategy(@Nullable final Function<ProfileRequestContext,String> strategy) {
        ComponentSupport.ifInitializedThrowUnmodifiabledComponentException(this);

        issuerLookupStrategy = strategy;
    }

// Checkstyle: CyclomaticComplexity OFF
    /** {@inheritDoc} */
    @Override
    protected boolean doPreExecute(@Nonnull final ProfileRequestContext profileRequestContext,
            @Nonnull final AuthenticationContext authenticationContext) {

        if (!super.doPreExecute(profileRequestContext, authenticationContext)) {
            return false;
        }
        
        final RelyingPartyContext rpCtx = profileRequestContext.getSubcontext(RelyingPartyContext.class);
        if (rpCtx != null && rpCtx.getConfiguration() != null &&
                rpCtx.getProfileConfig() instanceof BrowserSSOProfileConfiguration) {
            profileConfiguration = (BrowserSSOProfileConfiguration) rpCtx.getProfileConfig();
        }
        if (profileConfiguration == null) {
            log.error("{} BrowserSSOProfileConfiguration not found", getLogPrefix());
            ActionSupport.buildEvent(profileRequestContext, IdPEventIds.INVALID_PROFILE_CONFIG);
            return false;
        }
        
        final MessageContext outboundMessageCtx = profileRequestContext.getOutboundMessageContext();
        if (outboundMessageCtx == null) {
            log.debug("{} No outbound message context", getLogPrefix());
            ActionSupport.buildEvent(profileRequestContext, EventIds.INVALID_MSG_CTX);
            return false;
        } else if (!overwriteExisting && outboundMessageCtx.getMessage() != null) {
            log.debug("{} Outbound message context already contains a message", getLogPrefix());
            ActionSupport.buildEvent(profileRequestContext, EventIds.INVALID_MSG_CTX);
            return false;
        }

        idGenerator = idGeneratorLookupStrategy.apply(profileRequestContext);
        if (idGenerator == null) {
            log.debug("{} No identifier generation strategy", getLogPrefix());
            ActionSupport.buildEvent(profileRequestContext, EventIds.INVALID_PROFILE_CTX);
            return false;
        }
        
        if (issuerLookupStrategy != null) {
            issuerId = issuerLookupStrategy.apply(profileRequestContext);
        }

        outboundMessageCtx.setMessage(null);
        
        return true;
    }
// Checkstyle: CyclomaticComplexity ON

// Checkstyle: MethodLength OFF
    /** {@inheritDoc} */
    @Override
    protected void doExecute(@Nonnull final ProfileRequestContext profileRequestContext,
            @Nonnull final AuthenticationContext authenticationContext) {

        log.debug("{} Building AuthnRequest for upstream IdP ({})", getLogPrefix(),
                authenticationContext.getAuthenticatingAuthority());
        
        final XMLObjectBuilderFactory bf = XMLObjectProviderRegistrySupport.getBuilderFactory();
        final SAMLObjectBuilder<AuthnRequest> requestBuilder =
                (SAMLObjectBuilder<AuthnRequest>) bf.<AuthnRequest>getBuilderOrThrow(
                        AuthnRequest.DEFAULT_ELEMENT_NAME);
        final SAMLObjectBuilder<NameIDPolicy> nipBuilder =
                (SAMLObjectBuilder<NameIDPolicy>) bf.<NameIDPolicy>getBuilderOrThrow(
                        NameIDPolicy.DEFAULT_ELEMENT_NAME);

        final AuthnRequest object = requestBuilder.buildObject();
        
        object.setID(idGenerator.generateIdentifier());
        object.setIssueInstant(Instant.now());
        object.setVersion(SAMLVersion.VERSION_20);

        if (issuerId != null) {
            log.debug("{} Setting Issuer to {}", getLogPrefix(), issuerId);
            final SAMLObjectBuilder<Issuer> issuerBuilder =
                    (SAMLObjectBuilder<Issuer>) bf.<Issuer>getBuilderOrThrow(Issuer.DEFAULT_ELEMENT_NAME);
            final Issuer issuer = issuerBuilder.buildObject();
            issuer.setValue(issuerId);
            object.setIssuer(issuer);
        } else {
            log.debug("{} No issuer value available, leaving Issuer unset", getLogPrefix());
        }
        
        // ForceAuthn comes from configuration, which by default will take into account the
        // AuthenticationContext parent's state (but may be overridden by deployer).
        if (profileConfiguration.isForceAuthn(profileRequestContext)) {
            log.debug("{} Setting ForceAuthn for SAML AuthnRequest", getLogPrefix());
            object.setForceAuthn(true);
        }
        
        // Only set passive based on request.
        if (authenticationContext.isPassive()) {
            log.debug("{} Setting IsPassive for SAML AuthnRequest", getLogPrefix());
            object.setIsPassive(true);
        }
        
        final NameIDPolicy nip = nipBuilder.buildObject();
        nip.setAllowCreate(true);
        
        // TODO: use metadata for NameID Formats too?
        final List<String> formats = profileConfiguration.getNameIDFormatPrecedence(profileRequestContext);
        if (!formats.isEmpty()) {
            log.debug("{} Setting NameIDPolicy Format to '{}' for SAML AuthnRequest", getLogPrefix(), formats.get(0));
            nip.setFormat(formats.get(0));
        }
        
        object.setNameIDPolicy(nip);

        final RequestedAuthnContext rac = getRequestedAuthnContext(profileRequestContext);
        if (rac != null) {
            final AuthnContextComparisonTypeEnumeration operator =
                    profileConfiguration.getAuthnContextComparison(profileRequestContext);
            if (operator != null) {
                log.debug("{} Setting RequestedAuthnContext comparison to {}", getLogPrefix(), operator);
                rac.setComparison(operator);
            }
            object.setRequestedAuthnContext(rac);
        }
        
        object.setScoping(buildScoping(profileRequestContext, authenticationContext.getProxyCount(),
                authenticationContext.getProxiableAuthorities()));
        
        profileRequestContext.getOutboundMessageContext().setMessage(object);
    }
// Checkstyle: MethodLength ON
    
    /**
     * Build a {@link RequestedAuthnContext} if warranted.
     * 
     * @param profileRequestContext current profile request context
     * 
     * @return the object to include in the request, or null
     */
    @Nullable private RequestedAuthnContext getRequestedAuthnContext(
            @Nullable final ProfileRequestContext profileRequestContext) {
        
        // RequestedAuthnContext also based on profile configuration.
        final List<Principal> principals = profileConfiguration.getDefaultAuthenticationMethods(profileRequestContext);
        if (principals.isEmpty()) {
            return null;
        }
        
        final XMLObjectBuilderFactory bf = XMLObjectProviderRegistrySupport.getBuilderFactory();
        final SAMLObjectBuilder<RequestedAuthnContext> builder =
                (SAMLObjectBuilder<RequestedAuthnContext>) bf.<RequestedAuthnContext>getBuilderOrThrow(
                        RequestedAuthnContext.DEFAULT_ELEMENT_NAME);
        
        // Check for class refs.
        final List<AuthnContextClassRefPrincipal> classRefPrincipals = principals.stream()
                .filter(AuthnContextClassRefPrincipal.class::isInstance)
                .map(AuthnContextClassRefPrincipal.class::cast)
                .collect(Collectors.toUnmodifiableList());
        if (!classRefPrincipals.isEmpty()) {
            final RequestedAuthnContext rac = builder.buildObject();
            
            rac.getAuthnContextClassRefs().addAll(
                    classRefPrincipals.stream()
                        .map(AuthnContextClassRefPrincipal::getAuthnContextClassRef)
                        .collect(Collectors.toUnmodifiableList()));
            
            if (log.isDebugEnabled()) {
                log.debug("{} Setting RequestedAuthnContext class refs to {}", getLogPrefix(),
                        classRefPrincipals.stream()
                            .map(AuthnContextClassRefPrincipal::getName)
                            .collect(Collectors.toUnmodifiableList()));
            }
            
            return rac;
        }
        
        // Check for decl refs.
        final List<AuthnContextDeclRefPrincipal> declRefPrincipals = principals.stream()
                .filter(AuthnContextDeclRefPrincipal.class::isInstance)
                .map(AuthnContextDeclRefPrincipal.class::cast)
                .collect(Collectors.toUnmodifiableList());
        if (!declRefPrincipals.isEmpty()) {
            final RequestedAuthnContext rac = builder.buildObject();
            
            rac.getAuthnContextDeclRefs().addAll(
                    declRefPrincipals.stream()
                        .map(AuthnContextDeclRefPrincipal::getAuthnContextDeclRef)
                        .collect(Collectors.toUnmodifiableList()));
            
            if (log.isDebugEnabled()) {
                log.debug("{} Setting RequestedAuthnContext decl refs to {}", getLogPrefix(),
                        declRefPrincipals.stream()
                            .map(AuthnContextDeclRefPrincipal::getName)
                            .collect(Collectors.toUnmodifiableList()));
            }

            return rac;
        }
        
        return null;
    }
    
    /**
     * Build a {@Scoping} element, decrementing the proxy count if set.
     * 
     * @param profileRequestContext current profile request context
     * @param count proxy count
     * @param idplist list of IdP entityIDs
     * 
     * @return populated {@link Scoping}
     */
    @Nullable public Scoping buildScoping(@Nonnull final ProfileRequestContext profileRequestContext,
            @Nullable final Integer count, @Nonnull @NonnullElements final Set<String> idplist) {
        
        if (count == null && idplist.isEmpty()) {
            return null;
        } else if (profileConfiguration.isIgnoreScoping(profileRequestContext)) {
            log.warn("{} Skipping generation of Scoping element in violation of standard", getLogPrefix());
            return null;
        }
        
        final XMLObjectBuilderFactory bf = XMLObjectProviderRegistrySupport.getBuilderFactory();
        final SAMLObjectBuilder<Scoping> scopingBuilder =
                (SAMLObjectBuilder<Scoping>) bf.<Scoping>getBuilderOrThrow(Scoping.DEFAULT_ELEMENT_NAME);
        final Scoping scoping = scopingBuilder.buildObject();
        scoping.setProxyCount(Integer.min(0, count - 1));
        
        if (!idplist.isEmpty()) {
            final SAMLObjectBuilder<IDPList> idpListBuilder =
                    (SAMLObjectBuilder<IDPList>) bf.<IDPList>getBuilderOrThrow(IDPList.DEFAULT_ELEMENT_NAME);
            final SAMLObjectBuilder<IDPEntry> idpBuilder =
                    (SAMLObjectBuilder<IDPEntry>) bf.<IDPEntry>getBuilderOrThrow(IDPEntry.DEFAULT_ELEMENT_NAME);
                        
            final IDPList idps = idpListBuilder.buildObject();
            for (final String idp : idplist) {
                final IDPEntry entry = idpBuilder.buildObject();
                entry.setProviderID(idp);
                idps.getIDPEntrys().add(entry);
            }
            scoping.setIDPList(idps);
        }
        
        return scoping;
    }
    
}