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

package net.shibboleth.idp.saml.relyingparty.impl;

import java.util.Collection;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.opensaml.profile.context.ProfileRequestContext;
import org.opensaml.saml.common.messaging.context.navigate.EntityDescriptorLookupFunction;
import org.opensaml.saml.common.profile.logic.EntityAttributesPredicate;
import org.opensaml.saml.common.profile.logic.EntityAttributesPredicate.Candidate;
import org.opensaml.saml.common.profile.logic.EntityGroupNamePredicate;
import org.opensaml.saml.metadata.resolver.MetadataResolver;
import org.opensaml.saml.saml2.metadata.EntityDescriptor;

import net.shibboleth.idp.profile.logic.RelyingPartyIdPredicate;
import net.shibboleth.idp.profile.relyingparty.RelyingPartyConfiguration;
import net.shibboleth.idp.saml.profile.context.navigate.SAMLMetadataContextLookupFunction;
import net.shibboleth.idp.saml.profile.logic.MappedEntityAttributesPredicate;
import net.shibboleth.shared.annotation.constraint.NonnullElements;
import net.shibboleth.shared.logic.Constraint;
import net.shibboleth.shared.logic.StrategyIndirectedPredicate;

/**
 * Support functions for building {@link RelyingPartyConfiguration} objects with SAML functionality.
 */
public final class RelyingPartyConfigurationSupport {
    
    /** Constructor. */
    private RelyingPartyConfigurationSupport() {
        
    }
    
    /**
     * A shorthand method for constructing a {@link RelyingPartyConfiguration} with an activation condition based on
     * one or more relying party IDs.
     * 
     * <p>If a single ID is supplied, then the ID is also set as the identifier for the configuration.</p>
     * 
     * @param relyingPartyIds the relying parties for which the configuration should be active
     * 
     * @return  a default-constructed configuration with the appropriate condition set
     */
    @Nonnull public static RelyingPartyConfiguration byName(
            @Nonnull @NonnullElements final Collection<String> relyingPartyIds) {

        Constraint.isNotNull(relyingPartyIds, "Relying Party ID list cannot be null");

        final RelyingPartyConfiguration config = new RelyingPartyConfiguration();
        config.setActivationCondition(new RelyingPartyIdPredicate(relyingPartyIds));
        
        final StringBuffer name = new StringBuffer("EntityNames[");
        for (final String rpId: relyingPartyIds) {
            name.append(rpId).append(',');
            
        }
        name.append(']');
        config.setId(name.toString());
        return config;
    }

    /**
     * A shorthand method for constructing a {@link RelyingPartyConfiguration} with an activation condition based on
     * one or more {@link org.opensaml.saml.saml2.metadata.EntitiesDescriptor} groups, and optionally via
     * {@link org.opensaml.saml.saml2.metadata.AffiliationDescriptor} lookup.
     * 
     * @param groupNames the group names
     * @param resolver optional metadata source for affiliation lookup
     * 
     * @return  a default-constructed configuration with the appropriate condition set
     */
    @Nonnull public static RelyingPartyConfiguration byGroup(
            @Nonnull @NonnullElements final Collection<String> groupNames,
            @Nullable final MetadataResolver resolver) {
        Constraint.isNotNull(groupNames, "Group name list cannot be null");
        
        // We adapt an OpenSAML Predicate applying to an EntityDescriptor by indirecting the lookup of the
        // EntityDescriptor to a lookup sequence of PRC -> RPC -> SAMLMetadataContext -> EntityDescriptor.
        
        final StrategyIndirectedPredicate<ProfileRequestContext,EntityDescriptor> indirectPredicate =
                new StrategyIndirectedPredicate<>(
                        new EntityDescriptorLookupFunction().compose(new SAMLMetadataContextLookupFunction()),
                        new EntityGroupNamePredicate(groupNames, resolver));
        
        final RelyingPartyConfiguration config = new RelyingPartyConfiguration();
        config.setActivationCondition(indirectPredicate);

        final StringBuffer name = new StringBuffer("EntityGroups[");
        for (final String group: groupNames) {
            name.append(group).append(',');
            
        }
        name.append(']');
        config.setId(name.toString());
        return config;
    }

    
    /**
     * A shorthand method for constructing a {@link RelyingPartyConfiguration} with an activation condition based on
     * an {@link EntityAttributesPredicate}.
     * 
     * @param candidates the candidate rules
     * @param trim true iff tag values in metadata should be trimmed before comparison
     * @param matchAll true iff all the candidate rules are required to match
     * 
     * @return  a default-constructed configuration with the appropriate condition set
     */
    @Nonnull public static RelyingPartyConfiguration byTag(
            @Nonnull @NonnullElements final Collection<Candidate> candidates, final boolean trim,
            final boolean matchAll) {
        Constraint.isNotNull(candidates, "Candidate list cannot be null");
        
        // We adapt an OpenSAML Predicate applying to an EntityDescriptor by indirecting the lookup of the
        // EntityDescriptor to a lookup sequence of PRC -> RPC -> SAMLMetadataContext -> EntityDescriptor.
        
        final StrategyIndirectedPredicate<ProfileRequestContext,EntityDescriptor> indirectPredicate =
                new StrategyIndirectedPredicate<>(
                        new EntityDescriptorLookupFunction().compose(new SAMLMetadataContextLookupFunction()),
                        new EntityAttributesPredicate(candidates, trim, matchAll));
        
        final RelyingPartyConfiguration config = new RelyingPartyConfiguration();
        config.setActivationCondition(indirectPredicate);

        return config;
    }

    /**
     * A shorthand method for constructing a {@link RelyingPartyConfiguration} with an activation condition based on
     * a {@link MappedEntityAttributesPredicate}.
     * 
     * @param candidates the candidate rules
     * @param trim true iff tag values in metadata should be trimmed before comparison
     * @param matchAll true iff all the candidate rules are required to match
     * 
     * @return  a default-constructed configuration with the appropriate condition set
     */
    @Nonnull public static RelyingPartyConfiguration byMappedTag(
            @Nonnull @NonnullElements final Collection<Candidate> candidates, final boolean trim,
            final boolean matchAll) {
        Constraint.isNotNull(candidates, "Candidate list cannot be null");
        
        // We adapt an OpenSAML Predicate applying to an EntityDescriptor by indirecting the lookup of the
        // EntityDescriptor to a lookup sequence of PRC -> RPC -> SAMLMetadataContext -> EntityDescriptor.
        
        final StrategyIndirectedPredicate<ProfileRequestContext,EntityDescriptor> indirectPredicate =
                new StrategyIndirectedPredicate<>(
                        new EntityDescriptorLookupFunction().compose(new SAMLMetadataContextLookupFunction()),
                        new MappedEntityAttributesPredicate(candidates, trim, matchAll));
        
        final RelyingPartyConfiguration config = new RelyingPartyConfiguration();
        config.setActivationCondition(indirectPredicate);

        return config;
    }

}