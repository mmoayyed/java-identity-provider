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

package net.shibboleth.idp.saml.saml2.profile.delegation.impl;

import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import net.shibboleth.idp.profile.AbstractProfileAction;
import net.shibboleth.idp.profile.context.RelyingPartyContext;
import net.shibboleth.idp.saml.profile.context.navigate.SAMLMetadataContextLookupFunction;
import net.shibboleth.idp.saml.saml2.profile.config.BrowserSSOProfileConfiguration;
import net.shibboleth.utilities.java.support.annotation.Prototype;
import net.shibboleth.utilities.java.support.component.ComponentInitializationException;
import net.shibboleth.utilities.java.support.component.ComponentSupport;
import net.shibboleth.utilities.java.support.logic.Constraint;
import net.shibboleth.utilities.java.support.primitive.StringSupport;
import net.shibboleth.utilities.java.support.resolver.CriteriaSet;
import net.shibboleth.utilities.java.support.resolver.ResolverException;

import org.openliberty.xmltooling.disco.MetadataAbstract;
import org.openliberty.xmltooling.disco.ProviderID;
import org.openliberty.xmltooling.disco.SecurityContext;
import org.openliberty.xmltooling.disco.SecurityMechID;
import org.openliberty.xmltooling.disco.ServiceType;
import org.openliberty.xmltooling.security.Token;
import org.openliberty.xmltooling.soapbinding.Framework;
import org.opensaml.core.criterion.EntityIdCriterion;
import org.opensaml.core.xml.XMLObject;
import org.opensaml.core.xml.XMLObjectBuilder;
import org.opensaml.core.xml.schema.XSAny;
import org.opensaml.core.xml.util.XMLObjectSupport;
import org.opensaml.messaging.context.navigate.ChildContextLookup;
import org.opensaml.profile.action.ActionSupport;
import org.opensaml.profile.action.EventIds;
import org.opensaml.profile.context.ProfileRequestContext;
import org.opensaml.saml.common.messaging.context.AttributeConsumingServiceContext;
import org.opensaml.saml.common.messaging.context.SAMLMetadataContext;
import org.opensaml.saml.criterion.RoleDescriptorCriterion;
import org.opensaml.saml.saml2.core.Assertion;
import org.opensaml.saml.saml2.core.Attribute;
import org.opensaml.saml.saml2.core.AttributeStatement;
import org.opensaml.saml.saml2.core.AttributeValue;
import org.opensaml.saml.saml2.core.Audience;
import org.opensaml.saml.saml2.core.AudienceRestriction;
import org.opensaml.saml.saml2.core.AuthnRequest;
import org.opensaml.saml.saml2.core.Conditions;
import org.opensaml.saml.saml2.core.KeyInfoConfirmationDataType;
import org.opensaml.saml.saml2.core.NameID;
import org.opensaml.saml.saml2.core.Response;
import org.opensaml.saml.saml2.core.Subject;
import org.opensaml.saml.saml2.core.SubjectConfirmation;
import org.opensaml.saml.saml2.core.SubjectConfirmationData;
import org.opensaml.saml.saml2.metadata.AttributeConsumingService;
import org.opensaml.saml.saml2.metadata.RequestedAttribute;
import org.opensaml.saml.saml2.metadata.RoleDescriptor;
import org.opensaml.saml.saml2.profile.SAML2ActionSupport;
import org.opensaml.security.SecurityException;
import org.opensaml.security.credential.Credential;
import org.opensaml.security.credential.CredentialResolver;
import org.opensaml.security.credential.UsageType;
import org.opensaml.security.criteria.UsageCriterion;
import org.opensaml.soap.wsaddressing.Address;
import org.opensaml.soap.wsaddressing.EndpointReference;
import org.opensaml.soap.wsaddressing.Metadata;
import org.opensaml.xmlsec.keyinfo.KeyInfoGenerator;
import org.opensaml.xmlsec.keyinfo.KeyInfoGeneratorFactory;
import org.opensaml.xmlsec.keyinfo.KeyInfoGeneratorManager;
import org.opensaml.xmlsec.keyinfo.NamedKeyInfoGeneratorManager;
import org.opensaml.xmlsec.signature.KeyInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Function;

//TODO need a lot more Javadoc detail here, and event ID's supported.

/**
 * A profile action which decorates SAML 2 Assertions appropriately for use as delegation tokens.
 */
@Prototype
public class DecorateDelegatedAssertion extends AbstractProfileAction {
    
    /** Internal error message constant. */
    public static final String INTERNAL_ERR_MSG = "Internal IdP processing error";
    
    /** Enum which represents the state of the request presenter's indication of whether
     * a delegation token is requested. */
    private static enum DelegationRequest {
        /** Delegation was not requested. */
        NOT_REQUESTED,
        /** Delegation was requested, as optional. */
        REQUESTED_OPTIONAL,
        /** Delegation was requested, as required. */
        REQUESTED_REQUIRED,
    };
    
    /** Class logger. */
    private final Logger log = LoggerFactory.getLogger(DecorateDelegatedAssertion.class);
    
    // Configured data
    
    /** Default delegation request value. */
    private DelegationRequest defaultDelegationRequested = DelegationRequest.REQUESTED_OPTIONAL;
    
    /** The URL at which the IdP will accept Liberty ID-WSF SSOS requests. */
    private String libertySSOSEndpointURL;
    
    /** Strategy used to lookup the RelyingPartyContext. */
    @Nonnull private Function<ProfileRequestContext,RelyingPartyContext> relyingPartyContextLookupStrategy;
    
    /** Strategy used to lookup the SAMLMetadataContext. */
    @Nonnull private Function<ProfileRequestContext, SAMLMetadataContext> samlMetadataContextLookupStrategy;
    
    /** Strategy used to locate the {@link Assertion}s on which to operate. */
    @Nonnull private Function<ProfileRequestContext,List<Assertion>> assertionLookupStrategy;
    
    /** The manager used to generate KeyInfo instances from Credentials. */
    @Nonnull private NamedKeyInfoGeneratorManager keyInfoGeneratorManager;
    
    /** The credential resolver used to resolve HoK Credentials for the peer. */
    @Nonnull private CredentialResolver credentialResolver;
    
    
    // Runtime data
    
    /** The list of assertions on which to operate. */
    private List<Assertion> assertions;
    
    /** The delegation requested state for the current request. */
    private DelegationRequest delegationRequested;
    
    /** The current RelyingPartyContext. */
    private RelyingPartyContext relyingPartyContext;
    
    /** Whether delegation is allowed for the current relying party. */
    private boolean delegationAllowed;
    
    /** The entityID of the local responder entity. */
    private String responderId;
    
    /** The entityID of the SAML relying party. */
    private String relyingPartyId;
    
    /** The RoleDescriptor for the SAML peer entity. */
    private RoleDescriptor roleDescriptor;
    
    /** The AttributeConsumingService for the SAML peer entity. */
    private AttributeConsumingService attributeConsumingService;
    
    /** Constructor. */
    public DecorateDelegatedAssertion() {
        super();
        
        relyingPartyContextLookupStrategy = new ChildContextLookup<>(RelyingPartyContext.class);
        samlMetadataContextLookupStrategy = new SAMLMetadataContextLookupFunction();
        assertionLookupStrategy = new AssertionStrategy();
        
    }
    
    /**
     * Set the URL at which the IdP will accept Liberty ID-WSF SSOS requests. 
     * 
     * @param url the Liberty ID-WSF SSOS endpoint URL
     */
    public void setLibertySSOSEndpointURL(@Nonnull final String url) {
        ComponentSupport.ifInitializedThrowUnmodifiabledComponentException(this);
        libertySSOSEndpointURL = Constraint.isNotNull(StringSupport.trimOrNull(url), 
                "Liberty SSOS endpoint URL may not be null");
    }
    
    /**
     * Set the strategy used to locate the current {@link RelyingPartyContext}.
     * 
     * @param strategy strategy used to locate the current {@link RelyingPartyContext}
     */
    public void setRelyingPartyContextLookupStrategy(
            @Nonnull final Function<ProfileRequestContext, RelyingPartyContext> strategy) {
        ComponentSupport.ifInitializedThrowUnmodifiabledComponentException(this);
        relyingPartyContextLookupStrategy = Constraint.isNotNull(strategy, 
                "RelyingPartyContext lookup strategy may not be null");
    }
    
    /**
     * Set the strategy used to locate the current {@link SAMLMetadataContext}.
     * 
     * @param strategy strategy used to locate the current {@link SAMLMetadataContext}
     */
    public void setSAMLMetadataContextLookupStrategy(
            @Nonnull final Function<ProfileRequestContext, SAMLMetadataContext> strategy) {
        ComponentSupport.ifInitializedThrowUnmodifiabledComponentException(this);
        samlMetadataContextLookupStrategy = Constraint.isNotNull(strategy, 
                "SAMLMetadataContext lookup strategy may not be null");
    }
    
    /**
     * Set the strategy used to locate the {@link Assertion} to operate on.
     * 
     * @param strategy strategy used to locate the {@link Assertion} to operate on
     */
    public void setAssertionLookupStrategy(@Nonnull final Function<ProfileRequestContext,List<Assertion>> strategy) {
        ComponentSupport.ifInitializedThrowUnmodifiabledComponentException(this);
        assertionLookupStrategy = Constraint.isNotNull(strategy, "Assertion lookup strategy may not be null");
    }
    
    /**
     * Set the {@link KeyInfoGeneratorManager} instance used to generate {@link KeyInfo}
     * from {@link Credential}.
     * 
     * @param manager the manager instance to use
     */
    public void setKeyInfoGeneratorManager(@Nonnull final NamedKeyInfoGeneratorManager manager) {
        ComponentSupport.ifInitializedThrowUnmodifiabledComponentException(this);
        keyInfoGeneratorManager = Constraint.isNotNull(manager, "NamedKeyInfoGeneratorManager may not be null");
    }
    
    /**
     * Set the {@link CredentialResolver} instance to use to resolve HoK {@link Credential}.
     * 
     * <p>
     * Typically this should be a metadata-based resolver which accepts input as the 
     * peer's {@link RoleDescriptor}.
     * </p>
     * 
     * @param resolver the resolver instance to use
     */
    public void setCredentialResolver(@Nonnull final CredentialResolver resolver) {
        ComponentSupport.ifInitializedThrowUnmodifiabledComponentException(this);
        credentialResolver = Constraint.isNotNull(resolver, "CredentialResolver may not be null");
    }
    
    /**
     * Get the effective default value for whether request processing should proceed 
     * with issuance of a delegation token.
     * 
     * @return the default value
     */
    @Nonnull public DelegationRequest getDefaultDelegationRequested() {
        return defaultDelegationRequested;
    }
    
    /**
     * Set the effective default value for whether request processing should proceed 
     * with issuance of a delegation token.
     * 
     * @param delegationRequest the default delegation requested value
     */
    public void setDefaultDelegationRequested(@Nonnull final DelegationRequest delegationRequest) {
        ComponentSupport.ifInitializedThrowUnmodifiabledComponentException(this);
        
        defaultDelegationRequested = 
                Constraint.isNotNull(delegationRequest, "Default DelegationRequest may not be null");
    }
    
    /** {@inheritDoc} */
    protected void doInitialize() throws ComponentInitializationException {
        super.doInitialize();
        if (relyingPartyContextLookupStrategy == null) {
            throw new ComponentInitializationException("RelyingPartyContext lookup strategy may not be null");
        }
        if (samlMetadataContextLookupStrategy == null) {
            throw new ComponentInitializationException("SAMLMetadataContext lookup strategy may not be null");
        }
        if (assertionLookupStrategy == null) {
            throw new ComponentInitializationException("Assertion lookup strategy may not be null");
        }
        if (keyInfoGeneratorManager == null) {
            throw new ComponentInitializationException("KeyInfoGeneratorManager may not be null");
        }
        if (credentialResolver == null) {
            throw new ComponentInitializationException("CredentialResolver may not be null");
        }
        if (libertySSOSEndpointURL == null) {
            throw new ComponentInitializationException("Liberty SSOS endpoint URL may not be null");
        }
    }

    /** {@inheritDoc} */
    protected boolean doPreExecute(@Nonnull final ProfileRequestContext profileRequestContext) {
        ComponentSupport.ifNotInitializedThrowUninitializedComponentException(this);
        
        relyingPartyContext = relyingPartyContextLookupStrategy.apply(profileRequestContext);
        if (relyingPartyContext == null) {
            log.warn("No RelyingPartyContext was available");
            ActionSupport.buildEvent(profileRequestContext, EventIds.INVALID_PROFILE_CTX);
            return false; 
        }
        
        relyingPartyId = relyingPartyContext.getRelyingPartyId();
        if (relyingPartyId == null) {
            log.warn("No relying party ID was available");
            ActionSupport.buildEvent(profileRequestContext, EventIds.INVALID_PROFILE_CTX);
            return false; 
        }
        
        if (relyingPartyContext.getProfileConfig() instanceof BrowserSSOProfileConfiguration) {
            delegationAllowed = ((BrowserSSOProfileConfiguration)relyingPartyContext.getProfileConfig())
                    .isAllowingDelegation();
        } else {
            log.warn("ProfileConfiguration is an invalid type: {}", 
                    relyingPartyContext.getProfileConfig().getClass().getName());
            ActionSupport.buildEvent(profileRequestContext, EventIds.INVALID_PROFILE_CTX);
            return false; 
        }
        
        // This is @Nonnull
        responderId = relyingPartyContext.getConfiguration().getResponderId();
        
        SAMLMetadataContext samlMetadataContext = samlMetadataContextLookupStrategy.apply(profileRequestContext);
        if (samlMetadataContext == null) {
            log.warn("No SAMLMetadataContext was available");
            ActionSupport.buildEvent(profileRequestContext, EventIds.INVALID_PROFILE_CTX);
            return false;
        }
        
        roleDescriptor = samlMetadataContext.getRoleDescriptor();
        if (roleDescriptor == null) {
            log.warn("No RoleDescriptor was available");
            ActionSupport.buildEvent(profileRequestContext, EventIds.INVALID_PROFILE_CTX);
            return false;
        }
        
        AttributeConsumingServiceContext acsContext = 
                samlMetadataContext.getSubcontext(AttributeConsumingServiceContext.class);
        attributeConsumingService = acsContext != null ? acsContext.getAttributeConsumingService() : null;
        if (attributeConsumingService == null) {
            log.debug("No AttributeConsumingService was resolved, won't be able to determine " 
                    + "delegation requested status via metadata");
        }
        
        delegationRequested = getDelegationRequested(profileRequestContext);
        if (DelegationRequest.NOT_REQUESTED.equals(delegationRequested)) {
            log.debug("Issuance of a delegated Assertion is not in effect, skipping further processing");
            return false;
        }
        
        assertions = assertionLookupStrategy.apply(profileRequestContext);
        if (assertions == null || assertions.isEmpty()) {
            log.debug("No Assertions found to decorate, skipping further processing");
            return false;
        }
        
        return super.doPreExecute(profileRequestContext);
    }

    /** {@inheritDoc} */
    protected void doExecute(@Nonnull final ProfileRequestContext profileRequestContext) {
        ComponentSupport.ifNotInitializedThrowUninitializedComponentException(this);
        
        try {
            switch (delegationRequested) {
                case NOT_REQUESTED:
                    log.debug("Delegation was not requested, skipping delegation decoration");
                    break;
                case REQUESTED_OPTIONAL:
                    if (delegationAllowed) {
                        log.debug("Delegation token issuance was requested (optional) and allowed");
                        decorateDelegatedAssertion(profileRequestContext);
                    } else {
                        log.debug("Delegation token issuance was requested (optional), but not allowed, " 
                                + "skipping delegation decoration");
                        return;
                    }
                    break;
                case REQUESTED_REQUIRED:
                    if (delegationAllowed) {
                        log.debug("Delegation token issuance was requested (required) and allowed");
                        decorateDelegatedAssertion(profileRequestContext);
                    } else {
                        log.warn("Delegation token issuance was requested (required), but disallowed by policy");
                        //TODO what is right event type here - need to define new one?
                        ActionSupport.buildEvent(profileRequestContext, EventIds.ACCESS_DENIED);
                    }
                    break;
                default:
                    log.error("Unknown value '{}' for delegation request state", delegationRequested);
            }
        } catch (EventException e) {
            if (Objects.equals(EventIds.PROCEED_EVENT_ID, e.getEvent())) {
                log.debug("Decoration of Assertion for delegation terminated with explicit proceed signal");
            } else {
                log.warn("Decoration of Assertion for delegation terminated with explicit non-proceed signal", e);
                ActionSupport.buildEvent(profileRequestContext, e.getEvent());
            }
        }
    }
    
    /**
     * Decorate the Assertion to allow use as a delegated security token by the SAML requester.
     * 
     * @param requestContext the current request context
     */
    private void decorateDelegatedAssertion(@Nonnull final ProfileRequestContext requestContext) {
        
        if (assertions == null) {
            log.debug("No assertions found to decorate, nothing to do.");
            return;
        }
        
        for (Assertion assertion : assertions) {
            addSAMLPeerSubjectConfirmation(requestContext, assertion);
            addIdPAudienceRestriction(requestContext, assertion);
            addLibertySSOSEPRAttribute(requestContext, assertion);
        }
    }

    /**
     * Add Liberty SSOS service Endpoint Reference (EPR) attribute to Assertion's AttributeStatement.
     * 
     * @param requestContext the current request context
     * @param assertion the delegated assertion being issued
     */
    private void addLibertySSOSEPRAttribute(@Nonnull final ProfileRequestContext requestContext, 
            @Nonnull final Assertion assertion) {
        Attribute attribute = (Attribute) XMLObjectSupport.buildXMLObject(Attribute.DEFAULT_ELEMENT_NAME);
        attribute.setName(LibertyConstants.SERVICE_TYPE_SSOS);
        attribute.setNameFormat(Attribute.URI_REFERENCE);
        attribute.getAttributeValues().add(buildLibertSSOSEPRAttributeValue(requestContext, assertion));
        
        List<AttributeStatement> attributeStatements = assertion.getAttributeStatements();
        AttributeStatement attributeStatement = null;
        if (attributeStatements.isEmpty()) {
            attributeStatement = 
                    (AttributeStatement) XMLObjectSupport.buildXMLObject(AttributeStatement.DEFAULT_ELEMENT_NAME);
            assertion.getAttributeStatements().add(attributeStatement);
        } else {
            attributeStatement = attributeStatements.get(0);
        }
        attributeStatement.getAttributes().add(attribute);
    }

    /**
     * Build the Liberty SSOS EPR AttributeValue object.
     * 
     * @param requestContext the current request context
     * @param assertion the delegated assertion being issued
     * 
     * @return the AttributeValue object containing the EPR
     */
    @Nonnull private XMLObject buildLibertSSOSEPRAttributeValue(@Nonnull final ProfileRequestContext requestContext, 
            @Nonnull final Assertion assertion) {
        
        Address address = (Address) XMLObjectSupport.buildXMLObject(Address.ELEMENT_NAME);
        address.setValue(libertySSOSEndpointURL);
        
        MetadataAbstract libertyAbstract = (MetadataAbstract) XMLObjectSupport.buildXMLObject(
                LibertyConstants.DISCO_ABSTRACT_ELEMENT_NAME);
        libertyAbstract.setValue(LibertyConstants.SSOS_EPR_METADATA_ABSTRACT);
        
        ServiceType serviceType = (ServiceType) XMLObjectSupport.buildXMLObject(
                LibertyConstants.DISCO_SERVICE_TYPE_ELEMENT_NAME);
        serviceType.setValue(LibertyConstants.SERVICE_TYPE_SSOS);
        
        ProviderID providerID = (ProviderID) XMLObjectSupport.buildXMLObject(
                LibertyConstants.DISCO_PROVIDERID_ELEMENT_NAME);
        providerID.setValue(responderId);
        
        Framework framework = (Framework) XMLObjectSupport.buildXMLObject(Framework.DEFAULT_ELEMENT_NAME);
        framework.setVersion("2.0");
        
        SecurityMechID securityMechID  = (SecurityMechID) XMLObjectSupport.buildXMLObject(
                LibertyConstants.DISCO_SECURITY_MECH_ID_ELEMENT_NAME);
        securityMechID.setValue(LibertyConstants.SECURITY_MECH_ID_CLIENT_TLS_PEER_SAML_V2);
        
        Token token = (Token) XMLObjectSupport.buildXMLObject(LibertyConstants.SECURITY_TOKEN_ELEMENT_NAME);
        token.setUsage(LibertyConstants.TOKEN_USAGE_SECURITY_TOKEN);
        token.setRef("#" + assertion.getID());
        
        SecurityContext securityContext = (SecurityContext) XMLObjectSupport.buildXMLObject(
                LibertyConstants.DISCO_SECURITY_CONTEXT_ELEMENT_NAME);
        securityContext.getSecurityMechIDs().add(securityMechID);
        securityContext.getTokens().add(token);
        
        Metadata metadata = (Metadata) XMLObjectSupport.buildXMLObject(Metadata.ELEMENT_NAME);
        metadata.getUnknownXMLObjects().add(libertyAbstract);
        metadata.getUnknownXMLObjects().add(serviceType);
        metadata.getUnknownXMLObjects().add(providerID);
        metadata.getUnknownXMLObjects().add(framework);
        metadata.getUnknownXMLObjects().add(securityContext);
        
        EndpointReference epr = (EndpointReference) XMLObjectSupport.buildXMLObject(EndpointReference.ELEMENT_NAME);
        epr.setAddress(address);
        epr.setMetadata(metadata);
        
        XMLObjectBuilder<XSAny> xsAnyBuilder = (XMLObjectBuilder<XSAny>) XMLObjectSupport.getBuilder(XSAny.TYPE_NAME);
        XSAny attributeValue = xsAnyBuilder.buildObject(AttributeValue.DEFAULT_ELEMENT_NAME);
        attributeValue.getUnknownXMLObjects().add(epr);
        
        return attributeValue;
    }

    /**
     * An an AudienceRestriction condition indicating the IdP as an acceptable Audience.
     * 
     * @param requestContext the current request context
     * @param assertion the assertion being isued
     */
    private void addIdPAudienceRestriction(@Nonnull final ProfileRequestContext requestContext, 
            @Nonnull final Assertion assertion) {
        
        SAML2ActionSupport.addConditionsToAssertion(this, assertion);
        
        List<AudienceRestriction> audienceRestrictions = assertion.getConditions().getAudienceRestrictions();
        AudienceRestriction audienceRestriction = null;
        if (audienceRestrictions.isEmpty()) {
            audienceRestriction = (AudienceRestriction) XMLObjectSupport.buildXMLObject(
                    AudienceRestriction.DEFAULT_ELEMENT_NAME);
            assertion.getConditions().getAudienceRestrictions().add(audienceRestriction);
        } else {
            audienceRestriction = audienceRestrictions.get(0);
        }
        
        // Sanity check that IdP audience has not already been added by other code.
        for (Audience audience : audienceRestriction.getAudiences()) {
            if (Objects.equals(responderId, StringSupport.trimOrNull(audience.getAudienceURI()))) {
                log.debug("Local entity ID '{}' already present in assertion AudienceRestriction set, skipping",
                        responderId);
                return;
            }
        }
        
        Audience idpAudience = (Audience) XMLObjectSupport.buildXMLObject(Audience.DEFAULT_ELEMENT_NAME);
        idpAudience.setAudienceURI(responderId);
        audienceRestriction.getAudiences().add(idpAudience);
    }

    /**
     * Add SubjectConfirmation to the Assertion Subject to allow confirmation when wielded by the SAML requester.
     * 
     * @param requestContext the current request context
     * @param assertion the assertion being issued
     */
    private void addSAMLPeerSubjectConfirmation(@Nonnull final ProfileRequestContext requestContext,
            @Nonnull final Assertion assertion) {
        
        // Add holder-of-key confirmation for all signing keys present for SP, typically from metadata
        CriteriaSet criteriaSet = new CriteriaSet();
        criteriaSet.add(new RoleDescriptorCriterion(roleDescriptor));
        criteriaSet.add(new UsageCriterion(UsageType.SIGNING));
        // Add an entityID criterion just in case don't have a MetadataCredentialResolver,
        // and want to resolve via entityID + usage only, e.g. from a CollectionCredentialResolver
        // or other more general resolver type.
        criteriaSet.add(new EntityIdCriterion(relyingPartyId));
        
        try {
            addHoKSubjectConfirmation(assertion, credentialResolver.resolve(criteriaSet).iterator());
        } catch (ResolverException e) {
            log.warn("Error resolving holder-of-key credentials for SP '{}': {})", relyingPartyId, e.getMessage());
            throw new EventException(EventIds.MESSAGE_PROC_ERROR, "Error resolving holder-of-key credentials", e);
        }
        
    }

    /**
     * Add a holder-of-key SubjectConfirmation for the SAML peer entity ID. The KeyInfoConfirmationDataType
     * SubjectConfirmationData will contain a KeyInfo for each resolved credential;
     * 
     * @param assertion the assertion to be updated
     * @param credIterator the iterator of resolved credentials
     */
    private void addHoKSubjectConfirmation(@Nonnull final Assertion assertion, 
            @Nonnull final Iterator<Credential> credIterator) {
        
        if (!credIterator.hasNext()) {
            log.warn("No credentials were available from SP '{}' for HoK assertion", relyingPartyId);
            //TODO this is perhaps the wrong event ID
            //TODO perhaps this shouldn't be fatal. Since we haven't mutated the assertion yet, could
            //     maybe just return 'proceed'.
            throw new EventException(EventIds.MESSAGE_PROC_ERROR,
                    "No credentials were available for creating HoK subject confirmation");
        }
        
        KeyInfoConfirmationDataType scData = 
                (KeyInfoConfirmationDataType) XMLObjectSupport.getBuilder(KeyInfoConfirmationDataType.TYPE_NAME)
                .buildObject(SubjectConfirmationData.DEFAULT_ELEMENT_NAME, KeyInfoConfirmationDataType.TYPE_NAME);
        
        //TODO could support some strategy for using different named managers, rather than always the default manager.
        KeyInfoGeneratorManager kigm = keyInfoGeneratorManager.getDefaultManager();
        
        while (credIterator.hasNext()) {
            Credential cred = credIterator.next();
            KeyInfoGeneratorFactory kigf = kigm.getFactory(cred);
            KeyInfoGenerator kig = kigf.newInstance();
            try {
                KeyInfo keyInfo = kig.generate(cred);
                scData.getKeyInfos().add(keyInfo);
            } catch (SecurityException e) {
                log.warn("Error generating KeyInfo from peer credential: {}", e.getMessage());
                throw new EventException(EventIds.MESSAGE_PROC_ERROR, "Error generating KeyInfo from credential", e);
            }
        }
        
        NameID nameID = (NameID) XMLObjectSupport.buildXMLObject(NameID.DEFAULT_ELEMENT_NAME);
        nameID.setValue(relyingPartyId);
        nameID.setFormat(NameID.ENTITY);
        
        SubjectConfirmation sc = (SubjectConfirmation) XMLObjectSupport.buildXMLObject(
                SubjectConfirmation.DEFAULT_ELEMENT_NAME);
        sc.setMethod(SubjectConfirmation.METHOD_HOLDER_OF_KEY);
        sc.setNameID(nameID);
        sc.setSubjectConfirmationData(scData);
        
        Subject subject = assertion.getSubject();
        if (subject==null) {
            subject = (Subject) XMLObjectSupport.buildXMLObject(Subject.DEFAULT_ELEMENT_NAME);
            assertion.setSubject(subject);
        }
        subject.getSubjectConfirmations().add(sc);
    }

    /**
     * Check whether issuance of a delegated token has been requested.
     * 
     * @param requestContext the current request context
     * @return true if delegation is requested, false otherwise
     */
    private DelegationRequest getDelegationRequested(@Nonnull final ProfileRequestContext requestContext) {
        if (isDelegationRequestedByAudience(requestContext)) {
            log.debug("Delegation was requested via AuthnRequest Audience, treating as: {}", 
                    DelegationRequest.REQUESTED_REQUIRED);
            return DelegationRequest.REQUESTED_REQUIRED;
        }
        
        DelegationRequest requestedByMetadata = getDelegationRequestedByMetadata(requestContext);
        if (requestedByMetadata != DelegationRequest.NOT_REQUESTED) {
            log.debug("Delegation was requested via metadata: {}", requestedByMetadata);
            return requestedByMetadata;
        }
        
        log.debug("Delegation request was not explicitly indicated, using default value: {}", 
                getDefaultDelegationRequested());
        return getDefaultDelegationRequested();
    }
    

    /**
     * Determine whether a delegation token was requested via the SP's SPSSODescriptor AttributeConsumingService.
     * 
     * @param requestContext the current request context
     * @return DelegationRequest enum value as appropriate
     */
    @Nonnull private DelegationRequest getDelegationRequestedByMetadata(
            @Nonnull final ProfileRequestContext requestContext) {
        
        if (attributeConsumingService == null) {
            log.debug("No AttributeConsumingService was available");
            return DelegationRequest.NOT_REQUESTED;
        }
        
        for (RequestedAttribute requestedAttribute : attributeConsumingService.getRequestAttributes()) {
            if (Objects.equals(LibertyConstants.SERVICE_TYPE_SSOS, 
                    StringSupport.trimOrNull(requestedAttribute.getName()))) {
                log.debug("Saw requested attribute '{}' in metadata AttributeConsumingService for SP: {}",
                        LibertyConstants.SERVICE_TYPE_SSOS, relyingPartyId);
                if (requestedAttribute.isRequired()) {
                    log.debug("Metadata delegation request attribute indicated it was required");
                    return DelegationRequest.REQUESTED_REQUIRED;
                } else {
                    log.debug("Metadata delegation request attribute indicated it was NOT required");
                    return DelegationRequest.REQUESTED_OPTIONAL;
                }
            }
        }
        
        return DelegationRequest.NOT_REQUESTED;
    }

    /**
     * Determine whether a delegation token was requested via the inbound AuthnRequest's
     * Conditions' AudienceRestriction.
     * 
     * @param requestContext the current request context
     * @return true if the AudienceRestrictions condition contained the local entity Id, false otherwise
     */
    private boolean isDelegationRequestedByAudience(@Nonnull final ProfileRequestContext requestContext) {
        if (!(requestContext.getInboundMessageContext().getMessage() instanceof AuthnRequest)) {
            log.debug("Inbound SAML message was not an AuthnRequest: {}", 
                    requestContext.getInboundMessageContext().getMessage().getClass().getName());
            return false;
        }
        
        AuthnRequest authnRequest = (AuthnRequest) requestContext.getInboundMessageContext().getMessage();
        if (authnRequest.getConditions() != null) {
            Conditions conditions = authnRequest.getConditions();
            for (AudienceRestriction ar : conditions.getAudienceRestrictions()) {
                for (Audience audience : ar.getAudiences()) {
                    String audienceValue = StringSupport.trimOrNull(audience.getAudienceURI());
                    if (Objects.equals(audienceValue, responderId)) {
                        log.debug("Saw an AuthnRequest/Conditions/AudienceRestriction/Audience with value of '{}'",
                                responderId);
                        return true;
                    }
                }
            }
        }
        return false;
    }
    
    //TODO look at how this action, and/or either delegation issuance generally, influences encryption and signing
    //  -- some of this action's eval may need to happen earlier, so can resolve -Parameters in the context
    
    /**
    protected boolean isEncryptNameID(BaseSAML2ProfileRequestContext<?, ?, ?> requestContext) throws ProfileException {
        if (isDelegationIssuanceActive(requestContext)) {
            if (isRequestRequiresEncryptNameID(requestContext)) {
                log.warn("Issuance of a delegation token is active, and request indicated an encrypted NameID, ",
                        "which is currently unsupported");
                throw new ProfileException("Unable to issue delegation token with encrypted NameID");
            } else {
                log.debug("Issuance of a delegation token is active, " 
                        + "overriding eval as to encrypting NameID to: false");
                return false;
            }
        } else {
            log.debug("Issuance of a delegation token is not active, " 
                    + "proceeding with normal eval as to encrypting NameID");
            return super.isEncryptNameID(requestContext);
        }
    }

    protected boolean isSignAssertion(BaseSAML2ProfileRequestContext<?, ?, ?> requestContext) throws ProfileException {
        if (isDelegationIssuanceActive(requestContext)) {
            log.debug("Issuance of a delegation token is active, " 
                    + "overriding eval as to signing assertion to: true");
            return true;
        } else {
            log.debug("Issuance of a delegation token is not active, " 
                    + "proceeding with normal eval as to signing assertion");
            return super.isSignAssertion(requestContext);
        }
    }
    */
    
    /**
     * Determine whether issuance of a delegation token is effectively active.
     * @param requestContext the current request context
     * @return true if issuance of a delegation token is in effect for the current request, false otherwise
     */
    /**
    protected boolean isDelegationIssuanceActive(ProfileRequestContext requestContext) {
        return delegationRequested != DelegationRequest.NOT_REQUESTED 
                && isDelegationAllowed(requestContext);
    }
    */
    
    /**
     * Default strategy for obtaining assertion to modify.
     * 
     * <p>If the outbound context is empty, a new assertion is created and stored there. If the outbound
     * message is already an assertion, it's returned. If the outbound message is a response, then either
     * an existing or new assertion in the response is returned, depending on the action setting. If the
     * outbound message is anything else, null is returned.</p>
     */
    private class AssertionStrategy implements Function<ProfileRequestContext,List<Assertion>> {

        /** {@inheritDoc} */
        @Override
        @Nullable public List<Assertion> apply(@Nullable final ProfileRequestContext input) {
            if (input != null && input.getOutboundMessageContext() != null) {
                final Object outboundMessage = input.getOutboundMessageContext().getMessage();
                if (outboundMessage == null) {
                    log.debug("No outbound message found, nothing to decorate");
                    return Collections.emptyList();
                } else if (outboundMessage instanceof Assertion) {
                    log.debug("Found Assertion to decorate as outbound message");
                    return Collections.singletonList((Assertion) outboundMessage);
                } else if (outboundMessage instanceof Response) {
                    Response response = (Response) outboundMessage;
                    if (response.getAssertions().isEmpty()) {
                        log.debug("Outbound Response contained no Assertions, nothing to decorate");
                        return Collections.emptyList();
                    } else { 
                        //TODO What should be approach when have 2+ Assertions?  See other actions' options for details.
                        log.debug("Found Assertion to decorate in outbound Response");
                        return Collections.singletonList(response.getAssertions().get(0));
                    }
                } else {
                    log.debug("Found no Assertion to decorate");
                    return null;
                }
            } else {
                log.debug("Input ProfileRequestContext or outbound MessageContext was null");
                return null;
            }
        }
        
    }
    
    /**
     * Internal runtime exception class used to terminate processing and communicate 
     * a failure event up the call stack to a common location for production of the action event to 
     * be returned.
     */
    private static class EventException extends RuntimeException {
        
        /** Serial version UID. */
        private static final long serialVersionUID = -9159689696046606020L;
        
        /** The event ID. */
        private final String eventID;

        /**
         * Constructor.
         *
         * @param event the event ID
         * @param message the exception details message
         * @param cause the exception cause
         */
        public EventException(@Nonnull final String event, @Nullable final String message, 
                @Nullable final Throwable cause) {
            super(message, cause);
            eventID = Constraint.isNotNull(StringSupport.trimOrNull(event), "Event ID may not be null");
        }

        /**
         * Constructor.
         *
         * @param event the event ID
         * @param message the exception details message
         */
        public EventException(@Nonnull final String event, @Nullable final String message) {
            super(message);
            eventID = Constraint.isNotNull(StringSupport.trimOrNull(event), "Event ID may not be null");
        }
        
        /**
         * Get the event represented by this exception.
         * 
         * @return the event ID
         */
        @Nonnull public String getEvent() {
            return eventID;
        }
        
    }
}
