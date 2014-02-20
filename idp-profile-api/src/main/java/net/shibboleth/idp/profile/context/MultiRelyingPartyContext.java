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

package net.shibboleth.idp.profile.context;

import java.util.Collection;
import java.util.Map;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import net.shibboleth.utilities.java.support.annotation.constraint.NonnullElements;
import net.shibboleth.utilities.java.support.annotation.constraint.NotEmpty;
import net.shibboleth.utilities.java.support.annotation.constraint.NotLive;
import net.shibboleth.utilities.java.support.annotation.constraint.Unmodifiable;
import net.shibboleth.utilities.java.support.logic.Constraint;
import net.shibboleth.utilities.java.support.primitive.StringSupport;

import org.opensaml.messaging.context.BaseContext;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ListMultimap;
import com.google.common.collect.Maps;

/**
 * {@link BaseContext} representing multiple relying parties involved in a request, usually a
 * subcontext of {@link org.opensaml.profile.context.ProfileRequestContext}.
 * 
 * <p>The multiple parties may be accessed as a collection, by their name, or by "labels",
 * which are specific to a given profile/scenario.</p>
 */
public final class MultiRelyingPartyContext extends BaseContext {

    /** Map of RP contexts indexed by name. */
    @Nonnull @NonnullElements private Map<String,RelyingPartyContext> relyingPartyIdMap;
    
    /** Multimap of RP contexts indexed by role. */
    @Nonnull @NonnullElements private ListMultimap<String,RelyingPartyContext> relyingPartyLabelMap;
    
    /** Constructor. */
    public MultiRelyingPartyContext() {
        relyingPartyIdMap = Maps.newHashMap();
        relyingPartyLabelMap = ArrayListMultimap.create();
    }
    
    /**
     * Get an immutable collection of the RP contexts.
     * 
     * @return  immutable collection of RP contexts
     */
    @Nonnull @NonnullElements @NotLive @Unmodifiable public Collection<RelyingPartyContext> getRelyingPartyContexts() {
        return ImmutableList.copyOf(relyingPartyIdMap.values());
    }
    
    /**
     * Get an immutable collection of RP contexts associated with a label.
     * 
     * @param label the label to search for
     * 
     * @return  corresponding RP contexts
     */
    @Nonnull @NonnullElements @NotLive @Unmodifiable public Collection<RelyingPartyContext> getRelyingPartyContexts(
            @Nonnull @NotEmpty final String label) {
        return ImmutableList.copyOf(relyingPartyLabelMap.get(
                Constraint.isNotNull(StringSupport.trimOrNull(label), "Label cannot be null or empty")));
    }
    
    /**
     * Get a RP context by name/id.
     * 
     * @param id the identifier to search for
     * 
     * @return  a corresponding RP context
     */
    @Nullable public RelyingPartyContext getRelyingPartyContextById(@Nonnull @NotEmpty final String id) {
        return relyingPartyIdMap.get(Constraint.isNotNull(StringSupport.trimOrNull(id), "ID cannot be null or empty"));
    }
    
    /**
     * Add a RP context associated with a label.
     * 
     * @param label the label to associate with the context
     * @param context context to add
     */
    public void addRelyingPartyContext(@Nonnull @NotEmpty final String label,
            @Nonnull final RelyingPartyContext context) {
        final String trimmed = Constraint.isNotNull(StringSupport.trimOrNull(label), "Label cannot be null or empty");
        Constraint.isNotNull(context, "Context cannot be null");
        Constraint.isNotNull(context.getRelyingPartyId(), "RelyingParty ID cannot be null");
        
        relyingPartyIdMap.put(context.getRelyingPartyId(), context);
        relyingPartyLabelMap.put(trimmed, context);
    }
    
    /**
     * Remove a RP context associated with a label.
     * 
     * @param label the label associated with the context
     * @param context context to remove
     */
    public void removeRelyingPartyContext(@Nonnull @NotEmpty final String label,
            @Nonnull final RelyingPartyContext context) {
        final String trimmed = Constraint.isNotNull(StringSupport.trimOrNull(label), "Label cannot be null or empty");
        Constraint.isNotNull(context, "Context cannot be null");
        Constraint.isNotNull(context.getRelyingPartyId(), "RelyingParty ID cannot be null");
        
        relyingPartyIdMap.remove(context.getRelyingPartyId());
        relyingPartyLabelMap.remove(trimmed, context);
    }
    
}