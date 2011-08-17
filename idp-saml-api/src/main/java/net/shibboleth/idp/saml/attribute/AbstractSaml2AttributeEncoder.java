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

package net.shibboleth.idp.saml.attribute;

import org.opensaml.common.xml.SAMLConstants;
import org.opensaml.saml2.core.Attribute;
import org.opensaml.util.StringSupport;
import org.opensaml.util.component.UnmodifiableComponentException;

/** Base class for encoders that produce a SAML 2 {@link Attribute}. */
public abstract class AbstractSaml2AttributeEncoder extends AbstractSamlAttributeEncoder<Attribute> {

    /** A friendly, human readable, name for the attribute. */
    private String friendlyName;

    /** {@inheritDoc} */
    public final String getProtocol() {
        return SAMLConstants.SAML20P_NS;
    }

    /**
     * Gets the friendly, human readable, name for the attribute.
     * 
     * @return friendly, human readable, name for the attribute
     */
    public final String getFriendlyName() {
        return friendlyName;
    }

    /**
     * Sets the friendly, human readable, name for the attribute.
     * 
     * @param attributeFriendlyName friendly, human readable, name for the attribute
     */
    public final synchronized void setFriendlyName(String attributeFriendlyName) {
        if (isInitialized()) {
            throw new UnmodifiableComponentException(
                    "Friendly attribute name can not be changed after encoder has been initialized");
        }
        friendlyName = StringSupport.trimOrNull(attributeFriendlyName);
    }
}