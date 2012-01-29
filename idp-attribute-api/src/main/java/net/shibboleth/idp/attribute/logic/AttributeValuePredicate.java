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
import net.shibboleth.utilities.java.support.logic.Assert;

import com.google.common.base.Predicate;

/** A {@link Predicate} that checks to see if one, or all, of the values of an attribute match a given predicate. */
public class AttributeValuePredicate implements Predicate<Attribute> {

    /** Predicate used to check individual attribute values. */
    private final Predicate valuePredicate;

    /** Whether all values must match the given value predicate or just one. */
    private final boolean allValuesMustMatch;

    /**
     * Constructor.
     * 
     * @param valueMatchingPredicate predicate used to check individual attribute values
     * @param allAttributeValuesMustMatch whether all values must match the given value predicate or just one
     */
    public AttributeValuePredicate(@Nonnull final Predicate valueMatchingPredicate,
            final boolean allAttributeValuesMustMatch) {
        valuePredicate =
                Assert.isNotNull(valueMatchingPredicate, "Attribute value matching predicate must not be null");
        allValuesMustMatch = allAttributeValuesMustMatch;
    }

    /** {@inheritDoc} */
    public boolean apply(@Nonnull Attribute attribute) {
        assert attribute != null : "Attribute can not be null";
        
        Boolean allValuesMatched = null;
        for (Object value : attribute.getValues()) {

            if (valuePredicate.apply(value)) {
                if (!allValuesMustMatch) {
                    return true;
                }

                if (allValuesMatched == null) {
                    allValuesMatched = Boolean.TRUE;
                }
            } else {
                if (allValuesMustMatch) {
                    return false;
                }
                allValuesMatched = Boolean.FALSE;
            }
        }

        if (allValuesMatched != null && allValuesMustMatch && allValuesMatched) {
            return true;
        }

        return false;
    }
}