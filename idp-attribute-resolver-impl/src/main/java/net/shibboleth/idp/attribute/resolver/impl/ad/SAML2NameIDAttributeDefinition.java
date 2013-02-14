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
import net.shibboleth.utilities.java.support.component.ComponentInitializationException;
import net.shibboleth.utilities.java.support.component.ComponentSupport;
import net.shibboleth.utilities.java.support.primitive.StringSupport;

import org.opensaml.core.xml.config.XMLObjectProviderRegistrySupport;
import org.opensaml.saml.common.SAMLObjectBuilder;
import org.opensaml.saml.saml2.core.NameID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Function;
import com.google.common.base.Optional;

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

    /** Strategy used to locate the RelyingParty EntityId given a {@link AttributeResolutionContext}. */
    // TODO(rdw) These needs to be changed when the profile handling has been finalized
    // TODO Do we mean IdP or RelyingParty or what? Fix when [...]
    // the questions in https://wiki.shibboleth.net/confluence/display/OS30/Messaging+Abstractions+Discussion+Document
    // are answered
    // TODO should this be a org.opensaml.messaging.context.navigate.ContextDataLookupFunction ?
    private Function<AttributeResolutionContext, String> spEntityIdStrategy;

    /** Strategy used to locate the IdP EntityId given a {@link AttributeResolutionContext}. */
    // TODO(rdw) These needs to be changed when the profile handling has been finalized
    // TODO Do we mean IdP or RelyingParty or what? Fix when [...]
    // the questions in https://wiki.shibboleth.net/confluence/display/OS30/Messaging+Abstractions+Discussion+Document
    // are answered
    // TODO should this be a org.opensaml.messaging.context.navigate.ContextDataLookupFunction ?
    private Function<AttributeResolutionContext, String> idPEntityIdStrategy;

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
     * Gets the SPNameQualifier for the NameID used as an attribute value.
     * 
     * @return SPNameQualifier for the NameID used as an attribute value
     */
    public String getNameIdSPQualifier() {
        return nameIdSPQualifier;
    }

    /**
     * Sets the SPNameQualifier for the NameID used as an attribute value.
     * 
     * @param qualifier SPNameQualifier for the NameID used as an attribute value
     */
    public void setNameIdSPQualifier(String qualifier) {
        ComponentSupport.ifInitializedThrowUnmodifiabledComponentException(this);
        nameIdSPQualifier = qualifier;
    }

    /**
     * Gets the strategy for finding the IdP EntityId from the resolution context.
     * 
     * @return the required strategy.
     */
    public Function<AttributeResolutionContext, String> getIdPEntityIdStrategy() {
        return idPEntityIdStrategy;
    }

    /**
     * Sets the strategy for finding the IdP EntityId from the resolution context.
     * 
     * @param strategy what to set
     */
    public void setIdPEntityIdStrategy(Function<AttributeResolutionContext, String> strategy) {
        ComponentSupport.ifInitializedThrowUnmodifiabledComponentException(this);
        idPEntityIdStrategy = strategy;
    }

    /**
     * Gets the strategy for finding the RelyingParty EntityId from the resolution context.
     * 
     * @return the required strategy.
     */
    public Function<AttributeResolutionContext, String> getSPEntityIdStrategy() {
        return spEntityIdStrategy;
    }

    /**
     * Sets the strategy for finding the RelyingPartyContext from the resolution context.
     * 
     * @param strategy to set.
     */
    public void setSPEntityIdStrategy(Function<AttributeResolutionContext, String> strategy) {
        ComponentSupport.ifInitializedThrowUnmodifiabledComponentException(this);
        spEntityIdStrategy = strategy;
    }

    /** {@inheritDoc} */
    protected void doInitialize() throws ComponentInitializationException {
        super.doInitialize();

        if (null == spEntityIdStrategy) {
            throw new ComponentInitializationException("Attribute definition '" + getId()
                    + "': no Relying Party EntityId Lookup Strategy set");
        }

        if (null == idPEntityIdStrategy) {
            throw new ComponentInitializationException("Attribute definition '" + getId()
                    + "': no IdP EntityId Lookup Strategy set");
        }
    }

    /**
     * Builds a name ID. The provided value is the textual content of the NameID. The NameQualifier and SPNameQualifier
     * are set according to the configuration, or to the local and requesting entityIDs respectively.
     * 
     * @param nameIdValue value of the NameID
     * @param resolutionContext current resolution context
     * 
     * @return the constructed NameID
     * @throws AttributeResolutionException if the IdP Name is empty.
     */
    protected NameID buildNameId(@Nonnull String nameIdValue, @Nonnull AttributeResolutionContext resolutionContext)
            throws AttributeResolutionException {

        log.debug("NameIdAttribute {} : Building a SAML2 NameID with value for {}", getId(), nameIdValue);

        final String spEntityId =
                StringSupport.trimOrNull(spEntityIdStrategy.apply(resolutionContext));
        final String idpEntityId = StringSupport.trimOrNull(idPEntityIdStrategy.apply(resolutionContext));

        NameID nameId = nameIDBuilder.buildObject();
        nameId.setValue(nameIdValue);

        if (nameIdFormat != null) {
            nameId.setFormat(nameIdFormat);
        }

        if (nameIdQualifier != null) {
            nameId.setNameQualifier(nameIdQualifier);
        } else if (null != idpEntityId) {
            nameId.setNameQualifier(idpEntityId);
        } else {
            throw new AttributeResolutionException("Attribute definition '" + getId()
                    + " provided IdP EntityId was empty");
        }

        if (nameIdSPQualifier != null) {
            nameId.setSPNameQualifier(nameIdSPQualifier);
        } else if (null != spEntityId) {
            nameId.setSPNameQualifier(spEntityId);
        } else {
            throw new AttributeResolutionException("Attribute definition '" + getId()
                    + " provided SP EntityId was empty");
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
     * @throws AttributeResolutionException if the IdP Name is empty.
     */
    @Nullable private XMLObjectAttributeValue encodeOneValue(@Nonnull AttributeValue theValue,
            @Nonnull AttributeResolutionContext resolutionContext) throws AttributeResolutionException {
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
