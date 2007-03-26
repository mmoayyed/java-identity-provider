/*
 * Copyright [2006] [University Corporation for Advanced Internet Development, Inc.]
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

import org.apache.log4j.Logger;

import edu.internet2.middleware.shibboleth.common.attribute.Attribute;
import edu.internet2.middleware.shibboleth.common.attribute.provider.ScopedAttribute;
import edu.internet2.middleware.shibboleth.common.attribute.resolver.AttributeResolutionException;
import edu.internet2.middleware.shibboleth.common.attribute.resolver.provider.ShibbolethResolutionContext;

/**
 * Simple attribute definition.
 */
public class ScopedAttributeDefinition extends BaseAttributeDefinition {

    /** Log4j logger. */
    private static Logger log = Logger.getLogger(ScopedAttributeDefinition.class);

    /** Scope value. */
    private String scope;
    
    /**
     * Constructor.
     *
     * @param newScope scope of the attribute
     */
    public ScopedAttributeDefinition(String newScope){
        this.scope = newScope;
    }
    
    /** {@inheritDoc} */
    public Attribute doResolve(ShibbolethResolutionContext resolutionContext) throws AttributeResolutionException {
        log.debug("Resolving attribute: (" + getId() + ")");

        ScopedAttribute attribute = new ScopedAttribute();
        attribute.setId(getId());
        attribute.setScope(scope);
        for (Object o : getValuesFromAllDependencies(resolutionContext)) {
            attribute.getValues().add(o.toString());
        }

        
        
        return attribute;
    }

    /**
     * Get scope value.
     * @return Returns the scope.
     */
    public String getScope() {
        return scope;
    }

    /** {@inheritDoc} */
    public void validate() throws AttributeResolutionException {
        //do nothing
    }
}