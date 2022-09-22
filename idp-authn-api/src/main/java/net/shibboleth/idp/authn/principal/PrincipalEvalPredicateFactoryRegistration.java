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

package net.shibboleth.idp.authn.principal;

import java.security.Principal;

import javax.annotation.Nonnull;

import net.shibboleth.shared.annotation.ParameterName;
import net.shibboleth.utilities.java.support.collection.Pair;
import net.shibboleth.utilities.java.support.logic.Constraint;

/**
 * Wraps the association of a {@link PrincipalEvalPredicateFactory} against a particular
 * {@link Principal} subtype and a string operator.
 * 
 * <p>Used to support auto-wiring of factories into a
 * {@link PrincipalEvalPredicateFactoryRegistry}.</p>
 */
public class PrincipalEvalPredicateFactoryRegistration {

    /** The class and operator pair. */
    @Nonnull private Pair<Class<? extends Principal>,String> typeAndOperator;
    
    /** Predicate factory. */
    @Nonnull private PrincipalEvalPredicateFactory predicateFactory;
    
    /**
     * Constructor.
     *
     * @param key type and operator
     * @param value predicate factory
     */
    public PrincipalEvalPredicateFactoryRegistration(
            @Nonnull @ParameterName(name="key") final Pair<Class<? extends Principal>, String> key,
            @Nonnull @ParameterName(name="value") final PrincipalEvalPredicateFactory value) {
        typeAndOperator = Constraint.isNotNull(key, "Type/operator pair cannot be null");
        predicateFactory = Constraint.isNotNull(value, "PrincipalEvalPredicateFactory cannot be null");
    }

    /**
     * Gets the type and operator pair for this registration.
     * 
     * @return type and operator pair
     */
    @Nonnull public Pair<Class<? extends Principal>,String> getTypeAndOperator() {
        return typeAndOperator;
    }
    
    /**
     * Gets the factory for this registration.
     * 
     * @return registration
     */
    @Nonnull public PrincipalEvalPredicateFactory getPredicateFactory() {
        return predicateFactory;
    }
    
}