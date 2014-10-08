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

package net.shibboleth.idp.consent.logic;

import java.util.Collection;
import java.util.Set;
import java.util.regex.Pattern;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import net.shibboleth.idp.attribute.IdPAttribute;
import net.shibboleth.utilities.java.support.annotation.constraint.NonnullElements;
import net.shibboleth.utilities.java.support.annotation.constraint.NullableElements;
import net.shibboleth.utilities.java.support.primitive.StringSupport;

import com.google.common.base.Predicate;
import com.google.common.collect.Sets;

/**
 * Predicate to determine whether consent should be obtained for an attribute.
 */
public class AttributePredicate implements Predicate<IdPAttribute> {

    /** Whitelist of attribute IDs to allow. */
    @Nonnull @NonnullElements private Set<String> whitelistedAttributeIds;

    /** Blacklist of attribute IDs to deny. */
    @Nonnull @NonnullElements private Set<String> blacklistedAttributeIds;

    /** Regular expression to apply for acceptance testing. */
    @Nullable private Pattern expression;

    /**
     * 
     * Constructor.
     *
     * @param whitelist whitelisted attribute IDs
     * @param blacklist blacklisted attribute IDs
     * @param matchExpression attribute ID pattern
     */
    public AttributePredicate(@Nullable @NullableElements final Collection<String> whitelist,
            @Nullable @NullableElements final Collection<String> blacklist, @Nullable final Pattern matchExpression) {
        whitelistedAttributeIds = Sets.newHashSet(StringSupport.normalizeStringCollection(whitelist));
        blacklistedAttributeIds = Sets.newHashSet(StringSupport.normalizeStringCollection(blacklist));
        expression = matchExpression;
    }

    /** {@inheritDoc} */
    public boolean apply(@Nullable final IdPAttribute input) {

        final String attributeId = input.getId();

        if (!whitelistedAttributeIds.isEmpty() && !whitelistedAttributeIds.contains(attributeId)) {
            // Not in whitelist. Only accept if a regexp applies.
            if (expression == null) {
                return false;
            } else {
                return expression.matcher(attributeId).matches();
            }
        } else {
            // In whitelist (or none). Check blacklist, and if necessary a regexp.
            return !blacklistedAttributeIds.contains(attributeId)
                    && (expression == null || expression.matcher(attributeId).matches());
        }
    }
}
