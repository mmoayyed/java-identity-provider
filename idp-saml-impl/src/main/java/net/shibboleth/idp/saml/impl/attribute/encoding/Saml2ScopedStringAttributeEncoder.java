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

package net.shibboleth.idp.saml.impl.attribute.encoding;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import net.shibboleth.idp.attribute.IdPAttribute;
import net.shibboleth.idp.attribute.AttributeEncodingException;
import net.shibboleth.idp.attribute.IdPAttributeValue;
import net.shibboleth.idp.attribute.ScopedStringAttributeValue;
import net.shibboleth.idp.attribute.mapper.IdPRequestedAttribute;
import net.shibboleth.idp.attribute.mapper.impl.RequestedAttributeMapper;
import net.shibboleth.idp.attribute.mapper.impl.ScopedStringAttributeValueMapper;
import net.shibboleth.idp.saml.attribute.encoding.AbstractSAML2AttributeEncoder;
import net.shibboleth.idp.saml.attribute.encoding.AttributeMapperFactory;
import net.shibboleth.idp.saml.attribute.encoding.SAMLEncoderSupport;
import net.shibboleth.utilities.java.support.component.ComponentInitializationException;
import net.shibboleth.utilities.java.support.component.ComponentSupport;
import net.shibboleth.utilities.java.support.primitive.StringSupport;

import org.opensaml.core.xml.XMLObject;
import org.opensaml.saml.saml2.core.AttributeValue;
import org.opensaml.saml.saml2.metadata.RequestedAttribute;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * {@link net.shibboleth.idp.attribute.AttributeEncoder} that produces SAML 2 attributes from
 * {@link net.shibboleth.idp.attribute.IdPAttribute} that contains scope string values.
 */
public class Saml2ScopedStringAttributeEncoder extends AbstractSAML2AttributeEncoder<ScopedStringAttributeValue>
        implements
        AttributeMapperFactory<RequestedAttribute, IdPRequestedAttribute> {

    /** The log. */
    private final Logger log = LoggerFactory.getLogger(Saml2ScopedStringAttributeEncoder.class);

    /** Type of scoping to use. */
    private String scopeType;

    /** Delimiter used for "inline" scopeType. */
    private String scopeDelimiter;

    /** Attribute name used for "attribute" scopeType. */
    private String scopeAttributeName;

    /**
     * Get the scope attribute.
     * 
     * @return Returns the scopeAttribute.
     */
    public String getScopeAttributeName() {
        return scopeAttributeName;
    }

    /**
     * Get the scope delimiter.
     * 
     * @return Returns the scopeDelimiter.
     */
    public String getScopeDelimiter() {
        return scopeDelimiter;
    }

    /**
     * Get the scope type.
     * 
     * @return Returns the scopeType.
     */
    public String getScopeType() {
        return scopeType;
    }

    /**
     * Set the scope attribute.
     * 
     * @param newScopeAttribute The scopeAttribute to set.
     */
    public void setScopeAttributeName(String newScopeAttribute) {
        ComponentSupport.ifInitializedThrowUnmodifiabledComponentException(this);
        scopeAttributeName = StringSupport.trimOrNull(newScopeAttribute);
    }

    /**
     * Set the scope delimiter.
     * 
     * @param newScopeDelimiter The scopeDelimiter to set.
     */
    public void setScopeDelimiter(String newScopeDelimiter) {
        ComponentSupport.ifInitializedThrowUnmodifiabledComponentException(this);
        scopeDelimiter = StringSupport.trimOrNull(newScopeDelimiter);
    }

    /**
     * Set the scope type.
     * 
     * @param newScopeType The scopeType to set.
     */
    public void setScopeType(String newScopeType) {
        ComponentSupport.ifInitializedThrowUnmodifiabledComponentException(this);
        scopeType = StringSupport.trimOrNull(newScopeType);
    }

    /**
     * Ensures that the values we will need are all there.
     * 
     * {@inheritDoc}
     */
    protected void doInitialize() throws ComponentInitializationException {
        super.doInitialize();

        if (null == getScopeType()) {
            log.debug("Scope type not set, assuming \"inline\"");
            scopeType = "inline";
        }

        if ("attribute".equals(getScopeType())) {
            if (null == getScopeAttributeName()) {
                throw new ComponentInitializationException(
                        "Encoder of type \"attribute\" must specify a scope AttributeName");
            }
            if (null != getScopeDelimiter()) {
                log.warn("Scope delimiter {} not valid for type \"attribute\"", getScopeDelimiter());
            }
        } else if ("inline".equals(getScopeType())) {
            if (null == getScopeDelimiter()) {
                throw new ComponentInitializationException("Encoder of type \"inline\" must specify a delimiter");
            }
            if (null != getScopeAttributeName()) {
                log.warn("Scope Attribute name {} not valid for type \"inline\"", getScopeAttributeName());
            }
        } else {
            throw new ComponentInitializationException("Encoder type must be set to \"inline\" or \"attribute\"");
        }
    }

    /** {@inheritDoc} */
    protected boolean canEncodeValue(IdPAttribute attribute, IdPAttributeValue value) {
        ComponentSupport.ifNotInitializedThrowUninitializedComponentException(this);
        return value instanceof ScopedStringAttributeValue;
    }

    /** {@inheritDoc} */
    @Nullable protected XMLObject encodeValue(IdPAttribute attribute, ScopedStringAttributeValue value)
            throws AttributeEncodingException {
        ComponentSupport.ifNotInitializedThrowUninitializedComponentException(this);
        if ("attribute".equals(getScopeType())) {
            return SAMLEncoderSupport.encodeScopedStringValueAttribute(attribute,
                    AttributeValue.DEFAULT_ELEMENT_NAME, value, getScopeAttributeName());
        } else {
            return SAMLEncoderSupport.encodeScopedStringValueInline(attribute,
                    AttributeValue.DEFAULT_ELEMENT_NAME, value, getScopeDelimiter());
        }
    }

    /** {@inheritDoc} */
    @Nonnull public RequestedAttributeMapper getRequestedMapper() {
        final RequestedAttributeMapper val;

        val = new RequestedAttributeMapper();
        val.setAttributeFormat(getNameFormat());
        val.setId(getFriendlyName());
        val.setSAMLName(getName());
        final ScopedStringAttributeValueMapper foo = new ScopedStringAttributeValueMapper();
        foo.setDelimiter(getScopeDelimiter());
        val.setValueMapper(foo);

        return val;
    }

}