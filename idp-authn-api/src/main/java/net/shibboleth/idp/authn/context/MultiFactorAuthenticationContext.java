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

import net.shibboleth.idp.authn.AuthenticationFlowDescriptor;
import net.shibboleth.idp.authn.AuthenticationResult;
import net.shibboleth.idp.authn.MultiFactorAuthenticationTransition;
import net.shibboleth.utilities.java.support.annotation.constraint.Live;
import net.shibboleth.utilities.java.support.annotation.constraint.NonnullElements;
import net.shibboleth.utilities.java.support.annotation.constraint.NotEmpty;
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

    /** Map of login "factors" (flows) and the transition rules to run after them. */
    @Nonnull @NonnullElements private Map<String,MultiFactorAuthenticationTransition> transitionMap;
    
    /** Authentication results that are active (may be generated earlier or during current request). */
    @Nonnull @NonnullElements private final Map<String,AuthenticationResult> activeResults;

    /** Login flow descriptor for the MFA flow. */
    @Nullable private AuthenticationFlowDescriptor mfaFlowDescriptor;
    
    /** The next flow due to execute (or the currently executing flow during subflow execution). */
    @Nullable @NotEmpty private String nextFlowId;
    
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
            @Nonnull final Map<String,MultiFactorAuthenticationTransition> map) {
        Constraint.isNotNull(map, "Map cannot be null");
        
        transitionMap.clear();
        for (final Map.Entry<String,MultiFactorAuthenticationTransition> entry : map.entrySet()) {
            final String trimmed = StringSupport.trimOrNull(entry.getKey());
            if (entry.getValue() != null) {
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
     * Get the {@link AuthenticationFlowDescriptor} representing the MFA flow.
     * 
     * @return descriptor
     */
    @Nullable public AuthenticationFlowDescriptor getAuthenticationFlowDescriptor() {
        return mfaFlowDescriptor;
    }

    /**
     * Set the {@link AuthenticationFlowDescriptor} representing the MFA flow.
     * 
     * @param descriptor login flow descriptor
     * 
     * @return this context
     */
    @Nonnull public MultiFactorAuthenticationContext setAuthenticationFlowDescriptor(
            @Nullable final AuthenticationFlowDescriptor descriptor) {
        mfaFlowDescriptor = descriptor;
        
        return this;
    }

    /**
     * Get the next flow due to execute (or that is currently executing).
     * 
     * @return  the ID of the next flow to execute
     */
    @Nullable @NotEmpty public String getNextFlowId() {
        return nextFlowId;
    }
    
    /**
     * Set the next flow due to execute.
     * 
     * @param id flow ID
     * 
     * @return this context
     */
    @Nonnull public MultiFactorAuthenticationContext setNextFlowId(@Nullable @NotEmpty final String id) {
        nextFlowId = StringSupport.trimOrNull(id);
        
        return this;
    }

}