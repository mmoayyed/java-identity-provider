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

package net.shibboleth.idp.attribute.filter.impl.policyrule.saml.attributemapper;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import net.shibboleth.idp.attribute.AttributeValue;
import net.shibboleth.idp.attribute.filter.RequestedAttribute;
import net.shibboleth.utilities.java.support.annotation.constraint.NonnullAfterInit;
import net.shibboleth.utilities.java.support.annotation.constraint.NonnullElements;
import net.shibboleth.utilities.java.support.annotation.constraint.NullableElements;
import net.shibboleth.utilities.java.support.annotation.constraint.Unmodifiable;
import net.shibboleth.utilities.java.support.component.AbstractIdentifiableInitializableComponent;
import net.shibboleth.utilities.java.support.component.ComponentInitializationException;
import net.shibboleth.utilities.java.support.component.ComponentSupport;
import net.shibboleth.utilities.java.support.primitive.StringSupport;

import org.opensaml.core.xml.XMLObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A class that maps SAML2 {@link org.opensaml.saml.saml2.metadata.RequestedAttribute} into an IdP
 * {@link RequestedAttribute}.
 * 
 * TODO make this general on all types which extend SAML attributes.
 */
public class RequestedAttributeMapper extends AbstractIdentifiableInitializableComponent {

    /** log. */
    private Logger log = LoggerFactory.getLogger(RequestedAttributeMapper.class);
    
    /** The internal names to generate. */
    private List<String> attributeAliases = Collections.EMPTY_LIST;

    /** The attribute format. */
    private String attributeFormat;

    /** the (SAML) attribute name. */
    private String theSAMLName;

    /** The String used to prefix log message. */
    private String logPrefix;

    /** The value mapper. */
    private BaseAttributeValueMapper valueMapper;

    /**
     * sets the list of internal identifiers.
     * 
     * @param aliases the list
     */
    public void setAliases(@Nullable @NullableElements final List<String> aliases) {
        ComponentSupport.ifInitializedThrowUnmodifiabledComponentException(this);
        if (null == aliases) {
            return;
        }

        final ArrayList<String> newList = new ArrayList<String>(aliases.size());

        for (String s : aliases) {
            final String trimmed = StringSupport.trimOrNull(s);
            if (null != trimmed) {
                newList.add(trimmed);
            }
        }
        attributeAliases = Collections.unmodifiableList(newList);
    }

    /**
     * gets the list of internal identifiers.
     * 
     * @return newIds the list
     */
    @Nonnull @NonnullElements @Unmodifiable public List<String> getAliases() {
        return attributeAliases;
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
     * Get the class which converts types.
     * 
     * @return Returns the valueMapper.
     */
    @NonnullAfterInit public BaseAttributeValueMapper getValueMapper() {
        return valueMapper;
    }

    /**
     * Get the class which converts types.
     * 
     * @param mapper The valueMapper to set.
     */
    public void setValueMapper(BaseAttributeValueMapper mapper) {
        valueMapper = mapper;
    }

    /**
     * Gets the SAML attribute name.
     * 
     * @return the name
     */
    @NonnullAfterInit public String getSAMLName() {
        return theSAMLName;
    }

    /** {@inheritDoc} */
    public void setId(@Nullable String id) {
        super.setId(id);
    }

    /**
     * Get the (optional) attribute format.
     * 
     * @return Returns the format.
     */
    @Nullable public String getAttributeFormat() {
        return StringSupport.trimOrNull(attributeFormat);
    }

    /**
     * Sets the (optional) attribute format.
     * 
     * @param format The attributeFormat to set.
     */
    public void setAttributeFormat(String format) {
        attributeFormat = format;
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
        logPrefix = null;
        valueMapper.setLogPrefix(getLogPrefix());
    }

    /**
     * Map the SAML attribute to the required output type.  We have to be careful about handling
     * attributes types.  If the input has values but we fail to convert them then that is different
     * from not having any values and we signal this by putting in a name, but no attribute.
     * 
     * @param prototype the SAML attribute
     * @return the appropriate multimap map of names to {@link RequestedAttribute}s. 
     *
     */
    @Nonnull @NullableElements protected Map<String, RequestedAttribute> mapAttribute(
            org.opensaml.saml.saml2.metadata.RequestedAttribute prototype) {

        ComponentSupport.ifNotInitializedThrowUninitializedComponentException(this);

        final String name = prototype.getName();
        
        if (!name.equals(theSAMLName)) {
            log.debug("{} SAML attribute name {} does not match {}", getLogPrefix(), name, getId());
            return Collections.EMPTY_MAP;
        }
        
        final String format = prototype.getNameFormat();
        if (getAttributeFormat() != null && !getAttributeFormat().equals(format)) {
            log.debug("{} SAML name format {} does not match {}", getLogPrefix(), format, getAttributeFormat());
            return Collections.EMPTY_MAP;
        }


        List<XMLObject> inputValues =  prototype.getAttributeValues();
        List<AttributeValue> outputValues = getValueMapper().decodeValues(inputValues);
        
        boolean noMatch = !inputValues.isEmpty() && outputValues.isEmpty();

        final Map<String, RequestedAttribute> output =  new HashMap<String, RequestedAttribute>(inputValues.size());

        log.debug("{} attribute id {} and aliases {} will be created", getLogPrefix(), getId(), getAliases());

        
        if (noMatch) {
            log.debug("{} Attribute value conversion yielded no suitable values", getLogPrefix());
            output.put(getId(), null);
            for (String alias: getAliases()) {
                output.put(alias, null);
            }
            return output;
        }
  
        RequestedAttribute out = new RequestedAttribute(getId());
        out.setRequired(prototype.isRequired());
        out.setValues(outputValues);
        output.put(getId(), out);
        
        for (String id : attributeAliases) {
            out = new RequestedAttribute(id);
            out.setRequired(prototype.isRequired());
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

}
