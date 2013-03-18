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

package net.shibboleth.idp.attribute.filtering.impl.matcher.attributevalue;

import javax.annotation.Nullable;

import net.shibboleth.idp.attribute.AttributeValue;
import net.shibboleth.utilities.java.support.component.InitializableComponent;

/**
 * Interface which is implemented by all AttibuteValue {@link MatchFunctors}.
 * This interface used as a parameter various helper functions to easy the construction
 * of the classes that do the work.
 */
public interface TargetedMatchFunctor extends InitializableComponent {

    /**
     * Perform comparison.
     * @param value the attribute value to inspect
     * @return whether it matches the configuration of this match function.
     */
    public boolean compareAttributeValue(@Nullable AttributeValue value);
    
    /**
     * Gets the ID of the attribute whose values will be evaluated.
     * 
     * @return ID of the attribute whose values will be evaluated
     */
    public String getAttributeId();

    /**
     * Sets the ID of the attribute whose values will be evaluated.
     * 
     * @param id ID of the attribute whose values will be evaluated
     */
    public void setAttributeId(String id);

}
