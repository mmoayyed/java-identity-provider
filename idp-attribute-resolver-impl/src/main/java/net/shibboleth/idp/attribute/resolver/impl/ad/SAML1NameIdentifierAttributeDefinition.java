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

package net.shibboleth.idp.attribute.resolver.impl.ad;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import net.shibboleth.idp.attribute.IdPAttribute;
import net.shibboleth.idp.attribute.AttributeValue;
import net.shibboleth.idp.attribute.StringAttributeValue;
import net.shibboleth.idp.attribute.XMLObjectAttributeValue;
import net.shibboleth.idp.attribute.resolver.AttributeRecipientContext;
import net.shibboleth.idp.attribute.resolver.AttributeResolutionContext;
import net.shibboleth.idp.attribute.resolver.ResolutionException;
import net.shibboleth.idp.attribute.resolver.BaseAttributeDefinition;
import net.shibboleth.idp.attribute.resolver.PluginDependencySupport;
import net.shibboleth.utilities.java.support.component.ComponentSupport;
import net.shibboleth.utilities.java.support.primitive.StringSupport;

import org.opensaml.core.xml.config.XMLObjectProviderRegistrySupport;
import org.opensaml.saml.common.SAMLObjectBuilder;
import org.opensaml.saml.saml1.core.NameIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * An attribute definition the creates attributes whose values are {@link NameIdentifier}.
 * <br/>
 * When building the NameIdentifier the textual content of the NameIdentifier is the value of the source attribute. If a
 * {@link #nameIdQualifier} is provided that value is used as the NameIdentifier's name qualifier otherwise the
 * attribute issuer's entity ID is used. 
 */

public class SAML1NameIdentifierAttributeDefinition extends BaseAttributeDefinition {

    /** Class logger. */
    private final Logger log = LoggerFactory.getLogger(SAML1NameIdentifierAttributeDefinition.class);

    /** The builder for the object represented inside this attribute. */
    private final SAMLObjectBuilder<NameIdentifier> nameIdentifierBuilder;

    /** Format of the NameID. */
    private String nameIdFormat;

    /** Name qualifier for the NameID. */
    private String nameIdQualifier;

    /**
     * Constructor.
     */
    public SAML1NameIdentifierAttributeDefinition() {
        nameIdentifierBuilder =
                (SAMLObjectBuilder<NameIdentifier>) XMLObjectProviderRegistrySupport.getBuilderFactory().getBuilder(
                        NameIdentifier.DEFAULT_ELEMENT_NAME);
    }

    /**
     * Gets the format for the NameID used as an attribute value.
     * 
     * @return format for the NameID used as an attribute value
     */
    @Nullable public String getNameIdFormat() {
        return nameIdFormat;
    }

    /**
     * Sets the format for the NameID used as an attribute value.
     * 
     * @param format format for the NameID used as an attribute value
     */
    @Nullable public void setNameIdFormat(@Nullable String format) {
        ComponentSupport.ifInitializedThrowUnmodifiabledComponentException(this);
        nameIdFormat = format;
    }

    /**
     * Gets the NameQualifier for the NameID used as an attribute value.
     * 
     * @return NameQualifier for the NameID used as an attribute value
     */
    @Nullable public String getNameIdQualifier() {
        return nameIdQualifier;
    }

    /**
     * Sets the NameQualifier for the NameID used as an attribute value.
     * 
     * @param qualifier NameQualifier for the NameID used as an attribute value
     */
    public void setNameIdQualifier(@Nullable String qualifier) {
        ComponentSupport.ifInitializedThrowUnmodifiabledComponentException(this);
        nameIdQualifier = qualifier;
    }

    /**
     * Builds a name ID. The provided value is the textual content of the NameIdentifier. If a {@link #nameIdQualifier}
     * is not null it is used as the NameIdentifier's name qualifier, otherwise the attribute issuer's entity id is
     * used.
     * 
     * @param nameIdValue value of the NameIdentifier
     * @param resolutionContext current resolution context
     * 
     * @return the constructed NameIdentifier
     * @throws ResolutionException if the IdP Name is empty.
     */
    protected NameIdentifier buildNameId(@Nonnull String nameIdValue,
            @Nonnull AttributeResolutionContext resolutionContext) throws ResolutionException {

        log.debug("{} building a SAML1 NameIdentifier with value of '{}'", getLogPrefix(), nameIdValue);

        final AttributeRecipientContext attributeRecipientContext =
                resolutionContext.getSubcontext(AttributeRecipientContext.class);

        if (null == attributeRecipientContext) {
            throw new ResolutionException(getLogPrefix() + " no attribute recipient context provided ");
        }

        NameIdentifier nameIdentifier = nameIdentifierBuilder.buildObject();
        nameIdentifier.setNameIdentifier(nameIdValue);

        if (nameIdFormat != null) {
            log.debug("{} Format set to '{}'", getLogPrefix(), nameIdFormat);
            nameIdentifier.setFormat(nameIdFormat);
        }
        final String attributeIssuerID = StringSupport.trimOrNull(attributeRecipientContext.getAttributeIssuerID());

        if (nameIdQualifier != null) {
            nameIdentifier.setNameQualifier(nameIdQualifier);
            log.debug("{} NameQualifier set to '{}'", getLogPrefix(), nameIdQualifier);
        } else if (null != attributeIssuerID) {
            log.debug("{} NameQualifier set to '{}'", getLogPrefix(), attributeIssuerID);
            nameIdentifier.setNameQualifier(attributeIssuerID);
        } else {
            throw new ResolutionException(getLogPrefix() + " provided attribute issuer ID was empty");
        }

        return nameIdentifier;
    }

    /**
     * Worker function for doAttributeDefintionResolve. This returns an AttributeValue if the input value is appropriate
     * for encoding as a NameID.
     * 
     * @param theValue an arbitrary value.
     * @param resolutionContext the context to get the rest of the values from
     * @return null or an attributeValue;
     * @throws ResolutionException if the IdP Name is empty.
     */
    @Nullable private XMLObjectAttributeValue encodeOneValue(@Nonnull AttributeValue<?> theValue,
            @Nonnull AttributeResolutionContext resolutionContext) throws ResolutionException {

        if (theValue instanceof StringAttributeValue) {
            StringAttributeValue value = (StringAttributeValue) theValue;
            NameIdentifier nid = buildNameId(value.getValue(), resolutionContext);
            XMLObjectAttributeValue val = new XMLObjectAttributeValue(nid);
            return val;
        }
        log.warn("{} Value {} is not a string", getLogPrefix(), theValue.toString());
        return null;
    }

    /** {@inheritDoc} */
    @Nullable protected IdPAttribute doAttributeDefinitionResolve(@Nonnull AttributeResolutionContext resolutionContext)
            throws ResolutionException {

        ComponentSupport.ifNotInitializedThrowUninitializedComponentException(this);
        ComponentSupport.ifDestroyedThrowDestroyedComponentException(this);
        Set<? extends AttributeValue<?>> inputValues;
        Set<? extends AttributeValue<?>> outputValues = null;
        final IdPAttribute result = new IdPAttribute(getId());

        inputValues = PluginDependencySupport.getMergedAttributeValues(resolutionContext, getDependencies());

        if (null != inputValues && !inputValues.isEmpty()) {

            if (1 == inputValues.size()) {
                AttributeValue<?> val = encodeOneValue(inputValues.iterator().next(), resolutionContext);
                if (null != val) {
                    outputValues = Collections.singleton(val);
                }
            } else {
                // TODO(rdw) Fix typing
                // Intermediate to solve typing issues.
                final HashSet<XMLObjectAttributeValue> xmlVals = new HashSet<>(inputValues.size());
                for (AttributeValue<?> theValue : inputValues) {
                    final XMLObjectAttributeValue val = encodeOneValue(theValue, resolutionContext);
                    if (null != val) {
                        xmlVals.add(val);
                    }
                }
                if (0 == xmlVals.size()) {
                    log.warn("{} No appropriate values", getLogPrefix());
                    return null;
                }
                outputValues = xmlVals;
            }
        }
        result.setValues(outputValues);

        return result;
    }

}
