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

package net.shibboleth.idp.saml.impl.attribute.decoding;

import net.shibboleth.idp.attribute.Attribute;
import net.shibboleth.idp.attribute.AttributeDecodingException;
import net.shibboleth.idp.saml.attribute.decoding.AbstractSaml2NameIdentifierDecoder;

import org.opensaml.saml2.core.NameID;

/** Decodes a SAML 2 {@link NameID} into a String by returning its {@link NameID#getValue()} value. */
public class Saml2NameIdentifierStringDecoder extends AbstractSaml2NameIdentifierDecoder<String> {

    /** {@inheritDoc} */
    protected void doDecode(Attribute<String> attribute, NameID nameId) throws AttributeDecodingException {
        attribute.getValues().add(nameId.getValue());
    }
}