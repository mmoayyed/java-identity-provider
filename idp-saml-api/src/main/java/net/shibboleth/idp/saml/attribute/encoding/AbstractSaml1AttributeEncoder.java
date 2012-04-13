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

package net.shibboleth.idp.saml.attribute.encoding;

import java.util.List;

import net.shibboleth.idp.attribute.AttributeEncodingException;
import net.shibboleth.idp.attribute.AttributeValue;

import org.opensaml.core.xml.XMLObject;
import org.opensaml.core.xml.config.XMLObjectProviderRegistrySupport;
import org.opensaml.saml.common.SAMLObjectBuilder;
import org.opensaml.saml.common.xml.SAMLConstants;
import org.opensaml.saml.saml1.core.Attribute;

/**
 * Base class for encoders that produce a SAML 1 {@link Attribute}.
 * 
 * @param <EncodedType> the type of data that can be encoded by the encoder
 */
public abstract class AbstractSaml1AttributeEncoder<EncodedType extends AttributeValue> extends
        AbstractSamlAttributeEncoder<Attribute, EncodedType> {

    /** Builder used to construct {@link Attribute} objects. */
    private final SAMLObjectBuilder<Attribute> attributeBuilder;

    /** Constructor. */
    public AbstractSaml1AttributeEncoder() {
        super();

        attributeBuilder =
                (SAMLObjectBuilder<Attribute>) XMLObjectProviderRegistrySupport.getBuilderFactory().getBuilder(
                        Attribute.TYPE_NAME);
    }

    /** {@inheritDoc} */
    public final String getProtocol() {
        return SAMLConstants.SAML10P_NS;
    }

    /** {@inheritDoc} */
    protected Attribute buildAttribute(final net.shibboleth.idp.attribute.Attribute attribute,
            final List<XMLObject> attributeValues) throws AttributeEncodingException {

        final Attribute samlAttribute = attributeBuilder.buildObject();
        samlAttribute.setAttributeName(getName());
        samlAttribute.setAttributeNamespace(getNamespace());
        samlAttribute.getAttributeValues().addAll(attributeValues);

        return samlAttribute;
    }
}