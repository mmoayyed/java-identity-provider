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

import java.util.Collections;
import java.util.List;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import net.shibboleth.idp.profile.AbstractProfileAction;
import net.shibboleth.utilities.java.support.component.ComponentSupport;
import net.shibboleth.utilities.java.support.logic.Constraint;

import org.opensaml.core.xml.XMLObject;
import org.opensaml.core.xml.schema.XSAny;
import org.opensaml.core.xml.schema.XSString;
import org.opensaml.messaging.context.navigate.ChildContextLookup;
import org.opensaml.profile.action.ActionSupport;
import org.opensaml.profile.context.ProfileRequestContext;
import org.opensaml.profile.context.navigate.InboundMessageContextLookup;
import org.opensaml.saml.common.messaging.context.SAMLMetadataContext;
import org.opensaml.saml.common.messaging.context.SAMLPeerEntityContext;
import org.opensaml.saml.common.profile.SAMLEventIds;
import org.opensaml.saml.ext.saml2mdattr.EntityAttributes;
import org.opensaml.saml.saml2.core.Assertion;
import org.opensaml.saml.saml2.core.Attribute;
import org.opensaml.saml.saml2.core.AttributeStatement;
import org.opensaml.saml.saml2.core.Response;
import org.opensaml.saml.saml2.metadata.EntityDescriptor;
import org.opensaml.saml.saml2.metadata.Extensions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Function;
import com.google.common.base.Functions;

/**
 * Action that checks an SP's metadata for an {@link EntityAttribute} extension that signals
 * a requirement for a standardized form of subject identifier attribute and ensures that a
 * failure to meet it results in a specific error.
 * 
 * @event {@link org.opensaml.profile.action.EventIds#PROCEED_EVENT_ID}
 * @event {@link SAMLEventIds#SUBJECT_ID_REQ_FAILED}
 * 
 * @since 3.4.0
 */
public class EnforceSubjectIDRequirement extends AbstractProfileAction {

    /** Tag name. */
    @Nonnull private static final String REQUIREMENT_TAG_NAME = "urn:oasis:names:tc:SAML:profiles:subject-id:req";

    /** subject-id name. */
    @Nonnull private static final String SUBJECT_ID_ATTR_NAME = "urn:oasis:names:tc:SAML:attribute:subject-id";

    /** pairwise-id name. */
    @Nonnull private static final String PAIRWISE_ID_ATTR_NAME = "urn:oasis:names:tc:SAML:attribute:pairwise-id";

    /** Class logger. */
    @Nonnull private final Logger log = LoggerFactory.getLogger(EnforceSubjectIDRequirement.class);

    /**
     * Strategy used to locate the {@link SAMLMetadataContext} associated with a given {@link ProfileRequestContext}.
     */
    @Nonnull private Function<ProfileRequestContext,SAMLMetadataContext> metadataContextLookupStrategy;

    /** Strategy used to locate the {@link Assertion} objects to operate on. */
    @Nonnull private Function<ProfileRequestContext,List<Assertion>> assertionsLookupStrategy;
    
    /** Requirement in SP's metadata, if any. */
    @Nullable private String requirement;
    
    /** Track existence of subject-id. */
    @Nullable private Attribute subjectIDAttribute;
    
    /** Track existence of pairwise-id. */
    @Nullable private Attribute pairwiseIDAttribute;

    /** Constructor. */
    public EnforceSubjectIDRequirement() {
        // Default: inbound msg context -> SAMLPeerEntityContext -> SAMLMetadataContext
        metadataContextLookupStrategy = Functions.compose(
                new ChildContextLookup<>(SAMLMetadataContext.class),
                Functions.compose(new ChildContextLookup<>(SAMLPeerEntityContext.class),
                        new InboundMessageContextLookup()));
        assertionsLookupStrategy = new AssertionStrategy();
    }
    
    /**
     * Set the strategy used to locate the {@link SAMLMetadataContext} associated with a given
     * {@link ProfileRequestContext}.  Also sets the strategy to find the {@link SAMLMetadataContext}
     * from the {@link AttributeFilterContext}.
     * 
     * @param strategy lookup strategy
     */
    public void setMetadataContextLookupStrategy(
            @Nonnull final Function<ProfileRequestContext,SAMLMetadataContext> strategy) {
        ComponentSupport.ifInitializedThrowUnmodifiabledComponentException(this);

        metadataContextLookupStrategy =
                Constraint.isNotNull(strategy, "MetadataContext lookup strategy cannot be null");
    }

    /**
     * Set the strategy used to locate the {@link Assertion}s to operate on.
     * 
     * @param strategy lookup strategy
     */
    public void setAssertionsLookupStrategy(@Nonnull final Function<ProfileRequestContext,List<Assertion>> strategy) {
        ComponentSupport.ifInitializedThrowUnmodifiabledComponentException(this);

        assertionsLookupStrategy = Constraint.isNotNull(strategy, "Assertions lookup strategy cannot be null");
    }

    /** {@inheritDoc} */
    @Override
    protected boolean doPreExecute(@Nonnull final ProfileRequestContext profileRequestContext) {
        
        if (!super.doPreExecute(profileRequestContext)) {
            return false;
        }
        
        // Check for requirement in metadata.
        final SAMLMetadataContext metadataCtx = metadataContextLookupStrategy.apply(profileRequestContext);
        if (metadataCtx != null && metadataCtx.getEntityDescriptor() != null) {
            requirement = extractRequirement(metadataCtx.getEntityDescriptor());
        }
        
        if (requirement == null) {
            log.debug("{} No subject-id requirement in metadata, continuing", getLogPrefix());
            return false;
        } else if ("none".equals(requirement)) {
            log.debug("{} Metadata indicates no subject-id required, continuing", getLogPrefix());
            return false;
        }
        
        return true;
    }
    
    /** {@inheritDoc} */
    @Override
    protected void doExecute(@Nonnull final ProfileRequestContext profileRequestContext) {

        
        log.debug("{} Enforcing subject identifier requirement '{}'", getLogPrefix(), requirement);

        findAttributes(profileRequestContext);
        
        if ("subject-id".equals(requirement)) {
            if (subjectIDAttribute == null) {
                log.info("{} Requirement for 'subject-id' not satisfied", getLogPrefix());
                ActionSupport.buildEvent(profileRequestContext, SAMLEventIds.SUBJECT_ID_REQ_FAILED);
            }
        } else if ("pairwise-id".equals(requirement)) {
            if (pairwiseIDAttribute == null) {
                log.info("{} Requirement for 'pairwise-id' not satisfied", getLogPrefix());
                ActionSupport.buildEvent(profileRequestContext, SAMLEventIds.SUBJECT_ID_REQ_FAILED);
            }
        } else if ("any".equals(requirement)) {
            if (subjectIDAttribute == null && pairwiseIDAttribute == null) {
                log.info("{} Requirement for 'any' not satisfied", getLogPrefix());
                ActionSupport.buildEvent(profileRequestContext, SAMLEventIds.SUBJECT_ID_REQ_FAILED);
            }
        } else {
            log.warn("{} Metadata indicates invalid subject-id requirement value '{}', ignoring", getLogPrefix(),
                    requirement);
        }
    }
    
// Checkstyle: CyclomaticComplexity OFF
    /**
     * Search for relevant extension in metadata and determine requirement.
     * 
     * @param entity input to search
     * 
     * @return the relevant requirement, if any
     */
    @Nullable private String extractRequirement(@Nonnull final EntityDescriptor entity) {
        final Extensions exts = entity.getExtensions();
        if (exts == null) {
            return null;
        }
        
        final List<XMLObject> mdattrs = exts.getUnknownXMLObjects(EntityAttributes.DEFAULT_ELEMENT_NAME);
        if (mdattrs == null || mdattrs.isEmpty()) {
            return null;
        }
        
        for (final Attribute mdattr : ((EntityAttributes) mdattrs.get(0)).getAttributes()) {
            if (!REQUIREMENT_TAG_NAME.equals(mdattr.getName())
                    || !Attribute.URI_REFERENCE.equals(mdattr.getNameFormat())) {
                continue;
            }
            
            final List<XMLObject> vals = mdattr.getAttributeValues();
            if (vals == null || vals.isEmpty()) {
                continue;
            }
            
            final XMLObject val = vals.get(0);
            if (val instanceof XSString) {
                return ((XSString) val).getValue();
            } else if (val instanceof XSAny) {
                if (((XSAny) val).getUnknownAttributes().isEmpty()
                        && ((XSAny) val).getUnknownXMLObjects().isEmpty()) {
                    return ((XSAny) val).getTextContent();
                }
            }
        }
        
        return null;
    }
    
    /**
     * Find and store off the identifiers if they exist.
     * 
     * @param profileRequestContext profile request context
     */
    private void findAttributes(@Nonnull final ProfileRequestContext profileRequestContext) {
        for (final Assertion assertion : assertionsLookupStrategy.apply(profileRequestContext)) {
            for (final AttributeStatement statement : assertion.getAttributeStatements()) {
                for (final Attribute attribute : statement.getAttributes()) {
                    if (subjectIDAttribute == null) {
                        if (SUBJECT_ID_ATTR_NAME.equals(attribute.getName())) {
                            if (Attribute.URI_REFERENCE.equals(attribute.getNameFormat())
                                    && !attribute.getAttributeValues().isEmpty()) {
                                subjectIDAttribute = attribute;
                                if (pairwiseIDAttribute != null) {
                                    return;
                                } else {
                                    continue;
                                }
                            }
                        }
                    }
                    
                    if (pairwiseIDAttribute == null) {
                        if (PAIRWISE_ID_ATTR_NAME.equals(attribute.getName())) {
                            if (Attribute.URI_REFERENCE.equals(attribute.getNameFormat())
                                    && !attribute.getAttributeValues().isEmpty()) {
                                pairwiseIDAttribute = attribute;
                                if (subjectIDAttribute != null) {
                                    return;
                                } else {
                                    continue;
                                }
                            }
                        }
                    }
                }
            }
        }
    }
// Checkstyle: CyclomaticComplexity ON
    
    /**
     * Default strategy for obtaining assertions to modify.
     * 
     * <p>If the outbound context is empty, an empty list is returned. If the outbound
     * message is already an assertion, it's returned. If the outbound message is a response,
     * then its contents are returned. If the outbound message is anything else, an empty list
     * is returned.</p>
     */
    private class AssertionStrategy implements Function<ProfileRequestContext,List<Assertion>> {

        /** {@inheritDoc} */
        @Override
        @Nullable public List<Assertion> apply(@Nullable final ProfileRequestContext input) {
            if (input != null && input.getOutboundMessageContext() != null) {
                final Object outboundMessage = input.getOutboundMessageContext().getMessage();
                if (outboundMessage instanceof Assertion) {
                    return Collections.singletonList((Assertion) outboundMessage);
                } else if (outboundMessage instanceof Response) {
                    return ((Response) outboundMessage).getAssertions();
                }
            }
            
            return Collections.emptyList();
        }
    }
    
}