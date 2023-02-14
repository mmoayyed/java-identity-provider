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

package net.shibboleth.idp.saml.nameid.impl;

import java.util.List;
import java.util.Map;
import java.util.function.Function;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.opensaml.messaging.context.navigate.ChildContextLookup;
import org.opensaml.profile.context.ProfileRequestContext;
import org.opensaml.saml.common.SAMLException;
import org.opensaml.saml.saml2.core.NameID;
import org.opensaml.saml.saml2.profile.AbstractSAML2NameIDGenerator;
import org.opensaml.saml.saml2.profile.SAML2ObjectSupport;
import org.slf4j.Logger;

import net.shibboleth.idp.attribute.EmptyAttributeValue;
import net.shibboleth.idp.attribute.IdPAttribute;
import net.shibboleth.idp.attribute.IdPAttributeValue;
import net.shibboleth.idp.attribute.ScopedStringAttributeValue;
import net.shibboleth.idp.attribute.StringAttributeValue;
import net.shibboleth.idp.attribute.XMLObjectAttributeValue;
import net.shibboleth.idp.attribute.context.AttributeContext;
import net.shibboleth.idp.profile.context.navigate.RelyingPartyIdLookupFunction;
import net.shibboleth.idp.profile.context.navigate.ResponderIdLookupFunction;
import net.shibboleth.profile.context.RelyingPartyContext;
import net.shibboleth.shared.annotation.constraint.NonnullElements;
import net.shibboleth.shared.annotation.constraint.ThreadSafeAfterInit;
import net.shibboleth.shared.collection.CollectionSupport;
import net.shibboleth.shared.component.ComponentInitializationException;
import net.shibboleth.shared.logic.Constraint;
import net.shibboleth.shared.primitive.LoggerFactory;
import net.shibboleth.shared.primitive.StringSupport;

/**
 * Generator for {@link NameID} objects based on {@link IdPAttribute} data.
 */
@ThreadSafeAfterInit
public class AttributeSourcedSAML2NameIDGenerator extends AbstractSAML2NameIDGenerator {

    /** Class logger. */
    @Nonnull private final Logger log = LoggerFactory.getLogger(AttributeSourcedSAML2NameIDGenerator.class);

    /** Strategy function to lookup AttributeContext. */
    @Nonnull private Function<ProfileRequestContext, AttributeContext> attributeContextLookupStrategy;

    /** Whether to look at filtered or unfiltered attributes. */
    private boolean useUnfilteredAttributes;

    /** Delimiter to use for scoped attribute serialization. */
    private char delimiter;

    /** Attribute(s) to use as an identifier source. */
    @Nonnull @NonnullElements private List<String> attributeSourceIds;

    /** Constructor. */
    public AttributeSourcedSAML2NameIDGenerator() {
        attributeContextLookupStrategy =
                new ChildContextLookup<>(AttributeContext.class).compose(
                        new ChildContextLookup<>(RelyingPartyContext.class));
        delimiter = '@';
        attributeSourceIds = CollectionSupport.emptyList();
        setDefaultIdPNameQualifierLookupStrategy(new ResponderIdLookupFunction());
        setDefaultSPNameQualifierLookupStrategy(new RelyingPartyIdLookupFunction());
        useUnfilteredAttributes = false;
    }

    /**
     * Set the lookup strategy to use to locate the {@link AttributeContext}.
     * 
     * @param strategy lookup function to use
     */
    public void setAttributeContextLookupStrategy(
            @Nonnull final Function<ProfileRequestContext, AttributeContext> strategy) {
        checkSetterPreconditions();
        attributeContextLookupStrategy = Constraint.isNotNull(strategy,
            "AttributeContext lookup strategy cannot be null");
    }

    /**
     * Set the delimiter to use for serializing scoped attribute values.
     * 
     * @param ch scope to set
     */
    public void setScopedDelimiter(final char ch) {
        checkSetterPreconditions();
        delimiter = ch;
    }

    /**
     * Set the attribute sources to pull from.
     * 
     * @param ids attribute IDs to pull from
     */
    public void setAttributeSourceIds(@Nonnull @NonnullElements final List<String> ids) {
        checkSetterPreconditions();
        attributeSourceIds = CollectionSupport.copyToList(Constraint.isNotNull(ids, "Attribute ID collection cannot be null"));
    }

    /**
     * Set whether to source the input attributes from the unfiltered set.
     * 
     * @param flag flag to set
     */
    public void setUseUnfilteredAttributes(final boolean flag) {
        useUnfilteredAttributes = flag;
    }

    /** {@inheritDoc} */
    @Override protected void doInitialize() throws ComponentInitializationException {
        super.doInitialize();

        if (attributeSourceIds.isEmpty()) {
            throw new ComponentInitializationException("Attribute source ID list cannot be empty");
        }
    }

    /** {@inheritDoc} */
    @Override @Nullable protected NameID doGenerate(@Nonnull final ProfileRequestContext profileRequestContext)
            throws SAMLException {

        // Check for a natively generated NameIdentifier attribute value.

        final AttributeContext attributeCtx = attributeContextLookupStrategy.apply(profileRequestContext);
        if (attributeCtx == null) {
            log.warn("Unable to locate AttributeContext");
            return null;
        }

        final Map<String,IdPAttribute> attributes = useUnfilteredAttributes ? attributeCtx.getUnfilteredIdPAttributes()
                : attributeCtx.getIdPAttributes();

        for (final String sourceId : attributeSourceIds) {

            final IdPAttribute attribute = attributes.get(sourceId);
            if (attribute == null) {
                continue;
            }

            final List<IdPAttributeValue> values = attribute.getValues();
            for (final IdPAttributeValue value : values) {
                if (value instanceof XMLObjectAttributeValue &&
                        ((XMLObjectAttributeValue) value).getValue() instanceof NameID) {
                    if (SAML2ObjectSupport.areNameIDFormatsEquivalent(getFormat(),
                            ((NameID) ((XMLObjectAttributeValue) value).getValue()).getFormat())) {
                        log.info("Returning NameID from XMLObject-valued attribute {}", sourceId);
                        return (NameID) ((XMLObjectAttributeValue) value).getValue();
                    }
                    log.debug("Attribute {} value was NameID, but Format did not match", sourceId);
                }
            }
        }

        // Fall into base class version which will ask us for an identifier.

        return super.doGenerate(profileRequestContext);
    }

    /** {@inheritDoc} */
    @Override @Nullable protected String getIdentifier(@Nonnull final ProfileRequestContext profileRequestContext)
            throws SAMLException {

        final AttributeContext attributeCtx = attributeContextLookupStrategy.apply(profileRequestContext);

        final Map<String,IdPAttribute> attributes = useUnfilteredAttributes ? attributeCtx.getUnfilteredIdPAttributes()
                : attributeCtx.getIdPAttributes();
        
        for (final String sourceId : attributeSourceIds) {
            log.debug("Checking for source attribute {}", sourceId);

            final IdPAttribute attribute = attributes.get(sourceId);
            if (attribute == null) {
                continue;
            }

            final List<IdPAttributeValue> values = attribute.getValues();
            for (final IdPAttributeValue value : values) {
                if (value instanceof ScopedStringAttributeValue) {
                    log.debug("Generating NameID from Scoped String-valued attribute {}", sourceId);
                    return ((ScopedStringAttributeValue) value).getValue() + delimiter
                            + ((ScopedStringAttributeValue) value).getScope();
                } else if (value instanceof StringAttributeValue) {
                    final String strVal = StringSupport.trimOrNull(((StringAttributeValue) value).getValue());
                    if (strVal == null) {
                        log.debug("Skipping all-whitespace string value");
                        continue;
                    }
                    log.debug("Generating NameID from String-valued attribute {}", sourceId);
                    return strVal;
                } else if (value instanceof EmptyAttributeValue) {
                    log.debug("Skipping empty value");
                    continue;
                } else {
                    log.warn("Unrecognized attribute value type: {}", value.getClass().getName());
                }
            }
        }

        log.info("Attribute sources {} did not produce a usable identifier", attributeSourceIds);
        return null;
    }

}
