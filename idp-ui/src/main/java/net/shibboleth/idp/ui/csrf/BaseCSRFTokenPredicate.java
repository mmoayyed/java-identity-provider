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

package net.shibboleth.idp.ui.csrf;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.springframework.webflow.execution.RequestContext;

import net.shibboleth.utilities.java.support.annotation.constraint.NonnullElements;
import net.shibboleth.utilities.java.support.annotation.constraint.NotEmpty;
import net.shibboleth.utilities.java.support.primitive.StringSupport;

/**
 * A base class for predicates that determine if CSRF protection is required/enabled. Stores the
 * state IDs for view-states that have been either included and or excluded from CSRF 
 * protection. Also contains the logic to test whether a request context state 
 * has been included or excluded.
 */
public abstract class BaseCSRFTokenPredicate {
    
    /** The character value of the wildcard used to indicate all views should be included.*/
    @Nonnull @NotEmpty public static final String INCLUDE_ALL_WILDCARD = "*";
    
    /** View-state IDs that are excluded from CSRF protection.*/
    @Nonnull @NonnullElements private Set<String> excludedViewStateIds;
    
    /** View-state IDs that require CSRF protection. */
    @Nonnull @NonnullElements private Set<String> includedViewStateIds;
    
    /** Are all view-states included? if so, includedViewstateIds can be ignored. Default is false.*/
    @Nonnull private boolean includeAllViewStates;    
    
    /** Constructor.*/
    public BaseCSRFTokenPredicate() {
        excludedViewStateIds = Collections.emptySet();        
        includedViewStateIds = Collections.emptySet();
        includeAllViewStates = false;
    }
    
    /**
     * Set the excluded view-state IDs.
     * 
     * @param excludelist excluded view-state IDs.
     */
    public void setExcludedViewStateIds(@Nullable @NonnullElements final Collection<String> excludelist) {       
        excludedViewStateIds = new HashSet<>(StringSupport.normalizeStringCollection(excludelist));
    }
    
    /**
     * Set the included view-state IDs. If a wildcard is present, <code>includeAllViews</code> is set
     * to true and <code>includedViewstateIds</code> to an empty list. Removes empty and or null
     * values from the input.
     * 
     * @param includedList included view state IDs.
     */
    public void setIncludedViewStateIds(@Nullable @NonnullElements final Collection<String> includedList) {   
        if (includedList!=null) {
            includedViewStateIds = new HashSet<>(includedList.size());
            for (final String include : includedList) {
                final String trimmedInclude = StringSupport.trimOrNull(include);
                if (INCLUDE_ALL_WILDCARD.equals(trimmedInclude)) {
                    includeAllViewStates = true;
                    includedViewStateIds = Collections.emptySet();
                    return;
                } else if (trimmedInclude!=null) {
                    includedViewStateIds.add(trimmedInclude);
                }                
            }
        } else {
            includedViewStateIds = Collections.emptySet();
        }        
    }
    
    /**
     * Determines if the current state contained in the request context is included and not excluded from
     * CSRF protection. More specifically, checks the state's ID against the included and excluded view sets;
     * returns true iff:
     * <ol>
     *  <li>All views-states are included, and the specific view-state has not been explicitly excluded. Or;</li>
     *  <li>All views-states are not included but the specific view-state has been explicitly included
     *  and not explicitly excluded.</li>
     * </ol>
     * 
     * @param context the request context that contains the current state identifier.
     * @return true iff the state, by it's identifier, should be included in CSRF checks. False otherwise.
     */
    protected boolean isStateIncluded(@Nonnull final RequestContext context) {
        
        if (context.getCurrentState()==null) {
            return false;
        }       
       
        final String stateId = context.getCurrentState().getId();
        
        if (includeAllViewStates && !excludedViewStateIds.contains(stateId)) {
            return true;
        }
        if (!includeAllViewStates) {
            if (includedViewStateIds.contains(stateId) && !excludedViewStateIds.contains(stateId)) {
                return true;
            }
        }
        
        return false;
        
    }

}
