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
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import net.shibboleth.idp.authn.AbstractAuthenticationAction;
import net.shibboleth.idp.authn.context.AuthenticationContext;
import net.shibboleth.idp.profile.IdPEventIds;
import net.shibboleth.idp.saml.authn.principal.AuthnContextClassRefPrincipal;
import net.shibboleth.idp.saml.authn.principal.AuthnContextDeclRefPrincipal;
import net.shibboleth.idp.saml.saml2.profile.config.BrowserSSOProfileConfiguration;
import net.shibboleth.profile.config.navigate.IdentifierGenerationStrategyLookupFunction;
import net.shibboleth.profile.context.RelyingPartyContext;
import net.shibboleth.profile.context.navigate.RelyingPartyIdLookupFunction;
import net.shibboleth.shared.annotation.constraint.NonnullBeforeExec;
import net.shibboleth.shared.logic.Constraint;
import net.shibboleth.shared.security.IdentifierGenerationStrategy;

import org.opensaml.core.xml.XMLObjectBuilderFactory;
import org.opensaml.core.xml.config.XMLObjectProviderRegistrySupport;
import org.opensaml.core.xml.io.MarshallingException;
import org.opensaml.core.xml.io.UnmarshallingException;
import org.opensaml.core.xml.util.XMLObjectSupport;
import org.opensaml.messaging.context.MessageContext;
import org.opensaml.messaging.context.navigate.ChildContextLookup;
import org.opensaml.messaging.context.navigate.ParentContextLookup;
import org.opensaml.messaging.context.navigate.RootContextLookup;
import org.opensaml.profile.action.ActionSupport;
import org.opensaml.profile.action.EventIds;
import org.opensaml.profile.context.ProfileRequestContext;
import org.opensaml.profile.context.ProxiedRequesterContext;
import org.opensaml.profile.context.navigate.InboundMessageContextLookup;
import org.opensaml.saml.common.SAMLObjectBuilder;
import org.opensaml.saml.common.SAMLVersion;
import org.opensaml.saml.ext.reqattr.RequestedAttributes;
import org.opensaml.saml.saml2.core.AuthnContextComparisonTypeEnumeration;
import org.opensaml.saml.saml2.core.AuthnRequest;
import org.opensaml.saml.saml2.core.Extensions;
import org.opensaml.saml.saml2.core.IDPEntry;
import org.opensaml.saml.saml2.core.IDPList;
import org.opensaml.saml.saml2.core.Issuer;
import org.opensaml.saml.saml2.core.NameID;
import org.opensaml.saml.saml2.core.NameIDPolicy;
import org.opensaml.saml.saml2.core.RequestedAuthnContext;
import org.opensaml.saml.saml2.core.RequesterID;
import org.opensaml.saml.saml2.core.Scoping;
import org.opensaml.saml.saml2.core.Subject;
import org.opensaml.saml.saml2.metadata.RequestedAttribute;
import org.slf4j.Logger;
import net.shibboleth.shared.primitive.LoggerFactory;

/**
 * Action that creates an {@link AuthnRequest} and sets it as the message returned by
 * {@link ProfileRequestContext#getOutboundMessageContext()}.
 * 
 * <p>If an issuer value is returned via a lookup strategy, then it's set as the Issuer of the message.</p>
 * 
 * <p>Various other values are derived from the active configuration such as {@link RequestedAuthnContext},
 * {@link NameIDPolicy}, and {@link Scoping}.</p>
 * 
 * @event {@link EventIds#PROCEED_EVENT_ID}
 * @event {@link EventIds#INVALID_MSG_CTX}
 * @event {@link EventIds#INVALID_PROFILE_CTX}
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
    
    /** Strategy used to obtain the request issuer value. */
    @Nullable private Function<ProfileRequestContext,String> issuerLookupStrategy;

    /** Strategy used to obtain the original requester value. */
    @Nonnull private Function<ProfileRequestContext,String> requesterLookupStrategy;

    /** Strategy used to obtain the proxied requester context. */
    @Nonnull private Function<ProfileRequestContext,ProxiedRequesterContext> proxiedRequesterContextLookupStrategy;
    
    /** Optional strategy to populate request with a {@link NameID}. */
    @Nullable private Function<ProfileRequestContext,NameID> nameIDLookupStrategy;
    
    /** The generator to use. */
    @NonnullBeforeExec private IdentifierGenerationStrategy idGenerator;
    
    /** Applicable profile configuration. */
    @NonnullBeforeExec private BrowserSSOProfileConfiguration profileConfiguration;

    /** EntityID to populate into Issuer element. */
    @Nullable private String issuerId;
    
    /** Constructor. */
    public AddAuthnRequest() {
        // Default strategy is a 16-byte secure random source.
        idGeneratorLookupStrategy = new IdentifierGenerationStrategyLookupFunction();
        
        // Fool the parent class into looking above instead of below the PRC for the context.
        setAuthenticationContextLookupStrategy(new ParentContextLookup<>(AuthenticationContext.class));

        // Root PRC -> RelyingPartyContext -> ID
        final Function<ProfileRequestContext,String> rls = new RelyingPartyIdLookupFunction().compose(
                new RootContextLookup<>(ProfileRequestContext.class));
        assert rls!= null;
        requesterLookupStrategy = rls;
        
        // Root PRC -> inbound context -> ProxiedRequesterContext
        final Function<ProfileRequestContext,ProxiedRequesterContext> prcls =
                new ChildContextLookup<>(ProxiedRequesterContext.class).compose(
                        new InboundMessageContextLookup().compose(
                                new RootContextLookup<>(ProfileRequestContext.class)));
        assert prcls != null;
        proxiedRequesterContextLookupStrategy = prcls;
    }
    
    /**
     * Set whether to overwrite an existing message.
     * 
     * @param flag flag to set
     */
    public void setOverwriteExisting(final boolean flag) {
        checkSetterPreconditions();
        overwriteExisting = flag;
    }

    /**
     * Set the strategy used to locate the {@link IdentifierGenerationStrategy} to use.
     * 
     * @param strategy lookup strategy
     */
    public void setIdentifierGeneratorLookupStrategy(
            @Nonnull final Function<ProfileRequestContext,IdentifierGenerationStrategy> strategy) {
        checkSetterPreconditions();
        idGeneratorLookupStrategy =
                Constraint.isNotNull(strategy, "IdentifierGenerationStrategy lookup strategy cannot be null");
    }

    /**
     * Set the strategy used to locate the issuer value to use.
     * 
     * @param strategy lookup strategy
     */
    public void setIssuerLookupStrategy(@Nullable final Function<ProfileRequestContext,String> strategy) {
        checkSetterPreconditions();
        issuerLookupStrategy = strategy;
    }

    /**
     * Set the strategy used to locate the requester value to use for the Scoping element's {@link RequesterID} value.
     * 
     * @param strategy lookup strategy
     * 
     * @since 4.3.0
     */
    public void setRequesterLookupStrategy(@Nonnull final Function<ProfileRequestContext,String> strategy) {
        checkSetterPreconditions();
        requesterLookupStrategy = Constraint.isNotNull(strategy, "Requester lookup strategy cannot be null");
    }

    /**
     * Set the strategy used to locate the {@link ProxiedRequesterContext} to use for the Scoping element's
     * {@link RequesterID} values.
     * 
     * @param strategy lookup strategy
     * 
     * @since 4.3.0
     */
    public void setProxiedRequesterContextLookupStrategy(
            @Nonnull final Function<ProfileRequestContext,ProxiedRequesterContext> strategy) {
        checkSetterPreconditions();
        proxiedRequesterContextLookupStrategy =
                Constraint.isNotNull(strategy, "ProxiedRequesterContext lookup strategy cannot be null");
    }
    
    /**
     * Set optional strategy to derive a {@link NameID} to populate into the {@link AuthnRequest}'s
     * {@link Subject} element.
     * 
     * @param strategy lookup strategy
     * 
     * @since 5.0.0
     */
    public void setNameIDLookupStrategy(@Nullable final Function<ProfileRequestContext,NameID> strategy) {
        checkSetterPreconditions();
        nameIDLookupStrategy = strategy;
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
                (SAMLObjectBuilder<AuthnRequest>) bf.<AuthnRequest>ensureBuilder(
                        AuthnRequest.DEFAULT_ELEMENT_NAME);
        final SAMLObjectBuilder<NameIDPolicy> nipBuilder =
                (SAMLObjectBuilder<NameIDPolicy>) bf.<NameIDPolicy>ensureBuilder(
                        NameIDPolicy.DEFAULT_ELEMENT_NAME);

        final AuthnRequest object = requestBuilder.buildObject();
        object.setID(idGenerator.generateIdentifier());
        object.setIssueInstant(Instant.now());
        object.setVersion(SAMLVersion.VERSION_20);
        final Integer index = profileConfiguration.getAttributeIndex(profileRequestContext);
        if (index != null) {
            log.debug("{} Setting AttributeConsumingServiceIndex to '{}' for SAML AuthnRequest", getLogPrefix(),
                    index);
            object.setAttributeConsumingServiceIndex(index);
        }

        if (issuerId != null) {
            log.debug("{} Setting Issuer to {}", getLogPrefix(), issuerId);
            final SAMLObjectBuilder<Issuer> issuerBuilder =
                    (SAMLObjectBuilder<Issuer>) bf.<Issuer>ensureBuilder(Issuer.DEFAULT_ELEMENT_NAME);
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
        final String qualifier = profileConfiguration.getSPNameQualifier(profileRequestContext);
        if (qualifier != null) {
            log.debug("{} Setting NameIDPolicy SPNameQualifier to '{}' for SAML AuthnRequest", getLogPrefix(),
                    qualifier);
            nip.setSPNameQualifier(qualifier);
        }
        
        // TODO: use metadata for NameID Formats too?
        final List<String> formats = profileConfiguration.getNameIDFormatPrecedence(profileRequestContext);
        if (!formats.isEmpty()) {
            log.debug("{} Setting NameIDPolicy Format to '{}' for SAML AuthnRequest", getLogPrefix(), formats.get(0));
            nip.setFormat(formats.get(0));
        }

        object.setNameIDPolicy(nip);

        final RequestedAuthnContext rac = buildRequestedAuthnContext(profileRequestContext);
        if (rac != null) {
            final AuthnContextComparisonTypeEnumeration operator =
                    profileConfiguration.getAuthnContextComparison(profileRequestContext);
            if (operator != null) {
                log.debug("{} Setting RequestedAuthnContext comparison to {}", getLogPrefix(), operator);
                rac.setComparison(operator);
            }
            object.setRequestedAuthnContext(rac);
        }

        object.setSubject(buildSubject(profileRequestContext));
        object.setScoping(buildScoping(profileRequestContext, authenticationContext.getProxyCount(),
                authenticationContext.getProxiableAuthorities()));
        object.setExtensions(buildExtensions(profileRequestContext));
        
        final MessageContext omc = profileRequestContext.getOutboundMessageContext();
        assert omc != null;
        omc.setMessage(object);
    }
// Checkstyle: MethodLength ON
    
    /**
     * Build a {@link RequestedAuthnContext} if warranted.
     * 
     * @param profileRequestContext current profile request context
     * 
     * @return the object to include in the request, or null
     */
    @Nullable private RequestedAuthnContext buildRequestedAuthnContext(
            @Nullable final ProfileRequestContext profileRequestContext) {
        
        // RequestedAuthnContext also based on profile configuration.
        assert profileConfiguration!=null;
        final List<Principal> principals = profileConfiguration.getDefaultAuthenticationMethods(profileRequestContext);
        if (principals.isEmpty()) {
            return null;
        }
        
        final XMLObjectBuilderFactory bf = XMLObjectProviderRegistrySupport.getBuilderFactory();
        final SAMLObjectBuilder<RequestedAuthnContext> builder =
                (SAMLObjectBuilder<RequestedAuthnContext>) bf.<RequestedAuthnContext>ensureBuilder(
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
     * Build a {@link Subject} element if necessary.
     * 
     * @param profileRequestContext profile request context
     * 
     * @return the {@link Subject} element to include
     * 
     * @since 5.0.0
     */
    @Nullable private Subject buildSubject(@Nonnull final ProfileRequestContext profileRequestContext) {

        final NameID nameID = nameIDLookupStrategy != null
                ? nameIDLookupStrategy.apply(profileRequestContext) : null;
        if (nameID == null) {
            return null;
        }
        
        final XMLObjectBuilderFactory bf = XMLObjectProviderRegistrySupport.getBuilderFactory();
        final SAMLObjectBuilder<Subject> subjectBuilder =
                (SAMLObjectBuilder<Subject>) bf.<Subject>ensureBuilder(Subject.DEFAULT_ELEMENT_NAME);
        
        final Subject subject = subjectBuilder.buildObject();
        subject.setNameID(nameID);
        
        log.debug("{} Populating request with NameID '{}' and Format '{}'", getLogPrefix(),
                nameID.getValue(), nameID.getFormat());
        
        return subject;
    }
    
    /**
     * Build a {@link Scoping} element, decrementing the proxy count if set.
     * 
     * @param profileRequestContext current profile request context
     * @param count proxy count
     * @param idplist list of IdP entityIDs
     * 
     * @return populated {@link Scoping}
     */
    @Nullable private Scoping buildScoping(@Nonnull final ProfileRequestContext profileRequestContext,
            @Nullable final Integer count, @Nonnull final Set<String> idplist) {

        boolean include = false;
        
        assert profileConfiguration != null;
        if (profileConfiguration.isIgnoreScoping(profileRequestContext)) {
            log.warn("{} Skipping generation of Scoping element in violation of standard", getLogPrefix());
            return null;
        }
        
        final XMLObjectBuilderFactory bf = XMLObjectProviderRegistrySupport.getBuilderFactory();
        
        final SAMLObjectBuilder<Scoping> scopingBuilder =
                (SAMLObjectBuilder<Scoping>) bf.<Scoping>ensureBuilder(Scoping.DEFAULT_ELEMENT_NAME);
        final Scoping scoping = scopingBuilder.buildObject();
        
        if (count != null) {
            scoping.setProxyCount(Integer.max(0, count - 1));
            include = true;
        }
        
        if (!idplist.isEmpty()) {
            final SAMLObjectBuilder<IDPList> idpListBuilder =
                    (SAMLObjectBuilder<IDPList>) bf.<IDPList>ensureBuilder(IDPList.DEFAULT_ELEMENT_NAME);
            final SAMLObjectBuilder<IDPEntry> idpBuilder =
                    (SAMLObjectBuilder<IDPEntry>) bf.<IDPEntry>ensureBuilder(IDPEntry.DEFAULT_ELEMENT_NAME);
                        
            final IDPList idps = idpListBuilder.buildObject();
            for (final String idp : idplist) {
                final IDPEntry entry = idpBuilder.buildObject();
                entry.setProviderID(idp);
                idps.getIDPEntrys().add(entry);
            }
            scoping.setIDPList(idps);
            include = true;
        }

        final SAMLObjectBuilder<RequesterID> requesterIdBuilder =
                (SAMLObjectBuilder<RequesterID>) bf.<RequesterID>ensureBuilder(RequesterID.DEFAULT_ELEMENT_NAME);

        final ProxiedRequesterContext proxiedReqCtx =
                proxiedRequesterContextLookupStrategy.apply(profileRequestContext);
        if (proxiedReqCtx != null) {
            for (final String id : proxiedReqCtx.getRequesters()) {
                final RequesterID requesterId = requesterIdBuilder.buildObject();
                requesterId.setURI(id);
                scoping.getRequesterIDs().add(requesterId);
                include = true;
            }
        }

        final String immediateRequester = requesterLookupStrategy.apply(profileRequestContext);
        if (immediateRequester != null) {
            final RequesterID requesterId = requesterIdBuilder.buildObject();
            requesterId.setURI(immediateRequester);
            scoping.getRequesterIDs().add(requesterId);
            include = true;
        }
        
        return include ? scoping : null;
    }
 
    /**
     * Build {@link RequestedAttributes} extension if required.
     * 
     * @param profileRequestContext current profile request context
     * 
     * @return extension or null
     */
    @Nullable private Extensions buildExtensions(@Nonnull final ProfileRequestContext profileRequestContext) {
        
        assert profileConfiguration!=null;
        final Collection<RequestedAttribute> attrs = profileConfiguration.getRequestedAttributes(profileRequestContext);
        if (!attrs.isEmpty()) {
            final XMLObjectBuilderFactory bf = XMLObjectProviderRegistrySupport.getBuilderFactory();
            final SAMLObjectBuilder<Extensions> extBuilder =
                    (SAMLObjectBuilder<Extensions>) bf.<Extensions>ensureBuilder(
                            Extensions.DEFAULT_ELEMENT_NAME);
            final SAMLObjectBuilder<RequestedAttributes> reqExtBuilder =
                    (SAMLObjectBuilder<RequestedAttributes>) bf.<RequestedAttributes>ensureBuilder(
                            RequestedAttributes.DEFAULT_ELEMENT_NAME);
            final RequestedAttributes reqExt = reqExtBuilder.buildObject();
            attrs.forEach(attr -> {
                try {
                    assert attr != null;
                    reqExt.getRequestedAttributes().add(XMLObjectSupport.cloneXMLObject(attr));
                } catch (final MarshallingException|UnmarshallingException e) {
                    log.error("{} Error cloning RequestedAttribute from profile configuration", getLogPrefix(), e);
                }
            });
            final Extensions ext = extBuilder.buildObject();
            ext.getUnknownXMLObjects().add(reqExt);
            return ext;
        }
        
        return null;
    }
    
}