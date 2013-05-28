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

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import javax.annotation.Nonnull;
import javax.annotation.concurrent.ThreadSafe;

import net.shibboleth.idp.attribute.Attribute;
import net.shibboleth.idp.attribute.AttributeValue;
import net.shibboleth.idp.attribute.filter.AttributeFilterContext;
import net.shibboleth.idp.attribute.filter.AttributeFilterException;
import net.shibboleth.idp.attribute.filter.MatchFunctor;
import net.shibboleth.utilities.java.support.component.AbstractDestructableIdentifiableInitializableComponent;
import net.shibboleth.utilities.java.support.component.ComponentInitializationException;
import net.shibboleth.utilities.java.support.component.ComponentSupport;
import net.shibboleth.utilities.java.support.component.ComponentValidationException;
import net.shibboleth.utilities.java.support.component.UninitializedComponentException;
import net.shibboleth.utilities.java.support.logic.Constraint;

import com.google.common.base.Objects;

/**
 * {@link MatchFunctor} that implements the negation of a matcher. <br/>
 * <br/>
 * A given attribute value is considered to have matched if it is not returned by the composed {@link MatchFunctor}. The
 * predicate is the logical NOT of the composed {@link MatchFunctor}.
 */
@ThreadSafe
public final class NotMatcher extends AbstractDestructableIdentifiableInitializableComponent implements MatchFunctor {

    /** The matcher we are negating. */
    private MatchFunctor negatedMatcher;

    /**
     * Constructor.
     * 
     * @param valueMatcher attribute value matcher to be negated
     */
    public NotMatcher(@Nonnull final MatchFunctor valueMatcher) {
        negatedMatcher = Constraint.isNotNull(valueMatcher, "Attribute value matcher can not be null");
    }

    /**
     * Get the matcher that is being negated.
     * 
     * @return matcher that is being negated
     */
    public MatchFunctor getNegtedMatcher() {
        return negatedMatcher;
    }

    /**
     * {@inheritDoc} The result is the logical NOT of the composed {@link MatchFunctor}.
     */

    public boolean evaluatePolicyRule(@Nonnull AttributeFilterContext filterContext) 
            throws AttributeFilterException {
        return !negatedMatcher.evaluatePolicyRule(filterContext);
    }

    /**
     * A given attribute value is considered to have matched if it is not returned by the composed {@link MatchFunctor}.
     * {@inheritDoc}
     */
    public Set<AttributeValue> getMatchingValues(@Nonnull final Attribute attribute,
            @Nonnull final AttributeFilterContext filterContext) throws AttributeFilterException {
        Constraint.isNotNull(attribute, "Attribute to be filtered can not be null");
        Constraint.isNotNull(filterContext, "Attribute filter context can not be null");

        // Capture the matchers to avoid race with setComposedMatchers
        // Do this before the test on destruction to avoid race with destroy code
        final MatchFunctor currentMatcher = getNegtedMatcher();
        ComponentSupport.ifNotInitializedThrowUninitializedComponentException(this);
        ComponentSupport.ifDestroyedThrowDestroyedComponentException(this);

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
        super.doDestroy();
    }

    /** {@inheritDoc} */
    protected void doInitialize() throws ComponentInitializationException {
        super.doInitialize();

        ComponentSupport.initialize(negatedMatcher);
    }
    /** {@inheritDoc} */
    public void setId(String id) {
        super.setId(id);
    }


}