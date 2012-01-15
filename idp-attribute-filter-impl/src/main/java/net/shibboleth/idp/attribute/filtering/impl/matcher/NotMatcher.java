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

import net.jcip.annotations.ThreadSafe;
import net.shibboleth.idp.attribute.Attribute;
import net.shibboleth.idp.attribute.filtering.AttributeFilterContext;
import net.shibboleth.idp.attribute.filtering.AttributeFilteringException;
import net.shibboleth.idp.attribute.filtering.AttributeValueMatcher;
import net.shibboleth.utilities.java.support.component.AbstractInitializableComponent;
import net.shibboleth.utilities.java.support.component.ComponentInitializationException;
import net.shibboleth.utilities.java.support.component.ComponentSupport;
import net.shibboleth.utilities.java.support.component.ComponentValidationException;
import net.shibboleth.utilities.java.support.component.UninitializedComponentException;
import net.shibboleth.utilities.java.support.component.ValidatableComponent;

/**
 * Implement the NOT matcher. <br />
 * Anything returned from the sub matcher is removed from the attribute's list of values.
 */
@ThreadSafe
public class NotMatcher extends AbstractInitializableComponent implements AttributeValueMatcher, ValidatableComponent {

    /** The matcher we are NOT-ing. */
    private AttributeValueMatcher subMatcher;

    /** Destructor state. */
    private boolean destroyed;

    /**
     * Constructor.
     * 
     */
    public NotMatcher() {
    }

    /** tear down the child is destructable. {@inheritDoc} */
    public void destroy() {
        destroyed = true;
        ComponentSupport.destroy(subMatcher);
        subMatcher = null;
    }

    /**
     * Validate the sub component. {@inheritDoc}.
     * 
     * @throws ComponentValidationException if any of the child validates failed.
     */
    public void validate() throws ComponentValidationException {
        if (!isInitialized()) {
            throw new UninitializedComponentException("Not Matcher not initialized");
        }
        ComponentSupport.validate(subMatcher);
    }

    /**
     * Set the child matcher we are NOT ing.
     * 
     * @param child the matcher we will NOT with.
     */
    public synchronized void setSubMatcher(final AttributeValueMatcher child) {
        subMatcher = child;
    }

    /**
     * Get the matcher we are NOT ing.
     * 
     * @return the sub matcher, this is never NULL.
     */
    public AttributeValueMatcher getSubMatcher() {
        return subMatcher;
    }

    /** {@inheritDoc} */
    public Collection<?> getMatchingValues(final Attribute<?> attribute, final AttributeFilterContext filterContext)
            throws AttributeFilteringException {

        if (!isInitialized()) {
            throw new UninitializedComponentException("Not Matcher has not been initialized");
        }

        // capture the child to guarantee atomicity.
        final AttributeValueMatcher theSubMatcher = subMatcher;
        if (destroyed) {
            throw new AttributeFilteringException("Not Matcher has been destroyed");
        }

        final Collection subMatcherResults = theSubMatcher.getMatchingValues(attribute, filterContext);

        final Set result = new HashSet(attribute.getValues());
        for (Object value : subMatcherResults) {
            result.remove(value);
        }
        if (result.isEmpty()) {
            return Collections.emptySet();
        }
        return Collections.unmodifiableCollection(result);
    }

    /** {@inheritDoc} */
    protected void doInitialize() throws ComponentInitializationException {
        super.doInitialize();

        ComponentSupport.initialize(subMatcher);
    }
}