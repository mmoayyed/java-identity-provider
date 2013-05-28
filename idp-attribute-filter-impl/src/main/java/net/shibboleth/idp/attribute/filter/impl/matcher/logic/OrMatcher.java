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
import java.util.List;
import java.util.Set;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.concurrent.ThreadSafe;

import net.shibboleth.idp.attribute.Attribute;
import net.shibboleth.idp.attribute.AttributeValue;
import net.shibboleth.idp.attribute.filter.AttributeFilterContext;
import net.shibboleth.idp.attribute.filter.AttributeFilterException;
import net.shibboleth.idp.attribute.filter.MatchFunctor;
import net.shibboleth.utilities.java.support.annotation.constraint.NullableElements;
import net.shibboleth.utilities.java.support.collection.LazySet;
import net.shibboleth.utilities.java.support.component.ComponentSupport;
import net.shibboleth.utilities.java.support.logic.Constraint;

/**
 * {@link MatchFunctor} that implements the disjunction of matchers. That is, a given attribute value is considered to
 * have matched if it is returned by any of the composed {@link MatchFunctor}.
 */
@ThreadSafe
public class OrMatcher extends AbstractComposedMatcher {

    /**
     * Constructor.
     * 
     * @param composedMatchers matchers being composed
     */
    public OrMatcher(@Nullable @NullableElements final Collection<MatchFunctor> composedMatchers) {
        super(composedMatchers);
    }

    /** {@inheritDoc} */
    public boolean evaluatePolicyRule(@Nonnull AttributeFilterContext filterContext)
            throws AttributeFilterException {
        final List<MatchFunctor> currentMatchers = getComposedMatchers();
        ComponentSupport.ifNotInitializedThrowUninitializedComponentException(this);
        ComponentSupport.ifDestroyedThrowDestroyedComponentException(this);
        if (currentMatchers != null) {
            for (MatchFunctor child : currentMatchers) {
                if (child.evaluatePolicyRule(filterContext)) {
                    return true;
                }
            }
        }
        return false;
    }

    /** {@inheritDoc} */
    public Set<AttributeValue> getMatchingValues(Attribute attribute, AttributeFilterContext filterContext)
            throws AttributeFilterException {
        Constraint.isNotNull(attribute, "Attribute to be filtered can not be null");
        Constraint.isNotNull(filterContext, "Attribute filter context can not be null");

        // Capture the matchers to avoid race with setComposedMatchers
        // Do this before the test on destruction to avoid race with destroy code
        final List<MatchFunctor> currentMatchers = getComposedMatchers();
        ComponentSupport.ifNotInitializedThrowUninitializedComponentException(this);
        ComponentSupport.ifDestroyedThrowDestroyedComponentException(this);

        if (currentMatchers.isEmpty()) {
            return Collections.emptySet();
        }

        Set<AttributeValue> matchingValues = new LazySet<AttributeValue>();
        for (MatchFunctor matchFunctor : getComposedMatchers()) {
            matchingValues.addAll(matchFunctor.getMatchingValues(attribute, filterContext));
        }

        return Collections.unmodifiableSet(matchingValues);
    }
}