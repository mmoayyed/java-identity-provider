/*
 * Copyright [2007] [University Corporation for Advanced Internet Development, Inc.]
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package edu.internet2.middleware.shibboleth.common.xmlobject.impl;

import org.opensaml.xml.AbstractXMLObjectBuilder;

import edu.internet2.middleware.shibboleth.common.ShibbolethConstants;
import edu.internet2.middleware.shibboleth.common.xmlobject.ShibbolethMetadataKeyAuthority;

/**
 * Builder of {@link ShibbolethMetadataKeyAuthority} objects.
 */
public class ShibbolethMetadataKeyAuthorityBuilder extends AbstractXMLObjectBuilder<ShibbolethMetadataKeyAuthority> {

    /** {@inheritDoc} */
    public ShibbolethMetadataKeyAuthority buildObject(String namespaceURI, String localName, String namespacePrefix) {
        return new ShibbolethMetadataKeyAuthorityImpl(namespaceURI, localName, namespacePrefix);
    }
    
    /**
     * Build a KeyAuthority element with the default namespace prefix and element name.
     * 
     * @return a new instance of ShibbolethMetadataKeyAuthority
     */
    public ShibbolethMetadataKeyAuthority buildObject() {
        return buildObject(ShibbolethConstants.SHIB_MDEXT10_NS,
                ShibbolethMetadataKeyAuthority.DEFAULT_ELEMENT_LOCAL_NAME,
                ShibbolethConstants.SHIB_MDEXT10_PREFIX);
    }

}
