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
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import javax.annotation.Nullable;
import javax.annotation.concurrent.ThreadSafe;

import net.shibboleth.idp.attribute.Attribute;
import net.shibboleth.idp.attribute.AttributeValue;
import net.shibboleth.idp.attribute.filtering.AttributeFilterContext;
import net.shibboleth.idp.attribute.filtering.AttributeFilteringException;
import net.shibboleth.idp.attribute.filtering.AttributeValueMatcher;
import net.shibboleth.utilities.java.support.annotation.constraint.NullableElements;
import net.shibboleth.utilities.java.support.component.ComponentSupport;

import com.google.common.base.Objects;

/**
 * {@link AttributeValueMatcher} that implements the disjunction of matchers. That is, a given attribute value is
 * considered to have matched if it is returned by any of the composed {@link AttributeValueMatcher}.
 */
@ThreadSafe
public class OrMatcher extends AbstractComposedMatcher implements AttributeValueMatcher {

    /**
     * Constructor.
     * 
     * @param composedMatchers matchers being composed
     */
    public OrMatcher(@Nullable @NullableElements final Collection<AttributeValueMatcher> composedMatchers) {
        super(composedMatchers);
    }

    /** {@inheritDoc} */
    public Set<AttributeValue> getMatchingValues(Attribute attribute, AttributeFilterContext filterContext)
            throws AttributeFilteringException {
        assert attribute != null : "Attribute to be filtered can not be null";
        assert filterContext != null : "Attribute filter context can not be null";

        // Capture the matchers to avoid race with setComposedMatchers
        // Do this before the test on destruction to avoid race with destroy code
        final List<AttributeValueMatcher> currentMatchers = getComposedMatchers();
        ComponentSupport.ifNotInitializedThrowUninitializedComponentException(this);
        ComponentSupport.ifDestroyedThrowDestroyedComponentException(this);

        if (currentMatchers.isEmpty()) {
            return Collections.emptySet();
        }
        Iterator<AttributeValueMatcher> matcherItr = currentMatchers.iterator();

        Set<AttributeValue> matchingValues = matcherItr.next().getMatchingValues(attribute, filterContext);
        while (matcherItr.hasNext()) {
            matchingValues.addAll(matcherItr.next().getMatchingValues(attribute, filterContext));
            if (matchingValues.isEmpty()) {
                return Collections.emptySet();
            }
        }

        return Collections.unmodifiableSet(matchingValues);
    }

    /** {@inheritDoc} */
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }

        if (obj == this) {
            return true;
        }

        if (!(obj instanceof OrMatcher)) {
            return false;
        }

        return Objects.equal(getComposedMatchers(), ((OrMatcher) obj).getComposedMatchers());
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