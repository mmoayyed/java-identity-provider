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

import net.shibboleth.idp.attribute.Attribute;
import net.shibboleth.idp.attribute.AttributeValue;
import net.shibboleth.idp.attribute.StringAttributeValue;
import net.shibboleth.idp.attribute.XMLObjectAttributeValue;
import net.shibboleth.idp.attribute.resolver.AttributeResolutionContext;
import net.shibboleth.idp.attribute.resolver.AttributeResolutionException;
import net.shibboleth.idp.attribute.resolver.BaseAttributeDefinition;
import net.shibboleth.idp.attribute.resolver.PluginDependencySupport;
import net.shibboleth.utilities.java.support.component.ComponentSupport;
import net.shibboleth.utilities.java.support.primitive.StringSupport;

import org.opensaml.core.xml.config.XMLObjectProviderRegistrySupport;
import org.opensaml.saml.common.SAMLObjectBuilder;
import org.opensaml.saml.saml1.core.NameIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Optional;

/**
 * An attribute definition the creates attributes whose values are {@link NameIdentifier}.
 * 
 * When building the NameIdentifier the textual content of the NameIdentifier is the value of the source attribute. If a
 * {@link #nameIdQualifier} is provided that value is used as the NameIdentifier's name qualifier otherwise the
 * attribute issuer's entity ID is used. The attribute requester's entity ID is always used as the NameIdentifier's SP
 * name qualifier.
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
    public String getNameIdFormat() {
        return nameIdFormat;
    }

    /**
     * Sets the format for the NameID used as an attribute value.
     * 
     * @param format format for the NameID used as an attribute value
     */
    public void setNameIdFormat(String format) {
        ComponentSupport.ifInitializedThrowUnmodifiabledComponentException(this);
        nameIdFormat = format;
    }

    /**
     * Gets the NameQualifier for the NameID used as an attribute value.
     * 
     * @return NameQualifier for the NameID used as an attribute value
     */
    public String getNameIdQualifier() {
        return nameIdQualifier;
    }

    /**
     * Sets the NameQualifier for the NameID used as an attribute value.
     * 
     * @param qualifier NameQualifier for the NameID used as an attribute value
     */
    public void setNameIdQualifier(String qualifier) {
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
     * @throws AttributeResolutionException if the IdP Name is empty.
     */
    protected NameIdentifier buildNameId(@Nonnull String nameIdValue,
            @Nonnull AttributeResolutionContext resolutionContext) throws AttributeResolutionException {

        log.debug("NameIdAttribute {} : Building a SAML1 NameIdentifier with value for {}", getId(), nameIdValue);

        final String attributeIssuerID = StringSupport.trimOrNull(resolutionContext.getAttributeIssuerID());

        NameIdentifier nameIdentifier = nameIdentifierBuilder.buildObject();
        nameIdentifier.setNameIdentifier(nameIdValue);

        if (nameIdFormat != null) {
            nameIdentifier.setFormat(nameIdFormat);
        }

        if (nameIdQualifier != null) {
            nameIdentifier.setNameQualifier(nameIdQualifier);
        } else if (null != attributeIssuerID) {
            nameIdentifier.setNameQualifier(attributeIssuerID);
        } else {
            throw new AttributeResolutionException("Attribute definition '" + getId()
                    + " provided attribute issuer ID was empty");
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
     * @throws AttributeResolutionException if the IdP Name is empty.
     */
    @Nullable private XMLObjectAttributeValue encodeOneValue(@Nonnull AttributeValue theValue,
            @Nonnull AttributeResolutionContext resolutionContext) throws AttributeResolutionException {

        if (theValue instanceof StringAttributeValue) {
            StringAttributeValue value = (StringAttributeValue) theValue;
            NameIdentifier nid = buildNameId(value.getValue(), resolutionContext);
            XMLObjectAttributeValue val = new XMLObjectAttributeValue(nid);
            return val;
        }
        log.warn("NameIdAttribute {} : Value {} is not a string", getId(), theValue.toString());
        return null;
    }

    /** {@inheritDoc} */
    @Nonnull protected Optional<Attribute> doAttributeDefinitionResolve(
            @Nonnull AttributeResolutionContext resolutionContext) throws AttributeResolutionException {

        ComponentSupport.ifNotInitializedThrowUninitializedComponentException(this);
        ComponentSupport.ifDestroyedThrowDestroyedComponentException(this);
        Set<AttributeValue> inputValues;
        Set<AttributeValue> outputValues = null;
        final Attribute result = new Attribute(getId());

        inputValues = PluginDependencySupport.getMergedAttributeValues(resolutionContext, getDependencies());

        if (null != inputValues && !inputValues.isEmpty()) {

            if (1 == inputValues.size()) {
                AttributeValue val = encodeOneValue(inputValues.iterator().next(), resolutionContext);
                if (null != val) {
                    outputValues = Collections.singleton(val);
                }
            } else {
                outputValues = new HashSet<AttributeValue>(inputValues.size());
                for (AttributeValue theValue : inputValues) {
                    AttributeValue val = encodeOneValue(theValue, resolutionContext);
                    if (null != val) {
                        outputValues.add(val);
                    }
                }
                if (0 == outputValues.size()) {
                    outputValues = null;
                }
            }
        }
        result.setValues(outputValues);

        return Optional.fromNullable(result);
    }

}
