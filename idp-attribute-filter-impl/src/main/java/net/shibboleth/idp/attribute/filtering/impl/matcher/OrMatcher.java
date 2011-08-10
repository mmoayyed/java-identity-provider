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
import org.opensaml.util.component.DestructableComponent;
import org.opensaml.util.component.InitializableComponent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Implement the OR matcher.
 * 
 * All elements from all child matchers are combined into the resultant set.
 */
@ThreadSafe
public class OrMatcher implements AttributeValueMatcher, InitializableComponent, DestructableComponent {

    /** Class logger. */
    private final Logger log = LoggerFactory.getLogger(OrMatcher.class);

    /** Initialized state.*/
    private boolean initialized;

    /** Destructor state. */
    private boolean destroyed;

    /**
     * The supplied matchers to be ORed together.
     * 
     * This list in unmodifiable.
     */
    private List<AttributeValueMatcher> matchers = Collections.emptyList();

    /** Constructor. */
    public OrMatcher() {
    }

    /**
     * Has initialize been called on this object. {@inheritDoc}.
     * */
    public boolean isInitialized() {
        return initialized;
    }

    /** Mark the object as initialized having initialized any children. {@inheritDoc}. */
    public void initialize() throws ComponentInitializationException {
        if (initialized) {
            throw new ComponentInitializationException("Or Matcher is initialized multiple times");
        }
        for (AttributeValueMatcher matcher : matchers) {
            if (matcher instanceof InitializableComponent) {
                InitializableComponent init = (InitializableComponent) matcher;
                init.initialize();
            }
        }
        initialized = true;
    }

    /** tear down any destructable children. {@inheritDoc} */
    public void destroy() {
        destroyed = true;
        for (AttributeValueMatcher matcher : matchers) {
            if (matcher instanceof DestructableComponent) {
                DestructableComponent destructee = (DestructableComponent) matcher;
                destructee.destroy();
            }
        }
        matchers = null;
    }
    
    /**
     * Set the sub-matchers.
     * @param theMatchers a list of sub matchers.
     */
    public void setSubMatchers(final List<AttributeValueMatcher> theMatchers) {
        final List<AttributeValueMatcher> workingMatcherList = new ArrayList<AttributeValueMatcher>();

        CollectionSupport.addNonNull(theMatchers, workingMatcherList);
        if (workingMatcherList.isEmpty()) {
            log.warn("No sub-matchers provided to OR Value Matcher, this always returns no results");
        }
        matchers = Collections.unmodifiableList(workingMatcherList);
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

        if (!initialized) {
            throw new AttributeFilteringException("Object has not been initialized");
        }
        // Capture submatchers.  Where we do this is important - after the initialized
        // test and before the destroyed test.
        final List<AttributeValueMatcher> theMatchers = getSubMatchers();
        if (destroyed) {
            throw new AttributeFilteringException("Object has been destroyed");
        }
        
        final Set result = new HashSet();
        
        for (AttributeValueMatcher matcher : theMatchers) {
            result.addAll(matcher.getMatchingValues(attribute, filterContext));
        }
        if (result.isEmpty()) {
            return Collections.emptySet();
        }
        return Collections.unmodifiableCollection(result);
    }
}
