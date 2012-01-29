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

import java.util.Collections;
import java.util.List;
import java.util.Set;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import net.shibboleth.idp.attribute.filtering.AttributeValueMatcher;
import net.shibboleth.utilities.java.support.annotation.constraint.NonnullElements;
import net.shibboleth.utilities.java.support.annotation.constraint.NullableElements;
import net.shibboleth.utilities.java.support.collection.TransformedInputCollectionBuilder;
import net.shibboleth.utilities.java.support.component.AbstractDestructableInitializableComponent;
import net.shibboleth.utilities.java.support.component.ComponentInitializationException;
import net.shibboleth.utilities.java.support.component.ComponentSupport;
import net.shibboleth.utilities.java.support.component.ComponentValidationException;
import net.shibboleth.utilities.java.support.component.UnmodifiableComponent;
import net.shibboleth.utilities.java.support.component.ValidatableComponent;

/**
 * Base class for {@link AttributeValueMatcher} implementations that are compositions of other
 * {@link AttributeValueMatcher}.
 */
public abstract class AbstractComposedMatcher extends AbstractDestructableInitializableComponent implements
        AttributeValueMatcher, UnmodifiableComponent, ValidatableComponent {

    /** The composed matchers. */
    private Set<AttributeValueMatcher> matchers = Collections.emptySet();

    /**
     * Get the composed matchers.
     * 
     * @return the composed matchers
     */
    @Nonnull @NonnullElements public Set<AttributeValueMatcher> getComposedMatchers() {
        return matchers;
    }

    /**
     * Set the composed matchers.
     * 
     * @param composedMatchers the composed matchers
     */
    public synchronized void setComposedMatchers(
            @Nullable @NullableElements final List<AttributeValueMatcher> composedMatchers) {
        ifInitializedThrowUnmodifiabledComponentException();
        ifDestroyedThrowDestroyedComponentException();

        matchers =
                new TransformedInputCollectionBuilder<AttributeValueMatcher>().addAll(composedMatchers)
                        .buildImmutableSet();
    }

    /** {@inheritDoc} */
    public void validate() throws ComponentValidationException {
        if (!isInitialized()) {
            throw new ComponentValidationException("Matcher not initialized");
        }

        if (isDestroyed()) {
            throw new ComponentValidationException("Matcher has been destroyed");
        }

        for (AttributeValueMatcher matcher : matchers) {
            ComponentSupport.validate(matcher);
        }
    }

    /** {@inheritDoc} */
    protected void doDestroy() {
        for (AttributeValueMatcher matcher : matchers) {
            ComponentSupport.destroy(matcher);
        }
        // Clear after the setting of the flag top avoid race with getMatchingValues
        matchers = Collections.emptySet();

        super.doDestroy();
    }

    /** {@inheritDoc} */
    protected void doInitialize() throws ComponentInitializationException {
        super.doInitialize();

        for (AttributeValueMatcher matcher : matchers) {
            ComponentSupport.initialize(matcher);
        }
    }
}
