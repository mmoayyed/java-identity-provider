/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
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

import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.opensaml.profile.context.ProfileRequestContext;

import net.shibboleth.shared.annotation.constraint.Live;
import net.shibboleth.shared.annotation.constraint.NotEmpty;
import net.shibboleth.shared.collection.CollectionSupport;
import net.shibboleth.shared.logic.Constraint;
import net.shibboleth.shared.logic.FunctionSupport;
import net.shibboleth.shared.primitive.LoggerFactory;
import net.shibboleth.shared.primitive.StringSupport;

/**
 * A ruleset for managing the transition out of a step during the multi-factor authn flow.
 * 
 * <p>After each step/flow is successfully completed, this object supplies rules for determining what to
 * do next with a bit of pseudo-SWF reinvention that allows an event to be mapped to a new flow to run by
 * means of a function. If no mapping exists, or the function returns null, then the active event is simply
 * raised as the result of the overall flow execution.</p>
 * 
 * <p>Note that raising the "proceed" event from a previous step will cause the MFA flow itself to attempt
 * successful completion by finalizing its result.</p>
 * 
 * @since 3.3.0
 */
public class MultiFactorAuthenticationTransition {

    /** A function that determines the next flow to execute. */
    @Nonnull private Map<String,Function<ProfileRequestContext,String>> nextFlowStrategyMap;
    
    /** Constructor. */
    public MultiFactorAuthenticationTransition() {
        nextFlowStrategyMap = new HashMap<>();
    }
    
    /**
     * Get the function to run to determine the next subflow to run.
     * 
     * @param event the event to transition from
     * 
     * @return flow determination strategy
     */
    @Nonnull public Function<ProfileRequestContext,String> getNextFlowStrategy(@Nonnull @NotEmpty final String event) {
        final Function<ProfileRequestContext,String>  result = nextFlowStrategyMap.get(event); 
        if (result != null) {
            return result;
        }
        return FunctionSupport.constant(null);
    }
    
    /**
     * Get the map of transition rules to follow.
     * 
     * @return a map of transition functions keyed by event ID
     */
    @Nonnull @Live Map<String,Function<ProfileRequestContext,String>> getNextFlowStrategyMap() {
        return nextFlowStrategyMap;
    }
    
    /**
     * Set the map of transition rules to follow.
     * 
     * <p>The values in the map must be either a {@link String} identifying the flow ID to run, or
     * a {@link Function}<code>&lt;</code>{@link ProfileRequestContext}<code>,</code>{@link String}<code>&gt;</code>
     * to execute.</p> 
     * 
     * @param map map of transition rules
     */
    @SuppressWarnings("unchecked")
    public void setNextFlowStrategyMap(@Nonnull final Map<String,Object> map) {
        Constraint.isNotNull(map, "Transition strategy map cannot be null");
        
        nextFlowStrategyMap.clear();
        for (final Map.Entry<String,Object> entry : map.entrySet()) {
            final String trimmed = StringSupport.trimOrNull(entry.getKey());
            if (trimmed != null) {
                if (entry.getValue() instanceof String) {
                    final String flowId = StringSupport.trimOrNull((String) entry.getValue());
                    if (flowId != null) {
                        nextFlowStrategyMap.put(trimmed,
                                FunctionSupport.<ProfileRequestContext,String>constant(flowId));
                    }
                } else if (entry.getValue() instanceof Function) {
                    nextFlowStrategyMap.put(trimmed, (Function<ProfileRequestContext, String>) entry.getValue());
                } else if (entry.getValue() != null) {
                    LoggerFactory.getLogger(MultiFactorAuthenticationTransition.class).warn(
                            "Ignoring mapping from {} to unsupported object of type {}", trimmed,
                            entry.getValue().getClass());
                }
            }
        }
    }
    
    /**
     * Set the next flow to run directly, instead of using a strategy map.
     * 
     * <p>The transition rule is implicitly based on a "proceed" event occurring,
     * and assumes no custom transitions for any other events.</p>

     * @param flowId fully-qualified flow ID to run
     */
    public void setNextFlow(@Nullable @NotEmpty final String flowId) {
        setNextFlowStrategyMap(CollectionSupport.singletonMap("proceed", flowId));
    }

    /**
     * Set a function to run directly instead of using a strategy map.
     * 
     * <p>The transition rule is implicitly based on a "proceed" event occurring,
     * and assumes no custom transitions for any other events.</p>

     * @param strategy function to run
     */
    public void setNextFlowStrategy(@Nonnull final Function<ProfileRequestContext,String> strategy) {
        Constraint.isNotNull(strategy, "Flow strategy function cannot be null");
        
        setNextFlowStrategyMap(CollectionSupport.singletonMap("proceed", strategy));
    }

}