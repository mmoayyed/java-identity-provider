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

package net.shibboleth.idp.ui.csrf;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.springframework.webflow.definition.StateDefinition;

import net.shibboleth.shared.annotation.constraint.NotEmpty;


/**
 * A base helper class for predicates that determine if CSRF protection is required per state. 
 */
public abstract class BaseCSRFTokenPredicate {
    
    /** 
     *  Name of the metadata attribute that, if true, excludes a view 
     *  from CSRF protection.
     */
    @Nonnull @NotEmpty public static final String CSRF_EXCLUDED_ATTRIBUTE_NAME = "csrf_excluded";
    
    
    /**
     * Safe get the <code>boolean</code> value of the attribute from the attributes annotating 
     * the {@link StateDefinition}. Returns the <code>defaultValue</code> if either:
     * <ul>
     *  <li>the <code>state</code> is null.</li>
     *  <li><code>attributeName</code> is null or no value is found.</li>
     *  <li>the value is found but is not a {@link Boolean}.</li>
     * </ul>
     * 
     * @param state the state definition to find the attribute from.
     * @param attributeName the name of the attribute to find.
     * @param defaultValue a default value.
     * @return the boolean value of the attribute on the state definition.
     */
    @Nonnull protected boolean safeGetBooleanStateAttribute(@Nullable final StateDefinition state, 
            @Nullable final String attributeName, @Nonnull final boolean defaultValue) {
        
        //catch no state exists. Return default.
        if (state==null) {
            return defaultValue;
        }
        try {
            return state.getAttributes().getBoolean(attributeName,defaultValue);            
        } catch (final IllegalArgumentException e) {
            //catch attribute exists but not a boolean. Return default.
            return defaultValue;
        }
        
    }

}
