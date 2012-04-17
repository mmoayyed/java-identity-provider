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

import net.shibboleth.idp.attribute.Attribute;
import net.shibboleth.idp.attribute.AttributeEncodingException;
import net.shibboleth.idp.attribute.AttributeValue;
import net.shibboleth.idp.attribute.StringAttributeValue;
import net.shibboleth.idp.saml.attribute.encoding.AbstractSaml2AttributeEncoder;
import net.shibboleth.idp.saml.attribute.encoding.SamlEncoderSupport;

import org.opensaml.core.xml.XMLObject;

/**
 * {@link net.shibboleth.idp.attribute.AttributeEncoder} that produces SAML 2 attributes from
 * {@link net.shibboleth.idp.attribute.Attribute} that contains <code>String</code> values.
 */
public class Saml2StringAttributeEncoder extends AbstractSaml2AttributeEncoder<StringAttributeValue> {

    /** {@inheritDoc} */
    protected boolean canEncodeValue(net.shibboleth.idp.attribute.Attribute attribute, AttributeValue value) {
        return value instanceof StringAttributeValue;
    }

    /** {@inheritDoc} */
    protected XMLObject encodeValue(Attribute attribute, StringAttributeValue value) throws AttributeEncodingException {
        return SamlEncoderSupport.encodeStringValue(attribute,
                org.opensaml.saml.saml2.core.AttributeValue.DEFAULT_ELEMENT_NAME, value.getValue());
    }
}