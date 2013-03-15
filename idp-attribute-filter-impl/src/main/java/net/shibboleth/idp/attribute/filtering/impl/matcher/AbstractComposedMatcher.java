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

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import net.shibboleth.utilities.java.support.annotation.constraint.NonnullElements;
import net.shibboleth.utilities.java.support.annotation.constraint.NullableElements;
import net.shibboleth.utilities.java.support.annotation.constraint.Unmodifiable;
import net.shibboleth.utilities.java.support.collection.CollectionSupport;
import net.shibboleth.utilities.java.support.component.AbstractDestructableInitializableComponent;
import net.shibboleth.utilities.java.support.component.ComponentInitializationException;
import net.shibboleth.utilities.java.support.component.ComponentSupport;
import net.shibboleth.utilities.java.support.component.ComponentValidationException;
import net.shibboleth.utilities.java.support.component.UnmodifiableComponent;
import net.shibboleth.utilities.java.support.component.ValidatableComponent;

import com.google.common.base.Predicates;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;

/**
 * Base class for {@link MatchFunctor} implementations that are compositions of other
 * {@link MatchFunctor}.
 */
public abstract class AbstractComposedMatcher extends AbstractDestructableInitializableComponent implements
        MatchFunctor, UnmodifiableComponent, ValidatableComponent {

    /** The composed matchers. */
    private final List<MatchFunctor> matchers;

    /**
     * Constructor.
     * 
     * @param composedMatchers matchers being composed
     */
    public AbstractComposedMatcher(@Nullable @NullableElements final 
            Collection<MatchFunctor> composedMatchers) {
        ArrayList<MatchFunctor> checkedMatchers = new ArrayList<MatchFunctor>();

        if (composedMatchers != null) {
            CollectionSupport.addIf(checkedMatchers, composedMatchers, Predicates.notNull());
        }

        matchers = ImmutableList.copyOf(Iterables.filter(checkedMatchers, Predicates.notNull()));
    }

    /**
     * Get the composed matchers.
     * 
     * @return the composed matchers
     */
    @Nonnull @NonnullElements @Unmodifiable public List<MatchFunctor> getComposedMatchers() {
        return matchers;
    }

    /** {@inheritDoc} */
    public void validate() throws ComponentValidationException {
        if (!isInitialized()) {
            throw new ComponentValidationException("Matcher not initialized");
        }

        if (isDestroyed()) {
            throw new ComponentValidationException("Matcher has been destroyed");
        }

        for (MatchFunctor matcher : matchers) {
            ComponentSupport.validate(matcher);
        }
    }

    /** {@inheritDoc} */
    protected void doDestroy() {
        for (MatchFunctor matcher : matchers) {
            ComponentSupport.destroy(matcher);
        }

        super.doDestroy();
    }

    /** {@inheritDoc} */
    protected void doInitialize() throws ComponentInitializationException {
        super.doInitialize();

        for (MatchFunctor matcher : matchers) {
            ComponentSupport.initialize(matcher);
        }
    }
}