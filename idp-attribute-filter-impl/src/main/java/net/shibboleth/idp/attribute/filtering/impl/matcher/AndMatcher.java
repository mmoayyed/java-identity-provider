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
import java.util.List;
import java.util.Set;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import net.jcip.annotations.ThreadSafe;
import net.shibboleth.idp.attribute.Attribute;
import net.shibboleth.idp.attribute.filtering.AttributeFilterContext;
import net.shibboleth.idp.attribute.filtering.AttributeFilteringException;
import net.shibboleth.idp.attribute.filtering.AttributeValueMatcher;
import net.shibboleth.utilities.java.support.annotation.constraint.NonnullElements;
import net.shibboleth.utilities.java.support.annotation.constraint.NullableElements;
import net.shibboleth.utilities.java.support.collection.TransformedInputCollectionBuilder;
import net.shibboleth.utilities.java.support.component.AbstractInitializableComponent;
import net.shibboleth.utilities.java.support.component.ComponentInitializationException;
import net.shibboleth.utilities.java.support.component.ComponentSupport;
import net.shibboleth.utilities.java.support.component.ComponentValidationException;
import net.shibboleth.utilities.java.support.component.DestructableComponent;
import net.shibboleth.utilities.java.support.component.ValidatableComponent;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Implement the composition of matchers, that is a logic AND.
 * 
 * This is a difficult concept. We implement AND by saying that if a particular value occurs in the results returned by
 * all the child rules then it will be returned. (the the value was OK'd by matcher one AND the value was OK'd by
 * matcher2 AND .... <br />
 */
@ThreadSafe
public class AndMatcher extends AbstractInitializableComponent implements AttributeValueMatcher, DestructableComponent,
        ValidatableComponent {

    /** Class logger. */
    private final Logger log = LoggerFactory.getLogger(OrMatcher.class);

    /** Destructor state. */
    private boolean destroyed;

    /** The supplied matchers to be ORed together. */
    private List<AttributeValueMatcher> matchers;

    /** Constructor. */
    public AndMatcher() {
        matchers = Collections.emptyList();
    }
    
    /** {@inheritDoc} */
    public boolean isDestroyed() {
        return destroyed;
    }

    /** {@inheritDoc} */
    public synchronized void destroy() {
        destroyed = true;
        for (AttributeValueMatcher matcher : matchers) {
            ComponentSupport.destroy(matcher);
        }
        // Clear after the setting of the flag top avoid race with getMatchingValues
        matchers = null;
    }

    /** {@inheritDoc} */
    public void validate() throws ComponentValidationException {
        if (!isInitialized()) {
            throw new ComponentValidationException("Matcher not initialized");
        }
        
        if(isDestroyed()){
            throw new ComponentValidationException("Matcher has been destroyed");
        }
        
        for (AttributeValueMatcher matcher : matchers) {
            ComponentSupport.validate(matcher);
        }
    }

    /**
     * Get the sub matchers which are to be AND'ed.
     * 
     * @return the composed matchers
     */
    @Nonnull @NonnullElements public List<AttributeValueMatcher> getComposedMatchers() {
        return matchers;
    }

    /**
     * Set the sub matchers which will be AND'ed together.
     * 
     * @param composedMatchers the composed matchers
     */
    public synchronized void setComposedMatchers(
            @Nullable @NullableElements final List<AttributeValueMatcher> composedMatchers) {
        if(isInitialized()){
            //TODO
        }
        
        if(isDestroyed()){
            //TODO
        }
        
        matchers =
                new TransformedInputCollectionBuilder<AttributeValueMatcher>().addAll(composedMatchers)
                        .buildImmutableList();
        if (matchers.isEmpty()) {
            log.warn("No sub-matchers provided to AND Value Matcher, this always returns no results");
        }
    }

    /**
     * Computer the logical "AND" of the set of values.<br />
     * Check each value against all the matchers. <br />
     * Rather than start with all values and compare against all matchers we'll start with the values allowed by the
     * first matcher and compare against all the others, bailing out as soon as we see a failure. <br />
     * 
     * @param valueSets the array of sets of child attributes
     * 
     * @return the "logical and" of the results (all values which are in all sets)
     */
    @Nonnull @NonnullElements private Collection<?> computeAnd(
            @Nullable @NullableElements final List<Collection> valueSets) {
        final Set result = new TransformedInputCollectionBuilder().buildSet();

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
    @Nonnull @NonnullElements public Collection<?> getMatchingValues(@Nonnull final Attribute<?> attribute,
            @Nonnull final AttributeFilterContext filterContext) throws AttributeFilteringException {
        assert attribute != null : "Attribute to be filtered can not be null";
        assert filterContext != null : "Attribute filter contet can not be null";

        if (!isInitialized()) {
            throw new AttributeFilteringException("And Matcher has not been initialized");
        }
        
        // NOTE capture the matchers to avoid race with setSubMatchers.
        // Do this before the test on destruction to avoid
        // race with destroy code.
        final List<AttributeValueMatcher> theMatchers = getComposedMatchers();
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

    /** {@inheritDoc} */
    protected void doInitialize() throws ComponentInitializationException {
        super.doInitialize();

        for (AttributeValueMatcher matcher : matchers) {
            ComponentSupport.initialize(matcher);
        }
    }
}