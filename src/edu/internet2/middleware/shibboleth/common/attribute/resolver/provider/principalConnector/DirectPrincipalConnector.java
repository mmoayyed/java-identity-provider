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

package edu.internet2.middleware.shibboleth.common.attribute.resolver.provider.principalConnector;

import org.opensaml.common.SAMLObject;
import org.opensaml.saml1.core.NameIdentifier;
import org.opensaml.saml2.core.NameID;

import edu.internet2.middleware.shibboleth.common.attribute.resolver.AttributeResolutionException;
import edu.internet2.middleware.shibboleth.common.attribute.resolver.provider.ShibbolethResolutionContext;

/**
 * Principal connector that uses the SAML name identifier value as the principal name.
 */
public class DirectPrincipalConnector extends BasePrincipalConnector {

    /** {@inheritDoc} */
    public String resolve(ShibbolethResolutionContext resolutionContext) throws AttributeResolutionException {
        SAMLObject nameIdentifier = resolutionContext.getAttributeRequestContext().getSubjectNameIdentifier();

        if (nameIdentifier instanceof NameIdentifier) {
            return ((NameIdentifier) nameIdentifier).getNameIdentifier();
        } else if (nameIdentifier instanceof NameID) {
            return ((NameID) nameIdentifier).getValue();
        } else {
            return null;
        }
    }

    /** {@inheritDoc} */
    public void validate() throws AttributeResolutionException {

    }
}