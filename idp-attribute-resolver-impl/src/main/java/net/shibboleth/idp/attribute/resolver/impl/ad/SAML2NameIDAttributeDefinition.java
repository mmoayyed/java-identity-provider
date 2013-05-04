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
import net.shibboleth.idp.attribute.resolver.AttributeRecipientContext;
import net.shibboleth.idp.attribute.resolver.AttributeResolutionContext;
import net.shibboleth.idp.attribute.resolver.BaseAttributeDefinition;
import net.shibboleth.idp.attribute.resolver.PluginDependencySupport;
import net.shibboleth.idp.attribute.resolver.ResolutionException;
import net.shibboleth.utilities.java.support.component.ComponentSupport;
import net.shibboleth.utilities.java.support.primitive.StringSupport;

import org.opensaml.core.xml.config.XMLObjectProviderRegistrySupport;
import org.opensaml.saml.common.SAMLObjectBuilder;
import org.opensaml.saml.saml2.core.NameID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * An attribute definition that creates attributes whose values are {@link NameID}.
 * 
 * When building the NameID the textual content of the NameID is the value of the source attribute. If a
 * {@link #nameIdQualifier} is provided that value is used as the NameID's name qualifier otherwise the attribute
 * issuer's entity ID is used. The attribute requester's entity ID is always used as the NameID's SP name qualifier.
 */

public class SAML2NameIDAttributeDefinition extends BaseAttributeDefinition {

    /** Class logger. */
    private final Logger log = LoggerFactory.getLogger(SAML2NameIDAttributeDefinition.class);

    /** The builder for the object represented inside this attribute. */
    private final SAMLObjectBuilder<NameID> nameIDBuilder;

    /** Format of the NameID. */
    private String nameIdFormat;

    /** Name qualifier for the NameID. */
    private String nameIdQualifier;

    /** SP name qualifier for the NameID. */
    private String nameIdSPQualifier;

    /**
     * Constructor.
     */
    public SAML2NameIDAttributeDefinition() {
        nameIDBuilder =
                (SAMLObjectBuilder<NameID>) XMLObjectProviderRegistrySupport.getBuilderFactory().getBuilder(
                        NameID.DEFAULT_ELEMENT_NAME);
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
    public void setNameIdFormat(@Nullable String format) {
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
     * Gets the SPNameQualifier for the NameID used as an attribute value.
     * 
     * @return SPNameQualifier for the NameID used as an attribute value
     */
    @Nullable public String getNameIdSPQualifier() {
        return nameIdSPQualifier;
    }

    /**
     * Sets the SPNameQualifier for the NameID used as an attribute value.
     * 
     * @param qualifier SPNameQualifier for the NameID used as an attribute value
     */
    public void setNameIdSPQualifier(@Nullable String qualifier) {
        ComponentSupport.ifInitializedThrowUnmodifiabledComponentException(this);
        nameIdSPQualifier = qualifier;
    }

    /**
     * Builds a name ID. The provided value is the textual content of the NameID. The NameQualifier and SPNameQualifier
     * are set according to the configuration, or to the local and requesting entityIDs respectively.
     * 
     * @param nameIdValue value of the NameID
     * @param resolutionContext current resolution context
     * 
     * @return the constructed NameID
     * @throws ResolutionException if the IdP Name is empty.
     */
    protected NameID buildNameId(@Nonnull String nameIdValue, @Nonnull AttributeResolutionContext resolutionContext)
            throws ResolutionException {

        log.debug("NameIdAttribute {} : Building a SAML2 NameID with value for {}", getId(), nameIdValue);

        final AttributeRecipientContext attributeRecipientContext =
                resolutionContext.getSubcontext(AttributeRecipientContext.class);

        if (null == attributeRecipientContext) {
            throw new ResolutionException("Attribute definition '" + getId()
                    + " no attribute recipient context provided ");
        }

        final String attributeRecipientID =
                StringSupport.trimOrNull(attributeRecipientContext.getAttributeRecipientID());

        final String attributeIssuerID = StringSupport.trimOrNull(attributeRecipientContext.getAttributeIssuerID());

        NameID nameId = nameIDBuilder.buildObject();
        nameId.setValue(nameIdValue);

        if (nameIdFormat != null) {
            nameId.setFormat(nameIdFormat);
        }

        if (nameIdQualifier != null) {
            nameId.setNameQualifier(nameIdQualifier);
        } else if (null != attributeIssuerID) {
            nameId.setNameQualifier(attributeIssuerID);
        } else {
            throw new ResolutionException("Attribute definition '" + getId()
                    + " provided attribute issuer ID  was empty");
        }

        if (nameIdSPQualifier != null) {
            nameId.setSPNameQualifier(nameIdSPQualifier);
        } else if (null != attributeRecipientID) {
            nameId.setSPNameQualifier(attributeRecipientID);
        } else {
            throw new ResolutionException("Attribute definition '" + getId()
                    + " provided attribute recipient ID was empty");
        }

        return nameId;
    }

    /**
     * Worker function for doAttributeDefintionResolve. This returns an AttributeValue if the input value is appropriate
     * for encoding as a NameID.
     * 
     * @param theValue an arbitrary value.
     * @param resolutionContext the context to get the rest of the values from
     * @return null or an attributeValue.
     * @throws ResolutionException if the IdP Name is empty.
     */
    @Nullable private XMLObjectAttributeValue encodeOneValue(@Nonnull AttributeValue theValue,
            @Nonnull AttributeResolutionContext resolutionContext) throws ResolutionException {
        if (theValue instanceof StringAttributeValue) {
            StringAttributeValue value = (StringAttributeValue) theValue;
            NameID nid = buildNameId(value.getValue(), resolutionContext);
            XMLObjectAttributeValue val = new XMLObjectAttributeValue(nid);
            return val;
        }
        log.warn("NameIdAttribute {} : Value {} is not a string", getId(), theValue.toString());
        return null;
    }

    /** {@inheritDoc} */
    @Nullable protected Attribute doAttributeDefinitionResolve(@Nonnull AttributeResolutionContext resolutionContext)
            throws ResolutionException {

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
                    log.warn("NameIdAttribute {} No appropriate values", getId());
                    return null;
                }
            }
        }
        result.setValues(outputValues);

        return result;

    }

}
