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

import java.util.HashSet;
import java.util.Set;

import javax.annotation.Nonnull;

import net.shibboleth.idp.attribute.Attribute;
import net.shibboleth.idp.attribute.AttributeValue;
import net.shibboleth.idp.attribute.filtering.AttributeFilterContext;
import net.shibboleth.idp.attribute.filtering.AttributeFilteringException;
import net.shibboleth.idp.attribute.filtering.AttributeValueMatcher;
import net.shibboleth.utilities.java.support.logic.Assert;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Objects;
import com.google.common.base.Predicate;

/** A matcher that applies a {@link Predicate} to each attribute value to determine if its a match. */
public class AttributeValuePredicateMatcher implements AttributeValueMatcher {

    /** Class logger. */
    private final Logger log = LoggerFactory.getLogger(AttributeValuePredicateMatcher.class);

    /** Predicate used to check attribute values. */
    private final Predicate valuePredicate;

    /**
     * Constructor.
     * 
     * @param valueMatchingPredicate predicate used to check attribute values
     */
    public AttributeValuePredicateMatcher(@Nonnull Predicate valueMatchingPredicate) {
        valuePredicate = Assert.isNotNull(valueMatchingPredicate, "Attribute value matching predicate can not be null");
    }

    /** {@inheritDoc} */
    public Set<AttributeValue> getMatchingValues(@Nonnull Attribute attribute,
            @Nonnull AttributeFilterContext filterContext) throws AttributeFilteringException {
        assert attribute != null : "Attribute to be filtered can not be null";
        assert filterContext != null : "Attribute filter contet can not be null";

        HashSet matchedValues = new HashSet();

        for (Object value : attribute.getValues()) {
            try {
                if (valuePredicate.apply(value)) {
                    matchedValues.add(value);
                }
            } catch (Exception e) {
                // TODO RDW Work out how to provoke this path when nothing inside throws an exception, just errors
                log.debug("Attribute value '{}' of type '{}' caused an error while being evaluated '{}':\n{}",
                        new Object[] {value, value.getClass().getName(), valuePredicate.getClass().getName(), e});
                throw new AttributeFilteringException("Unable to apply predicate to attribute value", e);
            }
        }

        return matchedValues;
    }

    /** {@inheritDoc} */
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }

        if (obj == this) {
            return true;
        }

        if (!(obj instanceof AttributeValuePredicateMatcher)) {
            return false;
        }

        return Objects.equal(valuePredicate, ((AttributeValuePredicateMatcher) obj).valuePredicate);
    }

    /** {@inheritDoc} */
    public int hashCode() {
        return valuePredicate.hashCode();
    }

    /** {@inheritDoc} */
    public String toString() {
        return Objects.toStringHelper(this).add("valuePredicate", valuePredicate).toString();
    }
}