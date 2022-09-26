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

package net.shibboleth.idp.cas.attribute;

import java.util.ArrayList;
import java.util.Collection;

import javax.annotation.Nonnull;

import net.shibboleth.shared.annotation.constraint.Live;
import net.shibboleth.shared.annotation.constraint.NonnullElements;
import net.shibboleth.shared.annotation.constraint.NotEmpty;
import net.shibboleth.shared.logic.Constraint;
import net.shibboleth.shared.primitive.StringSupport;

/**
 * Wrapper class for a CAS attribute/values construct in a validate response.
 */
public class Attribute {

    /** Name of attribute. */
    @Nonnull @NotEmpty private String name;
    
    /** String values. */
    @Nonnull @NonnullElements private final Collection<String> values;
    
    /**
     * Constructor.
     * 
     * @param theName name of attribute
     * 
     */
    public Attribute(@Nonnull @NotEmpty final String theName) {
        name = Constraint.isNotNull(StringSupport.trimOrNull(theName),
                "CAS attribute name cannot be null or empty.");
        
        values = new ArrayList<>();
    }
    
    /**
     * Get the attribute's name.
     * 
     * @return the name
     * 
     */
    @Nonnull @NotEmpty public String getName() {
        return name;
    }
    
    /**
     * Get string values.
     * 
     * @return string value collection
     */
    @Nonnull @NonnullElements @Live public Collection<String> getValues() {
        return values;
    }
    
}