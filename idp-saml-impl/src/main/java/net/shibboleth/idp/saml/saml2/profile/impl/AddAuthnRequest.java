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

import java.time.Instant;
import java.util.List;
import java.util.function.Function;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import net.shibboleth.idp.authn.AbstractAuthenticationAction;
import net.shibboleth.idp.authn.context.AuthenticationContext;
import net.shibboleth.idp.profile.IdPEventIds;
import net.shibboleth.idp.profile.context.RelyingPartyContext;
import net.shibboleth.idp.saml.saml2.profile.config.BrowserSSOProfileConfiguration;
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
import org.opensaml.saml.saml2.core.AuthnRequest;
import org.opensaml.saml.saml2.core.Issuer;
import org.opensaml.saml.saml2.core.NameIDPolicy;
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
    
    /** Strategy used to obtain the response issuer value. */
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
        idGeneratorLookupStrategy = new Function<>() {
            public IdentifierGenerationStrategy apply(final ProfileRequestContext input) {
                return new SecureRandomIdentifierGenerationStrategy();
            }
        };
        
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
        
        // ForceAuthn may come from request or config.
        if (authenticationContext.isForceAuthn() || profileConfiguration.isForceAuthn(profileRequestContext)) {
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
        
        profileRequestContext.getOutboundMessageContext().setMessage(object);
    }
    
}