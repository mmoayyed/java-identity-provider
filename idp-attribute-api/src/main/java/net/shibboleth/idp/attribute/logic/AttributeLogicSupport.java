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

package net.shibboleth.idp.attribute.logic;

import javax.annotation.Nonnull;

import net.shibboleth.idp.attribute.Attribute;

import org.opensaml.messaging.context.BaseContext;

import com.google.common.base.Function;
import com.google.common.base.Predicate;

/**
 * Support class for attribute-related {@link Function} and {@link Predicate} implementations. The static methods herein
 * can be useful, when used in conjunction with static imports, for creating an internal DSL.
 */
public final class AttributeLogicSupport {

    /** Constructor. */
    private AttributeLogicSupport() {
    }

    /**
     * Creates a function that looks up an attribute, identified by the given ID, from a {@link BaseContext}.
     * 
     * @param attributeId the identifier attribute to be looked up
     * 
     * @return the function
     */
    @Nonnull public static Function<BaseContext, Attribute> lookupAttributeFromAttributeContext(
            @Nonnull String attributeId) {
        return new LookupAttributeFromAttributeContextFunction(attributeId);
    }

    /**
     * Creates a predicate that ensures that all values for a given attribute meet the requirements of a given
     * predicate.
     * 
     * @param valueMatchingPredicate predicate applied to all attribute values
     * 
     * @return the created predicate
     */
    @Nonnull public static Predicate<Attribute> allAttributeValuesMatches(@Nonnull Predicate valueMatchingPredicate) {
        return new AttributeValuePredicate(valueMatchingPredicate, true);
    }

    /**
     * Creates a predicate that ensures that at least one value for a given attribute meets the requirements of a given
     * predicate.
     * 
     * @param valueMatchingPredicate predicate applied to attribute values
     * 
     * @return the created predicate
     */
    @Nonnull public static Predicate<Attribute> atLeastOneAttributeValueMatches(
            @Nonnull Predicate valueMatchingPredicate) {
        return new AttributeValuePredicate(valueMatchingPredicate, false);
    }

    /**
     * Creates a predicate that ensures that the scope of a
     * {@link net.shibboleth.idp.attribute.ScopedStringAttributeValue} meets the requirement of a given predicate.
     * 
     * @param valueMatchingPredicate predicate applied to attribute values
     * 
     * @return the created predicate
     */
    @Nonnull public static Predicate scopedStringAttributeValueScopeMatches(
            @Nonnull Predicate<CharSequence> valueMatchingPredicate) {
        return new ScopedStringAttributeValueScopePredicate(valueMatchingPredicate);
    }

    /**
     * Creates a predicate that ensures that a {@link String} value or the value component of a
     * {@link net.shibboleth.idp.attribute.StringAttributeValue} meets the requirement of a given predicate.
     * 
     * @param valueMatchingPredicate predicate applied to attribute values
     * 
     * @return the created predicate
     */
    @Nonnull public static Predicate
            stringAttributeValueMatches(@Nonnull Predicate<CharSequence> valueMatchingPredicate) {
        return new StringAttributeValuePredicate(valueMatchingPredicate);
    }
}