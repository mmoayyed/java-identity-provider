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

package net.shibboleth.idp.attribute.filter.impl.matcher.logic;

import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.concurrent.ThreadSafe;

import net.shibboleth.idp.attribute.Attribute;
import net.shibboleth.idp.attribute.AttributeValue;
import net.shibboleth.idp.attribute.filter.AttributeFilterContext;
import net.shibboleth.idp.attribute.filter.AttributeFilterException;
import net.shibboleth.idp.attribute.filter.Matcher;
import net.shibboleth.utilities.java.support.annotation.constraint.NonnullElements;
import net.shibboleth.utilities.java.support.annotation.constraint.NullableElements;
import net.shibboleth.utilities.java.support.component.ComponentInitializationException;
import net.shibboleth.utilities.java.support.component.ComponentSupport;
import net.shibboleth.utilities.java.support.logic.Constraint;

import com.google.common.base.Objects;

/**
 * {@link Matcher} that implements the conjunction of matchers. That is, a given attribute value is considered to have
 * matched if, and only if, it is returned by every composed {@link Matcher}. The predicate is true if and only if all
 * composed {@link Matcher} returns true.
 */
@ThreadSafe
public class AndMatcher extends AbstractComposedMatcher {

    /**
     * Constructor.
     * 
     * @param composedMatchers matchers being composed
     */
    public AndMatcher(@Nullable @NullableElements final Collection<Matcher> composedMatchers) {
        super(composedMatchers);
    }

    /**
     * Return true iff all composed matchers return true. {@inheritDoc}
     * 
     * @throws AttributeFilterException
     */
    public boolean matches(@Nullable final AttributeFilterContext filterContext) throws AttributeFilterException {
        final List<Matcher> currentMatchers = getComposedMatchers();
        ComponentSupport.ifNotInitializedThrowUninitializedComponentException(this);
        ComponentSupport.ifDestroyedThrowDestroyedComponentException(this);

        for (Matcher child : currentMatchers) {
            if (!child.matches(filterContext)) {
                return false;
            }
        }

        return true;
    }

    /**
     * A given attribute value is considered to have matched if, and only if, it is returned by every composed
     * {@link Matcher}. {@inheritDoc}
     */
    @Nonnull @NonnullElements public Set<AttributeValue> getMatchingValues(@Nonnull final Attribute attribute,
            @Nonnull final AttributeFilterContext filterContext) throws AttributeFilterException {
        Constraint.isNotNull(attribute, "Attribute to be filtered can not be null");
        Constraint.isNotNull(filterContext, "Attribute filter context can not be null");

        // Capture the matchers to avoid race with setComposedMatchers
        // Do this before the test on destruction to avoid race with destroy code
        final List<Matcher> currentMatchers = getComposedMatchers();
        ComponentSupport.ifNotInitializedThrowUninitializedComponentException(this);
        ComponentSupport.ifDestroyedThrowDestroyedComponentException(this);

        Iterator<Matcher> matcherItr = currentMatchers.iterator();

        Set<AttributeValue> matchingValues = matcherItr.next().getMatchingValues(attribute, filterContext);
        while (matcherItr.hasNext()) {
            matchingValues.retainAll(matcherItr.next().getMatchingValues(attribute, filterContext));
            if (matchingValues.isEmpty()) {
                return Collections.emptySet();
            }
        }

        return Collections.unmodifiableSet(matchingValues);
    }
    
    /** {@inheritDoc} */
    protected void doInitialize() throws ComponentInitializationException {
        super.doInitialize();
        if (getComposedMatchers().isEmpty()) {
            throw new ComponentInitializationException("No matchers supplied to AND");
        }
    }

    /** {@inheritDoc} */
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }

        if (obj == this) {
            return true;
        }

        if (!(obj instanceof AndMatcher)) {
            return false;
        }

        return Objects.equal(getComposedMatchers(), ((AndMatcher) obj).getComposedMatchers());
    }

    /** {@inheritDoc} */
    public int hashCode() {
        return getComposedMatchers().hashCode();
    }

    /** {@inheritDoc} */
    public String toString() {
        return Objects.toStringHelper(this).add("composedMatchers", getComposedMatchers()).toString();
    }

}