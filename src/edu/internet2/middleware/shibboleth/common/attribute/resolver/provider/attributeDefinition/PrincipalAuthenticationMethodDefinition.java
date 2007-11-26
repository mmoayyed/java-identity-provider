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

package edu.internet2.middleware.shibboleth.common.attribute.resolver.provider.attributeDefinition;

import org.opensaml.xml.util.DatatypeHelper;

import edu.internet2.middleware.shibboleth.common.attribute.BaseAttribute;
import edu.internet2.middleware.shibboleth.common.attribute.provider.BasicAttribute;
import edu.internet2.middleware.shibboleth.common.attribute.resolver.AttributeResolutionException;
import edu.internet2.middleware.shibboleth.common.attribute.resolver.provider.ShibbolethResolutionContext;

/**
 * Attribute definition that exposes the principals authentication method as an attribute.
 */
public class PrincipalAuthenticationMethodDefinition extends BaseAttributeDefinition {

    /** {@inheritDoc} */
    protected BaseAttribute<String> doResolve(ShibbolethResolutionContext resolutionContext) {
        String authnMethod = resolutionContext.getAttributeRequestContext().getPrincipalAuthenticationMethod();

        if(DatatypeHelper.isEmpty(authnMethod)){
            return null;
        }
        
        BasicAttribute<String> attribute = new BasicAttribute<String>();
        attribute.setId(getId());
        attribute.getValues().add(authnMethod);
        return attribute;
    }

    /** {@inheritDoc} */
    public void validate() throws AttributeResolutionException {
        // do nothing
    }
}