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

import java.util.Iterator;
import java.util.List;
import java.util.Objects;

import javax.annotation.Nonnull;

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
import org.opensaml.core.xml.XMLObject;
import org.opensaml.core.xml.XMLObjectBuilder;
import org.opensaml.core.xml.schema.XSAny;
import org.opensaml.core.xml.util.XMLObjectSupport;
import org.opensaml.messaging.context.navigate.ChildContextLookup;
import org.opensaml.profile.action.ActionSupport;
import org.opensaml.profile.action.EventIds;
import org.opensaml.profile.context.ProfileRequestContext;
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
import org.opensaml.saml.saml2.core.Subject;
import org.opensaml.saml.saml2.core.SubjectConfirmation;
import org.opensaml.saml.saml2.core.SubjectConfirmationData;
import org.opensaml.saml.saml2.metadata.AttributeConsumingService;
import org.opensaml.saml.saml2.metadata.RequestedAttribute;
import org.opensaml.saml.saml2.metadata.RoleDescriptor;
import org.opensaml.saml.security.impl.MetadataCredentialResolver;
import org.opensaml.security.SecurityException;
import org.opensaml.security.credential.Credential;
import org.opensaml.security.credential.UsageType;
import org.opensaml.security.criteria.UsageCriterion;
import org.opensaml.soap.wsaddressing.Address;
import org.opensaml.soap.wsaddressing.EndpointReference;
import org.opensaml.soap.wsaddressing.Metadata;
import org.opensaml.xmlsec.keyinfo.KeyInfoGenerator;
import org.opensaml.xmlsec.keyinfo.KeyInfoGeneratorFactory;
import org.opensaml.xmlsec.keyinfo.KeyInfoGeneratorManager;
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
    
    /** Strategy used to lookup the RelyingPartyContext. */
    @Nonnull private Function<ProfileRequestContext,RelyingPartyContext> relyingPartyContextLookupStrategy;
    
    /** Strategy used to lookup the SAMLMetadataContext. */
    @Nonnull private Function<ProfileRequestContext, SAMLMetadataContext> samlMetadataContextLookupStrategy;
    
    /** Strategy used to lookup the AttributeConsumingService. */
    @Nonnull private Function<ProfileRequestContext, AttributeConsumingService> attributeConsumingServiceLookupStrategy;
    
    /** Strategy used to locate the {@link Assertion}s on which to operate. */
    @Nonnull private Function<ProfileRequestContext,List<Assertion>> assertionLookupStrategy;
    
    /** The manager used to generated KeyInfo instances from Credentials. */
    @Nonnull private KeyInfoGeneratorManager keyInfoGeneratorManager;
    
    /** The metadata credential resolver used to resolve HoK Credentials for the peer. */
    @Nonnull private MetadataCredentialResolver metadataCredentialResolver;
    
    
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
        
        //TODO need default impl?
        //attributeConsumingServiceLookupStrategy = null
        
        //TODO need default impl
        //assertionLookupStrategy = new AssertionStrategy();
        
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
     * Set the strategy used to locate the current {@link AttributeConsumingService}.
     * 
     * @param strategy strategy used to locate the current {@link AttributeConsumingService}
     */
    public void setAttributeConsumingServiceLookupStrategy(
            @Nonnull final Function<ProfileRequestContext, AttributeConsumingService> strategy) {
        ComponentSupport.ifInitializedThrowUnmodifiabledComponentException(this);
        attributeConsumingServiceLookupStrategy = Constraint.isNotNull(strategy, 
                "AttributeConsumingServicelookup strategy may not be null");
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
    public void setKeyInfoGeneratorManager(@Nonnull final KeyInfoGeneratorManager manager) {
        ComponentSupport.ifInitializedThrowUnmodifiabledComponentException(this);
        keyInfoGeneratorManager = Constraint.isNotNull(manager, "KeyInfoGeneratorManager may not be null");
    }
    
    /**
     * Set the {@link MetadataCredentialResolver} instance to use to resolve HoK {@link Credential} 
     * from the peer's {@link RoleDescriptor}.
     * 
     * @param resolver the resolver instance to use
     */
    public void setMetadataCredentialResolver(@Nonnull final MetadataCredentialResolver resolver) {
        ComponentSupport.ifInitializedThrowUnmodifiabledComponentException(this);
        metadataCredentialResolver = Constraint.isNotNull(resolver, "MetadataCredentialResolver may not be null");
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
        if (attributeConsumingServiceLookupStrategy == null) {
            throw new ComponentInitializationException("AttributeConsumingService lookup strategy may not be null");
        }
        if (assertionLookupStrategy == null) {
            throw new ComponentInitializationException("Assertion lookup strategy may not be null");
        }
        if (keyInfoGeneratorManager == null) {
            throw new ComponentInitializationException("KeyInfoGeneratorManager may not be null");
        }
        if (metadataCredentialResolver == null) {
            throw new ComponentInitializationException("MetadataCredentialResolver may not be null");
        }
    }

    /** {@inheritDoc} */
    protected boolean doPreExecute(@Nonnull final ProfileRequestContext profileRequestContext) {
        //TODO do we need to do store a proceed event in the case where processing should successfully be skipped?
        
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
        }
        
        roleDescriptor = samlMetadataContext.getRoleDescriptor();
        if (roleDescriptor == null) {
            log.warn("No RoleDescriptor was available");
            ActionSupport.buildEvent(profileRequestContext, EventIds.INVALID_PROFILE_CTX);
        }
        
        attributeConsumingService = attributeConsumingServiceLookupStrategy.apply(profileRequestContext);
        if (attributeConsumingService == null) {
            log.debug("No AttributeConsumingService was resolved, won't be able to determine " 
                    + "delegation requested status via metadata");
        }
        
        return super.doPreExecute(profileRequestContext);
    }

    /** {@inheritDoc} */
    protected void doExecute(@Nonnull final ProfileRequestContext profileRequestContext) {
        
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
                    //TODO error reporting
                    /**
                    requestContext.setFailureStatus(buildStatus(StatusCode.REQUESTER_URI, StatusCode.REQUEST_DENIED_URI,
                            "A delegation token was requested but was disallowed by policy"));
                    throw new ProfileException("Delegation was requested and required, but disallowed by policy");
                    */
                }
                break;
            default:
                log.error("Unknown value '{}' for delegation request state", delegationRequested);
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
        address.setValue(getIdPEPRAddress(requestContext));
        
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
     * Get the endpoint address for the IdP's Liberty ID-WSF SSOS service endpoint.
     * 
     * @param requestContext the current request context
     * @return the ID-WSF SSOS endpoint address
     */
    @Nonnull private String getIdPEPRAddress(@Nonnull final ProfileRequestContext requestContext) {
        //TODO need new approach, don't have own metadata anymore
        return null;
        
        /*
        BasicEndpointSelector endpointSelector = new BasicEndpointSelector();
        endpointSelector.setEndpointType(SingleSignOnService.DEFAULT_ELEMENT_NAME);
        endpointSelector.setEntityRoleMetadata(requestContext.getLocalEntityRoleMetadata());
        endpointSelector.getSupportedIssuerBindings().add(LibertyConstants.SOAP_BINDING_20_URI);
        
        Endpoint endpoint = endpointSelector.selectEndpoint();
        if (endpoint == null || DatatypeHelper.isEmpty(endpoint.getLocation())) {
            log.error("The IdP's Liberty ID-WSF SSOS service endpoint address could not be resolved");
            requestContext.setFailureStatus(buildStatus(StatusCode.RESPONDER_URI, null, INTERNAL_ERR_MSG));
            throw new ProfileException("IdP Liberty ID-WSF SSOS service endpoint address could not be resolved");
        }
        return endpoint.getLocation();
        */
    }

    /**
     * An an AudienceRestriction condition indicating the IdP as an acceptable Audience.
     * 
     * @param requestContext the current request context
     * @param assertion the assertion being isued
     */
    private void addIdPAudienceRestriction(@Nonnull final ProfileRequestContext requestContext, 
            @Nonnull final Assertion assertion) {
        
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
        
        // Add holder-of-key confirmation for all signing keys present for SP in metadata
        CriteriaSet criteriaSet = new CriteriaSet();
        criteriaSet.add(new RoleDescriptorCriterion(roleDescriptor));
        criteriaSet.add(new UsageCriterion(UsageType.SIGNING));
        
        try {
            addHoKSubjectConfirmation(assertion, metadataCredentialResolver.resolve(criteriaSet).iterator());
        } catch (ResolverException e) {
            log.error("Error resolving holder-of-key credentials for SP '{}': {})", relyingPartyId, e.getMessage());
            //TODO error handling
            return;
            //requestContext.setFailureStatus(buildStatus(StatusCode.RESPONDER_URI, null, INTERNAL_ERR_MSG));
            //throw new ProfileException("Error resolving holder-of-key credentials", e);
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
            log.error("No credentials were available from SP '{}' for HoK assertion", relyingPartyId);
            //TODO error reporting
            //throw new ProfileException("No credentials were available for creating HoK subject confirmation");
            return;
        }
        
        KeyInfoConfirmationDataType scData = (KeyInfoConfirmationDataType) XMLObjectSupport.buildXMLObject(
                SubjectConfirmationData.DEFAULT_ELEMENT_NAME, KeyInfoConfirmationDataType.TYPE_NAME);
        
        while (credIterator.hasNext()) {
            Credential cred = credIterator.next();
            KeyInfoGeneratorFactory kigf = keyInfoGeneratorManager.getFactory(cred);
            KeyInfoGenerator kig = kigf.newInstance();
            try {
                KeyInfo keyInfo = kig.generate(cred);
                scData.getKeyInfos().add(keyInfo);
            } catch (SecurityException e) {
                log.error("Error generating KeyInfo from peer credential: {}", e.getMessage());
                //TODO error reporting
                //throw new ProfileException("Error generating KeyInfo from credential", e);
                return;
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
}
