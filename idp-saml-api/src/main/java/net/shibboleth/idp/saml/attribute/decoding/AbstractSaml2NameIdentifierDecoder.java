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

package net.shibboleth.idp.saml.attribute.decoding;

import org.opensaml.common.xml.SAMLConstants;
import org.opensaml.saml2.core.NameID;

/**
 * Base class for decoders that read SAML 2 name identifiers.
 * 
 * @param <ValueType> type of the values of the IdP attribute
 */
public abstract class AbstractSaml2NameIdentifierDecoder<ValueType> extends
        AbstractSamlNameIdentifierDecoder<NameID, ValueType> {

    /** {@inheritDoc} */
    public final String getProtocol() {
        return SAMLConstants.SAML20P_PREFIX;
    }
}