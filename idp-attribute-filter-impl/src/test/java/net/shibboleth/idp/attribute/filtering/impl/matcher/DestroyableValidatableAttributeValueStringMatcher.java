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

package net.shibboleth.idp.attribute.filtering.impl.matcher;

import net.shibboleth.utilities.java.support.component.ComponentValidationException;
import net.shibboleth.utilities.java.support.component.DestructableComponent;
import net.shibboleth.utilities.java.support.component.ValidatableComponent;

/**
 * Simple class to test destruction.
 */
public class DestroyableValidatableAttributeValueStringMatcher extends AttributeValueStringMatcher implements DestructableComponent, ValidatableComponent
{
    /**
     * Handy helper function to create the submatchers in the boolean testers.
     * @param pattern the pattern to match.
     * @param caseSensitive whether we are case sensitive.
     * @return a new matcher.
     */
    static public DestroyableValidatableAttributeValueStringMatcher newMatcher(String pattern, boolean caseSensitive) {
        DestroyableValidatableAttributeValueStringMatcher value = new DestroyableValidatableAttributeValueStringMatcher();
        value.setCaseSentitive(caseSensitive);
        value.setMatchString(pattern);
        return value;
    }

    private boolean destroyed; 
    private boolean validated;

    /** {@inheritDoc} */
    public void destroy() {
        destroyed = true;
    }

    /**
     * Has destroy been called?
     * @return whether destroyed has been called.
     */
    public boolean isDestroyed() {
        return destroyed;
        
    }

    /** {@inheritDoc} */
    public void validate() throws ComponentValidationException {
        validated = true;
    }
    
    /**
     * Has validated been called?
     * @return whether destroyed has been called.
     */
    public boolean isValidated() {
        return validated;
    }

}