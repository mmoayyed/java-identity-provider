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

import org.opensaml.util.component.DestructableComponent;

/**
 * Simple class to test destruction.
 */
public class DestroyableAttributeValueStringMatcher extends AttributeValueStringMatcher implements DestructableComponent
{
    /**
     * Constructor.
     *
     * @param match what to match against.
     * @param isCaseSensitive whether the check is case sensitive.
     */
    protected DestroyableAttributeValueStringMatcher(String match, boolean isCaseSensitive) {
        super(match, isCaseSensitive);
    }
    private boolean destroyed; 

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
    
}