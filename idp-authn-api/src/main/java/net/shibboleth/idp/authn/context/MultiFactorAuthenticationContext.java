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

package net.shibboleth.idp.authn.context;

import java.util.HashMap;
import java.util.Map;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import net.shibboleth.idp.authn.AuthenticationResult;
import net.shibboleth.idp.authn.MultiFactorAuthenticationTransition;
import net.shibboleth.utilities.java.support.annotation.constraint.Live;
import net.shibboleth.utilities.java.support.annotation.constraint.NonnullElements;
import net.shibboleth.utilities.java.support.logic.Constraint;
import net.shibboleth.utilities.java.support.primitive.StringSupport;

import org.opensaml.messaging.context.BaseContext;

/**
 * A context that holds information about the intermediate state of the multi-factor login flow.
 *
 * @parent {@link AuthenticationContext}
 * @added At the beginning of the multi-factor login flow
 * @removed At the end of the multi-factor login flow
 * 
 * @since 3.3.0
 */
public class MultiFactorAuthenticationContext extends BaseContext {

    /** Map of login "factors" (flows) and the transition rules to run after it. */
    @Nonnull @NonnullElements private Map<String,MultiFactorAuthenticationTransition> transitionMap;
    
    /** Authentication results that are active (may be generated earlier or during current request). */
    @Nonnull @NonnullElements private final Map<String,AuthenticationResult> activeResults;
    
    /** The result of merging the various individual results together. */
    @Nullable private AuthenticationResult mergedResult;
    
    /** Constructor. */
    public MultiFactorAuthenticationContext() {
        transitionMap = new HashMap<>();
        activeResults = new HashMap<>();
    }

    /**
     * Get a live map of the transitions to apply.
     * 
     * @return  map of transition logic
     */
    @Nonnull @NonnullElements @Live public Map<String,MultiFactorAuthenticationTransition> getTransitionMap() {
        return transitionMap;
    }
    
    /**
     * Set the map of transitions to apply, replacing any existing entries.
     * 
     * @param map map of transition logic
     * 
     * @return this context
     */
    @Nonnull public MultiFactorAuthenticationContext setTransitionMap(
            @Nonnull @NonnullElements final Map<String,MultiFactorAuthenticationTransition> map) {
        Constraint.isNotNull(map, "Map cannot be null");
        
        transitionMap.clear();
        for (final Map.Entry<String,MultiFactorAuthenticationTransition> entry : map.entrySet()) {
            final String trimmed = StringSupport.trimOrNull(entry.getKey());
            if (trimmed != null && entry.getValue() != null) {
                transitionMap.put(trimmed, entry.getValue());
            }
        }
        
        return this;
    }
    
    /**
     * Get a live list of the {@link AuthenticationResult} objects produced during the flow.
     * 
     * @return list of results
     */
    @Nonnull @NonnullElements @Live public Map<String,AuthenticationResult> getActiveResults() {
        return activeResults;
    }
    
    /**
     * Get a single merged {@link AuthenticationResult} produced by the overall flow.
     * 
     * @return merged result
     */
    @Nullable public AuthenticationResult getMergedAuthenticationResult() {
        return mergedResult;
    }
    
    /**
     * Set a single merged {@link AuthenticationResult} produced by the overall flow.
     * 
     * @param result merged result
     * 
     * @return this context
     */
    @Nonnull public MultiFactorAuthenticationContext setMergedAuthenticationResult(
            @Nullable final AuthenticationResult result) {
        mergedResult = result;
        
        return this;
    }
    
}