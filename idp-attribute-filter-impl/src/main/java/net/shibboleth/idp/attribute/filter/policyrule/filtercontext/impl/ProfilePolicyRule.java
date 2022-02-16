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

package net.shibboleth.idp.attribute.filter.policyrule.filtercontext.impl;

import java.util.function.Function;

import javax.annotation.Nonnull;

import net.shibboleth.idp.attribute.filter.context.AttributeFilterContext;
import net.shibboleth.idp.attribute.filter.policyrule.impl.AbstractStringPolicyRule;
import net.shibboleth.idp.profile.context.RelyingPartyContext;
import net.shibboleth.utilities.java.support.component.ComponentSupport;
import net.shibboleth.utilities.java.support.logic.Constraint;

import org.opensaml.messaging.context.navigate.ParentContextLookup;
import org.opensaml.profile.context.ProfileRequestContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Compare the profile identifier for this resolution with the provided string.
 * 
 * @since 4.2.0
 */
public class ProfilePolicyRule extends AbstractStringPolicyRule {

    /** Class logger. */
    @Nonnull private final Logger log = LoggerFactory.getLogger(ProfilePolicyRule.class);


    /** How to get to the {@link ProfileRequestContext} from the {@link AttributeFilterContext}. */
    @Nonnull private Function<AttributeFilterContext,ProfileRequestContext> profileContextStrategy;

    /** Constructor. */
    public ProfilePolicyRule() {
        profileContextStrategy =
                new ParentContextLookup<>(ProfileRequestContext.class).compose(
                        new ParentContextLookup<>(RelyingPartyContext.class));
    }

    /**
     * Set the context location strategy we'll use.
     * 
     * @return Returns the strategy.
     */
    public Function<AttributeFilterContext, ProfileRequestContext> getProfileContextStrategy() {
        return profileContextStrategy;
    }

    /**
     * Get the context location strategy we'll use.
     * 
     * @param strategy what to set.
     */
    public void setProfileContextStrategy(final Function<AttributeFilterContext,ProfileRequestContext> strategy) {
        profileContextStrategy = Constraint.isNotNull(strategy, "ProfileContext lookup strategy cannot be null");
    }
    
    /**
     * Compare the principal name for this resolution with the provided string.
     * 
     * @param filterContext the context
     * @return whether it matches
     * 
     *         {@inheritDoc}
     */
    @Override public Tristate matches(@Nonnull final AttributeFilterContext filterContext) {
        ComponentSupport.ifNotInitializedThrowUninitializedComponentException(this);
        
        final ProfileRequestContext pc = profileContextStrategy.apply(filterContext);
        if (null == pc) {
            log.warn("{} Could not locate profile context", getLogPrefix());
            return Tristate.FAIL;
        }

        return stringCompare(pc.getProfileId());
    }

}