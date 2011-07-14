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

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import net.shibboleth.idp.attribute.Attribute;
import net.shibboleth.idp.attribute.filtering.AttributeFilterContext;
import net.shibboleth.idp.attribute.filtering.AttributeFilteringException;
import net.shibboleth.idp.attribute.filtering.AttributeValueMatcher;

import org.opensaml.util.Assert;

/**
 * Implement the NOT matcher.
 * <br />
 * Anything returned from the sub matcher is removed from the attribute's list of values. 
 */
public class NotMatcher implements AttributeValueMatcher {

    /** The matcher we are NOT-ing. */
    private final AttributeValueMatcher subMatcher;
    
    /**
     * Constructor.
     *
     * @param child the matcher we will NOT with.
     */
    public NotMatcher(final AttributeValueMatcher child) {
        Assert.isNotNull(child, "Not matcher must have a child");
        subMatcher = child;
    }
    
    /** private Constructor to ensure that the sub matcher is never null. */
    @SuppressWarnings("unused")
    private NotMatcher() {
        Assert.isFalse(true, "unreachable code");
        subMatcher = null;
    }

    /** Get the matcher we are NOT ing.
     * @return the sub matcher, this is never NULL.
     */
    public AttributeValueMatcher getSubMatcher() {
        return subMatcher;
    }
    
    /** {@inheritDoc} */
    public Collection<?> getMatchingValues(final Attribute<?> attribute, final AttributeFilterContext filterContext)
            throws AttributeFilteringException {
        
        final Set result = new HashSet(attribute.getValues());
        for (Object value:subMatcher.getMatchingValues(attribute, filterContext)) {
            result.remove(value);
        }
        if (result.isEmpty()) {
            return Collections.emptySet();
        }
        return Collections.unmodifiableCollection(result);
    }
}
