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

package net.shibboleth.idp.authn;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.shibboleth.utilities.java.support.annotation.constraint.NonnullElements;
import net.shibboleth.utilities.java.support.annotation.constraint.NotEmpty;
import net.shibboleth.utilities.java.support.logic.Constraint;
import net.shibboleth.utilities.java.support.primitive.StringSupport;

/**
 * A registry of mappings between a type of matching operator and a corresponding
 * {@link PrincipalEvalPredicateFactory} that returns predicates enforcing
 * a particular set of matching rules for that operator.
 */
public final class PrincipalEvalPredicateFactoryRegistry {

    /** Class logger. */
    private final Logger log = LoggerFactory.getLogger(PrincipalEvalPredicateFactoryRegistry.class);
    
    /** Storage for the registry mappings. */
    private Map<String, PrincipalEvalPredicateFactory> registry;

    /** Constructor. */
    public PrincipalEvalPredicateFactoryRegistry() {
        registry = new ConcurrentHashMap();
    }
    
    /**
     * Constructor.
     * 
     * @param fromMap  map to populate registry with
     */
    public PrincipalEvalPredicateFactoryRegistry(
            @Nonnull @NonnullElements Map<String, PrincipalEvalPredicateFactory> fromMap) {
        registry = new ConcurrentHashMap(Constraint.isNotNull(fromMap, "Source map cannot be null"));
    }
    
    /**
     * Get a registered predicate factory for a given operator string, if any.
     * 
     * @param operator  an operator string
     * @return a corresponding predicate factory, or null
     */
    @Nullable public PrincipalEvalPredicateFactory lookup(@Nonnull @NotEmpty final String operator) {
        String trimmed = Constraint.isNotNull(StringSupport.trimOrNull(operator), "Operator cannot be null or empty");
        
        PrincipalEvalPredicateFactory factory = registry.get(trimmed);
        if (factory != null) {
            log.debug("Registry located predicate factory of type {} for operator {}", factory.getClass().getName(),
                    trimmed);
            return factory;
        } else {
            log.debug("Registry failed to locate predicate factory for operator {}", trimmed);
            return null;
        }
    }
    
    /**
     * Register a predicate factory for a given operator string.
     * 
     * @param operator  an operator string
     * @param factory   the predicate factory to register
     */
    public void register(@Nonnull @NotEmpty final String operator,
            @Nonnull PrincipalEvalPredicateFactory factory) {
        String trimmed = Constraint.isNotNull(StringSupport.trimOrNull(operator), "Operator cannot be null or empty");
        Constraint.isNotNull(factory, "PrincipalEvalPredicateFactory cannot be null");
        
        log.debug("Registering predicate factory of type {} for operator {}", factory.getClass().getName(), operator);
        registry.put(trimmed, factory);
    }
    
    /**
     * Deregister a predicate factory for a given operator string.
     * 
     * @param operator  an operator string
     */
    public void deregister(@Nonnull @NotEmpty final String operator) {
        String trimmed = Constraint.isNotNull(StringSupport.trimOrNull(operator), "Operator cannot be null or empty");
        
        log.debug("Deregistering predicate factory for operator {}", operator);
        registry.remove(trimmed);
    }
}