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

package net.shibboleth.idp.attribute.filtering;

import java.util.Collections;
import java.util.Set;

import javax.annotation.Nonnull;
import javax.annotation.concurrent.ThreadSafe;

import net.shibboleth.idp.attribute.Attribute;
import net.shibboleth.idp.attribute.AttributeValue;
import net.shibboleth.utilities.java.support.annotation.constraint.NonnullElements;
import net.shibboleth.utilities.java.support.annotation.constraint.Unmodifiable;

/**
 * A function that gets the values of an attribute that meets this matchers requirements.
 * 
 * <p>
 * Implementations of this interface <strong>MUST</strong> implement appropriate {@link Object#equals(Object)} and
 * {@link Object#hashCode()} methods.
 * </p>
 */
@ThreadSafe
public interface AttributeValueMatcher {

    /** A {@link AttributeValueMatcher} that returns all attribute values as matched. */
    public static final AttributeValueMatcher MATCHES_ALL = new AttributeValueMatcher() {

        /** {@inheritDoc} */
        public Set<AttributeValue> getMatchingValues(Attribute attribute, AttributeFilterContext filterContext)
                throws AttributeFilteringException {
            return attribute.getValues();
        }
    };

    /** A {@link AttributeValueMatcher} that returns no attribute values as matched. */
    public static final AttributeValueMatcher MATCHES_NONE = new AttributeValueMatcher() {

        /** {@inheritDoc} */
        public Set<AttributeValue> getMatchingValues(Attribute attribute, AttributeFilterContext filterContext)
                throws AttributeFilteringException {
            return Collections.emptySet();
        }
    };

    /**
     * Determines the values, for the given attribute, that satisfies the requirements of this rule. Note, the value set
     * of the given attribute <strong>MUST NOT</strong> be altered by implementations of this method.
     * 
     * @param attribute attribute whose values will be filtered
     * @param filterContext current filter request
     * 
     * @return attribute values that satisfy this rule
     * 
     * @throws AttributeFilteringException thrown is there is a problem evaluating one or more attribute values against
     *             this rule's criteria
     */
    @Nonnull @NonnullElements @Unmodifiable public Set<AttributeValue> getMatchingValues(
            @Nonnull final Attribute attribute, @Nonnull final AttributeFilterContext filterContext)
            throws AttributeFilteringException;

}