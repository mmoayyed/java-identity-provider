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

import net.shibboleth.idp.attribute.StringAttributeValue;
import net.shibboleth.utilities.java.support.logic.Constraint;

import com.google.common.base.Predicate;

/**
 * A predicate that supports the evaluation of a {@link Predicate} that supports {@link String} objects
 * {@link StringAttributeValue} attribute values.
 */
public class StringAttributeValuePredicate implements Predicate {

    /** Predicate used to match the attribute value. */
    private final Predicate<CharSequence> valuePredicate;

    /**
     * Constructor.
     * 
     * @param valueMatchingPredicate the predicate used match the attribute values
     */
    public StringAttributeValuePredicate(@Nonnull final Predicate<CharSequence> valueMatchingPredicate) {
        valuePredicate =
                Constraint.isNotNull(valueMatchingPredicate,
                        "String attribute value matching predicate can not be null");
    }

    /** {@inheritDoc} */
    public boolean apply(@Nonnull Object input) {
        Constraint.isNotNull(input, "Input can not be null");

        if (input instanceof StringAttributeValue) {
            return valuePredicate.apply(((StringAttributeValue) input).getValue());
        }

        throw new IllegalArgumentException("This predicate only supports values of type " + String.class.getName()
                + " and " + StringAttributeValue.class.getName() + " not " + input.getClass().getName());
    }
}