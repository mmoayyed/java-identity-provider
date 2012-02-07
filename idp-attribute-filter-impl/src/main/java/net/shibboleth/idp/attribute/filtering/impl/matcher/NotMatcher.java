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

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import javax.annotation.Nonnull;

import net.jcip.annotations.ThreadSafe;
import net.shibboleth.idp.attribute.Attribute;
import net.shibboleth.idp.attribute.AttributeValue;
import net.shibboleth.idp.attribute.filtering.AttributeFilterContext;
import net.shibboleth.idp.attribute.filtering.AttributeFilteringException;
import net.shibboleth.idp.attribute.filtering.AttributeValueMatcher;
import net.shibboleth.utilities.java.support.component.AbstractDestructableInitializableComponent;
import net.shibboleth.utilities.java.support.component.ComponentInitializationException;
import net.shibboleth.utilities.java.support.component.ComponentSupport;
import net.shibboleth.utilities.java.support.component.ComponentValidationException;
import net.shibboleth.utilities.java.support.component.UninitializedComponentException;
import net.shibboleth.utilities.java.support.component.ValidatableComponent;
import net.shibboleth.utilities.java.support.logic.Assert;

import com.google.common.base.Objects;

/**
 * {@link AttributeValueMatcher} that implements the negation of a matcher. That is, a given attribute value is
 * considered to have matched if it is not returned the composed {@link AttributeValueMatcher}.
 */
@ThreadSafe
public final class NotMatcher extends AbstractDestructableInitializableComponent implements AttributeValueMatcher,
        ValidatableComponent {

    /** The matcher we are negating. */
    private AttributeValueMatcher negatedMatcher;

    /**
     * Constructor.
     *
     * @param valueMatcher attribute value matcher to be negated
     */
    public NotMatcher(@Nonnull final AttributeValueMatcher valueMatcher){
        negatedMatcher = Assert.isNotNull(valueMatcher, "Attribute value matcher can not be null");
    }
    
    /**
     * Get the matcher that is being negated.
     * 
     * @return matcher that is being negated
     */
    public AttributeValueMatcher getNegtedMatcher() {
        return negatedMatcher;
    }

    /** {@inheritDoc} */
    public Set<AttributeValue> getMatchingValues(final Attribute attribute, final AttributeFilterContext filterContext)
            throws AttributeFilteringException {
        assert attribute != null : "Attribute to be filtered can not be null";
        assert filterContext != null : "Attribute filter context can not be null";

        // Capture the matchers to avoid race with setComposedMatchers
        // Do this before the test on destruction to avoid race with destroy code
        final AttributeValueMatcher currentMatcher = getNegtedMatcher();
        ifNotInitializedThrowUninitializedComponentException();
        ifDestroyedThrowDestroyedComponentException();

        Set<AttributeValue> attributeValues = new HashSet<AttributeValue>(attribute.getValues());

        attributeValues.removeAll(currentMatcher.getMatchingValues(attribute, filterContext));

        if (attributeValues.isEmpty()) {
            return Collections.emptySet();
        }
        return Collections.unmodifiableSet(attributeValues);
    }

    /**
     * Validate the sub component.
     * 
     * @throws ComponentValidationException if any of the child validates failed.
     */
    public void validate() throws ComponentValidationException {
        if (!isInitialized()) {
            throw new UninitializedComponentException("Not Matcher not initialized");
        }
        ComponentSupport.validate(negatedMatcher);
    }

    /** {@inheritDoc} */
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }

        if (obj == this) {
            return true;
        }

        if (!(obj instanceof NotMatcher)) {
            return false;
        }

        return Objects.equal(negatedMatcher, ((NotMatcher) obj).negatedMatcher);
    }

    /** {@inheritDoc} */
    public int hashCode() {
        return negatedMatcher.hashCode();
    }

    /** {@inheritDoc} */
    public String toString() {
        return Objects.toStringHelper(this).add("negatedMatcher", negatedMatcher).toString();
    }

    /** {@inheritDoc} */
    protected void doDestroy() {
        ComponentSupport.destroy(negatedMatcher);
        negatedMatcher = null;
    }

    /** {@inheritDoc} */
    protected void doInitialize() throws ComponentInitializationException {
        super.doInitialize();

        ComponentSupport.initialize(negatedMatcher);
    }
}