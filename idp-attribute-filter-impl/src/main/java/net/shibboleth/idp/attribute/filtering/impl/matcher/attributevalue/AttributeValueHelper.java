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

package net.shibboleth.idp.attribute.filtering.impl.matcher.attributevalue;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import net.shibboleth.idp.attribute.Attribute;
import net.shibboleth.idp.attribute.AttributeValue;
import net.shibboleth.idp.attribute.filtering.AttributeFilterContext;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Predicate;

/**
 * Helper methods for implementing the {@link Predicate<AttributeFilterContext>} part of the MatchFunctor.
 */
public final class AttributeValueHelper {

    /** log. */
    private static Logger log = LoggerFactory.getLogger(AttributeValueHelper.class);

    /** (hidden) Constructor. */
    private AttributeValueHelper() {

    }

    /**
     * Helper method to convert an un-targeted {@link AttributeValue} matchers into {@link Predicate
     * <AttributeFilterContext>}. We look up the attribute whose name is supplied and compare against the values.
     * 
     * @param valueComparator The class we are providing the helper function to. We will use its implementation of
     *            comparison to apply the rule described above.
     * @param attributeId the attribute ID.
     * @return whether the rule holds or not.
     */
    protected static Predicate<AttributeFilterContext> filterContextPredicate(
            @Nonnull final TargetedMatchFunctor valueComparator, @Nonnull final String attributeId) {

        return new Predicate<AttributeFilterContext>() {

            public boolean apply(@Nullable AttributeFilterContext context) {
                final Attribute attribute = context.getPrefilteredAttributes().get(attributeId);

                if (null == attribute) {
                    // TODO logging
                    log.info("No attribute available");
                    return false;
                }

                for (AttributeValue value : attribute.getValues()) {
                    if (valueComparator.compareAttributeValue(value)) {
                        return true;
                    }
                }
                return false;
            }
        };
    }

    /**
     * Helper method to convert an un-targeted {@link AttributeValue} matchers into {@link Predicate
     * <AttributeFilterContext>}. We take the rule that if an matcher is true for any attribute value in the input set
     * then the predicate is true. This function applies this rule in such a way that it can be plugged into various
     * implementations.
     * 
     * @param valueComparator The class we are providing the helper function to. We will use its implementation of
     *            comparison to apply the rule described above.
     * @return whether the rule holds or not.
     */
    protected static Predicate<AttributeFilterContext> filterContextPredicate(
            @Nonnull final TargetedMatchFunctor valueComparator) {

        return new Predicate<AttributeFilterContext>() {

            public boolean apply(@Nullable AttributeFilterContext context) {
                for (Attribute attribute : context.getPrefilteredAttributes().values()) {

                    if (null == attribute) {
                        // TODO logging
                        log.info("No attribute available");
                        continue;
                    }

                    for (AttributeValue value : attribute.getValues()) {
                        if (valueComparator.compareAttributeValue(value)) {
                            return true;
                        }
                    }
                }
                return false;
            }
        };
    }

}
