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

package net.shibboleth.idp.saml.profile.logic;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import net.shibboleth.idp.profile.context.RelyingPartyContext;
import net.shibboleth.utilities.java.support.annotation.constraint.NotEmpty;
import net.shibboleth.utilities.java.support.logic.Constraint;
import net.shibboleth.utilities.java.support.primitive.StringSupport;

import org.opensaml.core.xml.XMLObject;
import org.opensaml.messaging.context.BaseContext;
import org.opensaml.messaging.context.navigate.ChildContextLookup;
import org.opensaml.messaging.context.navigate.ContextDataLookupFunction;
import org.opensaml.profile.context.ProfileRequestContext;
import org.opensaml.saml.common.messaging.context.SAMLMetadataContext;
import org.opensaml.saml.common.messaging.context.SAMLPeerEntityContext;
import org.opensaml.saml.saml2.metadata.EntitiesDescriptor;

import com.google.common.base.Function;
import com.google.common.base.Functions;
import com.google.common.base.Predicate;

/**
 * Predicate to determine whether the supplied name matches any of an entity's containing
 * {@link EntitiesDescriptor} groups. 
 */
public class EntitiesDescriptorPredicate implements Predicate<ProfileRequestContext> {
    
    /** Strategy function to lookup SAMLMetadataContext. */
    @Nonnull private Function<ProfileRequestContext,SAMLMetadataContext> metadataContextLookupStrategy;
    
    /** Group to match. */
    @Nonnull @NotEmpty private final String groupName;
    
    /**
     * Constructor.
     * 
     * @param name group name to match
     */
    public EntitiesDescriptorPredicate(@Nonnull @NotEmpty final String name) {
        groupName = Constraint.isNotNull(StringSupport.trimOrNull(name), "Group name cannot be null or empty");
        
        // Default is PRC -> RPC -> SAML Peer -> Metadata
        metadataContextLookupStrategy = Functions.compose(
                Functions.compose(new ChildContextLookup<>(SAMLMetadataContext.class), new SAMLPeerEntityLookup()),
                new ChildContextLookup<ProfileRequestContext,RelyingPartyContext>(RelyingPartyContext.class));
    }
    
    /**
     * Set the lookup strategy to use to locate the {@link SAMLMetadataContext}.
     * 
     * @param strategy lookup function to use
     */
    public synchronized void setMetadataContextLookupStrategy(
            @Nonnull final Function<ProfileRequestContext,SAMLMetadataContext> strategy) {

        metadataContextLookupStrategy =
                Constraint.isNotNull(strategy, "SAMLMetadataContext lookup strategy cannot be null");
    }

    /** {@inheritDoc} */
    @Override
    public boolean apply(@Nullable final ProfileRequestContext input) {
        final SAMLMetadataContext metadataCtx = metadataContextLookupStrategy.apply(input);
        if (metadataCtx != null && metadataCtx.getEntityDescriptor() != null) {
            XMLObject group = metadataCtx.getEntityDescriptor().getParent();
            while (group != null && group instanceof EntitiesDescriptor) {
                if (((EntitiesDescriptor) group).getName() != null
                        && groupName.equals(((EntitiesDescriptor) group).getName())) {
                    return true;
                }
                group = group.getParent();
            }
        }
        
        return false;
    }
    
    /** A function to access a SAMLPeerEntityContext underlying a RelyingPartyContext. */
    private class SAMLPeerEntityLookup implements ContextDataLookupFunction<RelyingPartyContext,SAMLPeerEntityContext> {

        /** {@inheritDoc} */
        @Override
        @Nullable public SAMLPeerEntityContext apply(@Nullable final RelyingPartyContext input) {
            
            if (input != null) {
                final BaseContext peer = input.getRelyingPartyIdContextTree();
                if (peer != null && peer instanceof SAMLPeerEntityContext) {
                    return (SAMLPeerEntityContext) peer;
                }
            }
            
            return null;
        }
        
    }

}