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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import net.shibboleth.idp.attribute.Attribute;
import net.shibboleth.idp.attribute.filtering.AttributeFilterContext;
import net.shibboleth.idp.attribute.filtering.AttributeFilteringException;
import net.shibboleth.idp.attribute.filtering.AttributeValueMatcher;

import org.opensaml.util.Assert;
import org.opensaml.util.collections.CollectionSupport;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Implement the OR matcher.
 * 
 * All elements from all child matchers are combined into the resultant set.
 */
public class OrMatcher implements AttributeValueMatcher {

    /** Class logger. */
    private final Logger log = LoggerFactory.getLogger(OrMatcher.class);

    /**
     * The supplied matchers to be ORed  together.
     * 
     * This list in unmodifiable.
     */
    private final List<AttributeValueMatcher> matchers;

    /**
     * Constructor.
     * 
     * @param theMatchers a list of sub matchers.
     */
    public OrMatcher(final List<AttributeValueMatcher> theMatchers) {

        final List<AttributeValueMatcher> workingMatcherList =
                new ArrayList<AttributeValueMatcher>();

        CollectionSupport.addNonNull(theMatchers, workingMatcherList);
        if (workingMatcherList.isEmpty()) {
            log.warn("No sub-matchers provided to OR Value Matcher, this always returns no results");
        }
        matchers = Collections.unmodifiableList(workingMatcherList);
    }
    
    /** private default constructor to force the invariant of matchers being non null. */
    @SuppressWarnings("unused")
    private OrMatcher() {
        Assert.isFalse(true, "uncallable code");
        matchers = null;
    }
    
    /**
     * Get the sub matchers which are to be OR'd.
     * @return the sub matchers. This is never null or empty,
     */
    public List<AttributeValueMatcher> getSubMatchers() {
        return matchers;
    }
    
    /** {@inheritDoc} */
    public Collection<?> getMatchingValues(Attribute<?> attribute, AttributeFilterContext filterContext)
            throws AttributeFilteringException {
        
        final Set result = new HashSet(); 
        
        for (AttributeValueMatcher matcher:matchers) {
            result.addAll(matcher.getMatchingValues(attribute, filterContext));
        }
        if (result.isEmpty()) {
            return Collections.emptySet();
        }
        return Collections.unmodifiableCollection(result);
    }
}
