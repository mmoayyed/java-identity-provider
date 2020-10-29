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

package net.shibboleth.idp.consent.logic.impl;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Pattern;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import net.shibboleth.idp.attribute.EmptyAttributeValue;
import net.shibboleth.idp.attribute.IdPAttribute;
import net.shibboleth.idp.attribute.IdPAttributeValue;
import net.shibboleth.utilities.java.support.annotation.constraint.NonnullElements;
import net.shibboleth.utilities.java.support.component.AbstractInitializableComponent;
import net.shibboleth.utilities.java.support.component.ComponentSupport;
import net.shibboleth.utilities.java.support.logic.Predicate;
import net.shibboleth.utilities.java.support.primitive.StringSupport;

/**
 * Predicate to determine whether consent should be obtained for an attribute.
 */
public class AttributePredicate extends AbstractInitializableComponent implements Predicate<IdPAttribute> {

    /** Set of attribute IDs for which to prompt for consent. */
    @Nonnull @NonnullElements private Set<String> promptedAttributeIds;

    /** Set of attribute IDs to ignore for consent. */
    @Nonnull @NonnullElements private Set<String> ignoredAttributeIds;

    /** Regular expression to apply for acceptance testing. */
    @Nullable private Pattern matchExpression;

    /** Constructor. */
    public AttributePredicate() {
        promptedAttributeIds = Collections.emptySet();
        ignoredAttributeIds = Collections.emptySet();
    }

    /**
     * Set the attribute IDs for which to prompt for consent.
     * 
     * @param prompted prompted attribute IDs
     */
    public void setPromptedAttributeIds(@Nullable @NonnullElements final Collection<String> prompted) {
        ComponentSupport.ifInitializedThrowUnmodifiabledComponentException(this);

        promptedAttributeIds = new HashSet<>(StringSupport.normalizeStringCollection(prompted));
    }

    /**
     * Set the attribute IDs to ignore for consent.
     * 
     * @param ignored ignored attribute IDs
     */
    public void setIgnoredAttributeIds(@Nullable @NonnullElements final Collection<String> ignored) {
        ComponentSupport.ifInitializedThrowUnmodifiabledComponentException(this);

        ignoredAttributeIds = new HashSet<>(StringSupport.normalizeStringCollection(ignored));
    }

    /**
     * Set an attribute ID matching expression to apply for acceptance.
     * 
     * @param expression an attribute ID matching expression
     */
    public void setAttributeIdMatchExpression(@Nullable final Pattern expression) {
        ComponentSupport.ifInitializedThrowUnmodifiabledComponentException(this);

        if (expression != null && !expression.pattern().isEmpty()) {
            matchExpression = expression;
        } else {
            matchExpression = null;
        }
    }

    /** {@inheritDoc} */
    public boolean test(@Nullable final IdPAttribute input) {

        if (input == null) {
            return false;
        }

        if (isEmpty(input)) {
            return false;
        }

        final String attributeId = input.getId();

        if (!promptedAttributeIds.isEmpty() && !promptedAttributeIds.contains(attributeId)) {
            // Not in prompted set. Only prompt if a regexp applies.
            if (matchExpression == null) {
                return false;
            }
            return matchExpression.matcher(attributeId).matches();
        }
        
        // In prompted set (or none). Check unprompted set, and if necessary a regexp.
        return !ignoredAttributeIds.contains(attributeId)
                && (matchExpression == null || matchExpression.matcher(attributeId).matches());
    }

    /**
     * Whether the IdP attribute is empty.
     * 
     * @param input the IdP Attribute
     * @return true if the IdP attribute has no values or empty values, false otherwise
     */
    private boolean isEmpty(@Nonnull final IdPAttribute input) {

        for (final IdPAttributeValue value : input.getValues()) {
            if (!(value instanceof EmptyAttributeValue)) {
                return false;
            }
        }

        return true;
    }
}
