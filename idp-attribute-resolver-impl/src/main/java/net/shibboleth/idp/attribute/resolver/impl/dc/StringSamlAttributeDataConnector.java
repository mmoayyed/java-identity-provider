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

package net.shibboleth.idp.attribute.resolver.impl.dc;

import net.shibboleth.idp.attribute.AttributeValue;
import net.shibboleth.idp.attribute.StringAttributeValue;
import net.shibboleth.utilities.java.support.primitive.StringSupport;

import org.opensaml.core.xml.XMLObject;
import org.opensaml.core.xml.schema.XSAny;
import org.opensaml.core.xml.schema.XSString;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Concrete class to encode a SAML attribute as String Attributes.
 */
public class StringSamlAttributeDataConnector extends BaseSamlAttributeDataConnector{

    /** Class logger. */
    private final Logger log = LoggerFactory.getLogger(StringSamlAttributeDataConnector.class);


    /** {@inheritDoc} */
    protected AttributeValue encodeValue(XMLObject object) {
        if (object instanceof XSAny) {
            XSAny any = (XSAny) object;
            String val = StringSupport.trimOrNull(any.getTextContent());
            if (null == val) {
                log.debug("XSAny value '{}' had no contents", any);
                return null;
            }
            log.debug("Encoding XSAny value '{}' as string", val);
            return new StringAttributeValue(val);
        }
        log.debug("Could not encode value {}", object);
        return null;
    }

}
