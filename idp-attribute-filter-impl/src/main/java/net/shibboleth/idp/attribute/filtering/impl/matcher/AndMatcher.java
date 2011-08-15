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

import org.opensaml.util.collections.CollectionSupport;
import org.opensaml.util.component.ComponentInitializationException;
import org.opensaml.util.component.ComponentSupport;
import org.opensaml.util.component.ComponentValidationException;
import org.opensaml.util.component.DestructableComponent;
import org.opensaml.util.component.InitializableComponent;
import org.opensaml.util.component.ValidatableComponent;
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
public class AndMatcher implements AttributeValueMatcher, InitializableComponent, DestructableComponent,
        ValidatableComponent {

    /** Class logger. */
    private final Logger log = LoggerFactory.getLogger(OrMatcher.class);

    /** Initialized state. */
    private boolean initialized;

    /** Destructor state. */
    private boolean destroyed;

    /**
     * The supplied matchers to be ORed together.
     * 
     * This list in unmodifiable.
     */
    private List<AttributeValueMatcher> matchers = Collections.emptyList();

    /**
     * Constructor.
     */
    public AndMatcher() {
        log.info("AND matcher as part of a Permit or Deny rule is likely to be a configuration error");
    }

    /**
     * Has initialize been called on this object. {@inheritDoc}.
     * */
    public boolean isInitialized() {
        return initialized;
    }

    /** Mark the object as initialized having initialized any children. {@inheritDoc}. */
    public synchronized void initialize() throws ComponentInitializationException {
        if (initialized) {
            throw new ComponentInitializationException("And Matcher being initialized multiple times");
        }

        for (AttributeValueMatcher matcher : matchers) {
            ComponentSupport.initialize(matcher);
        }
        initialized = true;
    }

    /** tear down any destructable children. {@inheritDoc} */
    public void destroy() {
        destroyed = true;
        for (AttributeValueMatcher matcher : matchers) {
            ComponentSupport.destroy(matcher);
        }
        // Clear after the setting of the flag top avoid race with getMatchingValues
        matchers = null;
    }

    /**
     * Validate any validatable children. 
     * {@inheritDoc}
     * 
     * @throws ComponentValidationException if any of the child validates failed.
     */
    public void validate() throws ComponentValidationException {
        if (!initialized) {
            throw new ComponentValidationException("Object not initialized");
        }
        for (AttributeValueMatcher matcher : matchers) {
            ComponentSupport.validate(matcher);
        }
    }

    /**
     * Get the sub matchers which are to be AND'd.
     * 
     * @return the sub matchers. This is never null or empty,
     */
    public List<AttributeValueMatcher> getSubMatchers() {
        return matchers;
    }

    /**
     * Set the sub matchers which will be anded together.
     * 
     * @param newMatchers what to set.
     */
    public synchronized void setSubMatchers(final List<AttributeValueMatcher> newMatchers) {
        final List<AttributeValueMatcher> workingMatcherList = new ArrayList<AttributeValueMatcher>();
        CollectionSupport.addNonNull(newMatchers, workingMatcherList);
        if (workingMatcherList.isEmpty()) {
            log.warn("No sub-matchers provided to AND Value Matcher, this always returns no results");
        }
        matchers = Collections.unmodifiableList(workingMatcherList);
    }

    /**
     * Computer the logical "And" of the set of values.<br />
     * Check each value against all the matchers. <br />
     * Rather than start with all values and compare against all matchers we'll start with the values allowed by the
     * first matcher and compare against all the others, bailing out as soon as we see a failure. <br />
     * 
     * @param valueSets the array of sets of child attributes.
     * @return the "logical and" of the results (all values which are in all sets(
     */
    private Collection<?> computeAnd(List<Collection> valueSets) {
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
        return result;
    }

    /** {@inheritDoc} */
    public Collection<?> getMatchingValues(Attribute<?> attribute, AttributeFilterContext filterContext)
            throws AttributeFilteringException {

        if (!initialized) {
            throw new AttributeFilteringException("And Matcher has not been initialized");
        }
        // NOTE capture the matchers to avoid race with setSubMatchers.
        // Do this before the test on destruction to avoid
        // race with destroy code.
        final List<AttributeValueMatcher> theMatchers = getSubMatchers();
        if (destroyed) {
            throw new AttributeFilteringException("And Matcher has been destroyed");
        }

        if (theMatchers.isEmpty()) {
            return Collections.emptySet();
        }
        if (theMatchers.size() == 1) {
            // just return all the values returned by the child
            return theMatchers.get(0).getMatchingValues(attribute, filterContext);
        }

        // Grab all the values allowed by all the filters
        List<Collection> valueSets = new ArrayList<Collection>(theMatchers.size());
        for (AttributeValueMatcher matcher : theMatchers) {
            valueSets.add(matcher.getMatchingValues(attribute, filterContext));
        }

        final Collection result = computeAnd(valueSets);

        if (result.isEmpty()) {
            return Collections.emptySet();
        }
        return Collections.unmodifiableCollection(result);
    }

}
