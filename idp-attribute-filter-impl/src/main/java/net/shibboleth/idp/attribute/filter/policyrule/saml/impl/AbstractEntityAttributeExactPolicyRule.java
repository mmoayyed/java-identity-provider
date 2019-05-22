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

package net.shibboleth.idp.attribute.filter.policyrule.saml.impl;

import java.util.Set;

import javax.annotation.Nonnull;

import net.shibboleth.utilities.java.support.annotation.constraint.NonnullAfterInit;
import net.shibboleth.utilities.java.support.annotation.constraint.NonnullElements;
import net.shibboleth.utilities.java.support.annotation.constraint.NotEmpty;
import net.shibboleth.utilities.java.support.component.ComponentInitializationException;
import net.shibboleth.utilities.java.support.logic.Constraint;

/**
 * Matcher that checks, via an exact match, for an entity attribute with a given value.
 */
public abstract class AbstractEntityAttributeExactPolicyRule extends AbstractEntityAttributePolicyRule {

    /** The value of the entity attribute the entity must have. */
    @NonnullAfterInit @NotEmpty private String value;

    /**
     * Gets the value of the entity attribute the entity must have.
     * 
     * @return value of the entity attribute the entity must have
     */
    @NonnullAfterInit @NotEmpty public String getValue() {
        return value;
    }

    /**
     * Sets the value of the entity attribute the entity must have.
     * 
     * @param attributeValue value of the entity attribute the entity must have
     */
    public void setValue(@Nonnull @NotEmpty final String attributeValue) {
        value = Constraint.isNotNull(attributeValue, "Attribute value cannot be null.");
    }

    /** {@inheritDoc} */
    @Override
    protected void doInitialize() throws ComponentInitializationException {
        super.doInitialize();
        if (null == value) {
            throw new ComponentInitializationException(getLogPrefix() + " No value supplied to compare against");
        }
    }

    /** {@inheritDoc} */
    @Override
    protected boolean entityAttributeValueMatches(
            @Nonnull @NotEmpty @NonnullElements final Set<String> entityAttributeValues) {
        return entityAttributeValues.contains(value);
    }

}