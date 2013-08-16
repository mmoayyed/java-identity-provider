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

package net.shibboleth.idp.attribute.mapper;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import net.shibboleth.idp.attribute.Attribute;
import net.shibboleth.idp.attribute.AttributeValue;
import net.shibboleth.utilities.java.support.annotation.constraint.NonnullAfterInit;
import net.shibboleth.utilities.java.support.annotation.constraint.NonnullElements;
import net.shibboleth.utilities.java.support.annotation.constraint.NotEmpty;
import net.shibboleth.utilities.java.support.annotation.constraint.NullableElements;
import net.shibboleth.utilities.java.support.annotation.constraint.Unmodifiable;
import net.shibboleth.utilities.java.support.component.AbstractIdentifiableInitializableComponent;
import net.shibboleth.utilities.java.support.component.ComponentInitializationException;
import net.shibboleth.utilities.java.support.component.ComponentSupport;
import net.shibboleth.utilities.java.support.primitive.StringSupport;

import org.opensaml.core.xml.XMLObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Predicates;
import com.google.common.collect.Collections2;
import com.google.common.collect.ImmutableList;

/**
 * Basis of all classes which map SAML2 {@link org.opensaml.saml.saml2.core.Attribute} into an IdP {@link Attribute}.
 * 
 * @param <InType> the input (SAML2 attribute) type
 * @param <OutType> the output (IdP Attribute) type
 */
public abstract class AbstractSAMLAttributeMapper<InType extends org.opensaml.saml.saml2.core.Attribute, 
                                                  OutType extends Attribute>
        extends AbstractIdentifiableInitializableComponent implements AttributeMapper<InType, OutType> {

    /** log. */
    private final Logger log = LoggerFactory.getLogger(AbstractSAMLAttributeMapper.class);

    /** The internal names to generate. */
    private List<String> attributeIds = Collections.EMPTY_LIST;

    /** The attribute format. */
    private String attributeFormat;

    /** the (SAML) attribute name. */
    private String theSAMLName;

    /** The String used to prefix log message. */
    private String logPrefix;

    /** The value mapper. */
    private AbstractSAMLAttributeValueMapper valueMapper;

    /**
     * Sets the list of internal identifiers.
     * 
     * @param theIds the list
     */
    public void setAttributeIds(@Nullable @NullableElements final List<String> theIds) {
        ComponentSupport.ifInitializedThrowUnmodifiabledComponentException(this);
        if (null == theIds) {
            return;
        }

        attributeIds = ImmutableList.copyOf(Collections2.filter(theIds, Predicates.notNull()));
    }

    /**
     * Gets the list of internal identifiers.
     * 
     * @return the identifiers
     */
    @Nonnull @NonnullElements @Unmodifiable public List<String> getAttributeIds() {
        return attributeIds;
    }

    /**
     * Sets the SAML attribute name.
     * 
     * @param name the name
     */
    @Nullable public void setSAMLName(final String name) {
        ComponentSupport.ifInitializedThrowUnmodifiabledComponentException(this);
        theSAMLName = StringSupport.trimOrNull(name);
    }

    /**
     * Gets the SAML attribute name.
     * 
     * @return the name
     */
    @NonnullAfterInit public String getSAMLName() {
        return theSAMLName;
    }

    /**
     * Get the class which converts types.
     * 
     * @param mapper The valueMapper to set.
     */
    public void setValueMapper(AbstractSAMLAttributeValueMapper mapper) {
        valueMapper = mapper;
    }

    /**
     * Get the class which converts types.
     * 
     * @return Returns the valueMapper.
     */
    @NonnullAfterInit public AbstractSAMLAttributeValueMapper getValueMapper() {
        return valueMapper;
    }

    /** {@inheritDoc} */
    public void setId(@Nullable String id) {
        super.setId(id);
    }

    /**
     * Sets the (optional) attribute format.
     * 
     * @param format The attributeFormat to set.
     */
    public void setAttributeFormat(@Nullable final String format) {
        attributeFormat = StringSupport.trimOrNull(format);
    }

    /**
     * Get the (optional) attribute format.
     * 
     * @return Returns the format.
     */
    @Nullable public String getAttributeFormat() {
        return attributeFormat;
    }

    /** {@inheritDoc} */
    protected void doInitialize() throws ComponentInitializationException {
        super.doInitialize();
        if (null == theSAMLName) {
            throw new ComponentInitializationException(getLogPrefix() + " SAML name not present");
        }
        if (null == valueMapper) {
            throw new ComponentInitializationException(getLogPrefix() + " No value mapper present");
        }
        if (attributeIds.isEmpty()) {
            throw new ComponentInitializationException(getLogPrefix() + 
                    " At least one attribute Id should be provided");
        }
        logPrefix = null;
        valueMapper.setLogPrefix(getLogPrefix());
    }

    /**
     * Determines if the attribute matches the provided parameterisation.
     * 
     * @param attribute the attribute to consider
     * @return whether it matches.
     */
    protected boolean attributeMatches(@Nonnull InType attribute) {
        final String name = attribute.getName();

        if (!name.equals(theSAMLName)) {
            log.debug("{} SAML attribute name {} does not match {}", getLogPrefix(), name, getId());
            return false;
        }

        // Ignore format if it's "unspecified".
        String format = attribute.getNameFormat();
        if (org.opensaml.saml.saml2.core.Attribute.UNSPECIFIED.equals(format)) {
            format = null;
        }

        if (getAttributeFormat() != null && format != null && !getAttributeFormat().equals(format)
                && !org.opensaml.saml.saml2.core.Attribute.UNSPECIFIED.equals(getAttributeFormat())) {
            log.debug("{} SAML name format {} does not match {}", getLogPrefix(), format, getAttributeFormat());
            return false;
        }
        return true;
    }

    /**
     * Map the SAML attribute to the required output type. We have to be careful about handling attributes types. If the
     * input has values but we fail to convert them then that is different from not having any values and we signal this
     * by putting in a name, but no attribute.
     * 
     * @param prototype the SAML attribute
     * @return the appropriate multimap map of names to {@link RequestedAttribute}s.
     * 
     */
    @Nonnull @NullableElements public Map<String, OutType> mapAttribute(@Nonnull InType prototype) {

        ComponentSupport.ifNotInitializedThrowUninitializedComponentException(this);

        if (!attributeMatches(prototype)) {
            return Collections.EMPTY_MAP;
        }

        final List<XMLObject> inputValues = prototype.getAttributeValues();
        final List<AttributeValue> outputValues = getValueMapper().decodeValues(inputValues);

        final boolean noMatch = !inputValues.isEmpty() && outputValues.isEmpty();

        final Map<String, OutType> output = new HashMap<String, OutType>(inputValues.size());

        log.debug("{} attribute id {} and aliases {} will be created", getLogPrefix(), getId(), getAttributeIds());

        if (noMatch) {
            // NOTE this is different from not matching, so we set a NULL entry
            log.debug("{} Attribute value conversion yielded no suitable values", getLogPrefix());
            for (String alias : getAttributeIds()) {
                output.put(alias, null);
            }
            return output;
        }

        OutType out = newAttribute(prototype, getId());
        out.setValues(outputValues);

        for (String id : attributeIds) {
            out = newAttribute(prototype, getId());
            out.setValues(outputValues);
            output.put(id, out);
        }
        return output;
    }

    /**
     * return a string which is to be prepended to all log messages.
     * 
     * @return "<type> Attribute Mapper '<mapper ID>' :"
     */
    protected String getLogPrefix() {
        // local cache of cached entry to allow unsynchronised clearing.
        String prefix = logPrefix;
        if (null == prefix) {
            StringBuilder builder = new StringBuilder();
            if (null != getValueMapper()) {
                builder.append(getValueMapper().getAttributeTypeName()).append(" ");
            }

            builder.append("Attribute Mapper '").append(getId()).append("':");
            prefix = builder.toString();
            if (null == logPrefix) {
                logPrefix = prefix;
            }
        }
        return prefix;
    }

    /**
     * Function to summon up the output type based on an ID and an object of the input type. Typically the input type
     * will be inspected for extra paramterisation.
     * 
     * @param input the input value to inspect.
     * @param id the identifier of the new attribute.
     * @return an output, suitable set up with per object information.
     */
    protected abstract OutType newAttribute(@Nonnull InType input, @Nonnull @NotEmpty String id);
}
