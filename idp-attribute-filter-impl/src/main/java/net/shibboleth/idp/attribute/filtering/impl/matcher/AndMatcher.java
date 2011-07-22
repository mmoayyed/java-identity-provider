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

import net.jcip.annotations.ThreadSafe;
import net.shibboleth.idp.attribute.Attribute;
import net.shibboleth.idp.attribute.filtering.AttributeFilterContext;
import net.shibboleth.idp.attribute.filtering.AttributeFilteringException;
import net.shibboleth.idp.attribute.filtering.AttributeValueMatcher;

import org.opensaml.util.Assert;
import org.opensaml.util.collections.CollectionSupport;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Implement the AND matcher.
 * 
 * This is a difficult concept. We implement AND by saying that if a particular value occurs in the results returned by
 * all the child rules then it will be returned. (the the value was OK'd by matcher one AND the value was OK'd by
 * matcher2 AND .... <br />
 * However it seems likely that such a constraint is erroneous...
 */
@ThreadSafe
public class AndMatcher implements AttributeValueMatcher {

    /** Class logger. */
    private final Logger log = LoggerFactory.getLogger(OrMatcher.class);

    /**
     * The supplied matchers to be ORed together.
     * 
     * This list in unmodifiable.
     */
    private final List<AttributeValueMatcher> matchers;

    /**
     * Constructor.
     * 
     * @param theMatchers a list of sub matchers.
     */
    public AndMatcher(final List<AttributeValueMatcher> theMatchers) {

        final List<AttributeValueMatcher> workingMatcherList = new ArrayList<AttributeValueMatcher>();

        log.info("AND matcher as part of a Permit or Deny rule is likely to be a configuration error");

        CollectionSupport.addNonNull(theMatchers, workingMatcherList);
        if (workingMatcherList.isEmpty()) {
            log.warn("No sub-matchers provided to AND Value Matcher, this always returns no results");
        }
        matchers = Collections.unmodifiableList(workingMatcherList);
    }

    /** private default constructor to force the invariant of matchers being non null. */
    @SuppressWarnings("unused")
    private AndMatcher() {
        Assert.isFalse(true, "uncallable code");
        matchers = null;
    }

    /**
     * Get the sub matchers which are to be OR'd.
     * 
     * @return the sub matchers. This is never null or empty,
     */
    public List<AttributeValueMatcher> getSubMatchers() {
        return matchers;
    }

    /** {@inheritDoc} */
    public Collection<?> getMatchingValues(Attribute<?> attribute, AttributeFilterContext filterContext)
            throws AttributeFilteringException {

        if (matchers.isEmpty()) {
            return Collections.emptySet();
        }

        if (matchers.size() == 1) {
            // just return all the values returned by the child
            return matchers.get(0).getMatchingValues(attribute, filterContext);
        }

        // Grab all the values allowed by all the filters
        List<Collection> valueSets = new ArrayList<Collection>(matchers.size());
        for (AttributeValueMatcher matcher : matchers) {
            valueSets.add(matcher.getMatchingValues(attribute, filterContext));
        }
        // Check each value against all the matchers.
        // Rather than start with all values and compare against all
        // matchers we'll start with the values allowed by the first matcher
        // and compare against all the others, bailing out as soon as we
        // see a failure.
        //
        final Set result = new HashSet();

        for (Object value : valueSets.get(0)) {
            // Compare in all the other sets
            int i = 1;
            while (i < valueSets.size() && valueSets.get(i).contains(value)) {
                // That one matches, try the next one
                i++;
            }
            if (i >= valueSets.size()) {
                // we matched all the children, so add the data in
                result.add(value);
            }
        }

        if (result.isEmpty()) {
            Collections.emptySet();
        }
        return Collections.unmodifiableCollection(result);
    }
}
