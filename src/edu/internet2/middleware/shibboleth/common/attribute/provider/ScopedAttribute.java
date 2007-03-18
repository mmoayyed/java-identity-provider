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

package edu.internet2.middleware.shibboleth.common.attribute.provider;

import org.opensaml.xml.util.DatatypeHelper;

/**
 * An attribute whose values contain a scope.
 */
public class ScopedAttribute extends BasicAttribute<String> {

    /** Attribute's value's scope. */
    private String scope;
    
    /**
     * Gets the scope of a value.
     * 
     * @return scope of a value
     */
    public String getScope(){
        return scope;
    }
    
    /**
     * Sets the scope of a value.
     * 
     * @param newScope scope of a value
     */
    public void setScope(String newScope){
        scope = DatatypeHelper.safeTrimOrNullString(newScope);
    }
}