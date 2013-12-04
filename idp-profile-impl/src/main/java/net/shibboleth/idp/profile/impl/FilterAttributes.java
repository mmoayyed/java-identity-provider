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

package net.shibboleth.idp.profile.impl;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import net.shibboleth.idp.attribute.AttributeContext;
import net.shibboleth.idp.attribute.filter.AttributeFilterContext;
import net.shibboleth.idp.attribute.filter.AttributeFilter;
import net.shibboleth.idp.attribute.filter.AttributeFilterException;
import net.shibboleth.idp.profile.EventIds;

import org.opensaml.profile.ProfileException;
import org.opensaml.profile.action.AbstractProfileAction;
import org.opensaml.profile.action.ActionSupport;
import org.opensaml.profile.context.ProfileRequestContext;

import net.shibboleth.idp.relyingparty.RelyingPartyContext;
import net.shibboleth.utilities.java.support.component.ComponentSupport;
import net.shibboleth.utilities.java.support.logic.Constraint;

import org.opensaml.messaging.context.navigate.ChildContextLookup;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Function;

/**
 * Action that invokes the {@link AttributeFilter} for the current request.
 * 
 * @event {@link org.opensaml.profile.action.EventIds#PROCEED_EVENT_ID}
 * @event {@link EventIds#INVALID_RELYING_PARTY_CTX}
 * @event {@link EventIds#INVALID_ATTRIBUTE_CTX}
 * @event {@link EventIds#UNABLE_FILTER_ATTRIBS}
 * 
 * @post If resolution is successful, the relevant
 * RelyingPartyContext.getSubcontext(AttributeContext.class, false) != null
 */
public class FilterAttributes extends AbstractProfileAction {

    /** Class logger. */
    @Nonnull private final Logger log = LoggerFactory.getLogger(FilterAttributes.class);

    /** Engine used to fetch attributes. */
    @Nonnull private final AttributeFilter filterEngine;

    /**
     * Strategy used to locate the {@link RelyingPartyContext} associated with a given {@link ProfileRequestContext}.
     */
    @Nonnull private Function<ProfileRequestContext, RelyingPartyContext> relyingPartyContextLookupStrategy;

    /** RelyingPartyContext to operate on. */
    @Nullable private RelyingPartyContext rpContext;
    
    /** AttributeContext to filter. */
    @Nullable private AttributeContext attributeContext;
    
    /**
     * Constructor. Initializes {@link #relyingPartyContextLookupStrategy} to {@link ChildContextLookup}.
     * 
     * @param engine engine used to filter attributes
     */
    public FilterAttributes(@Nonnull final AttributeFilter engine) {
        filterEngine = Constraint.isNotNull(engine, "AttributeFilter cannot be null");
        relyingPartyContextLookupStrategy = new ChildContextLookup<>(RelyingPartyContext.class, false);
    }

    /**
     * Sets the strategy used to locate the {@link RelyingPartyContext} associated with a given
     * {@link ProfileRequestContext}.
     * 
     * @param strategy strategy used to locate the {@link RelyingPartyContext} associated with a given
     *            {@link ProfileRequestContext}
     */
    public void setRelyingPartyContextLookupStrategy(
            @Nonnull final Function<ProfileRequestContext, RelyingPartyContext> strategy) {
        ComponentSupport.ifInitializedThrowUnmodifiabledComponentException(this);

        relyingPartyContextLookupStrategy = Constraint.isNotNull(strategy,
                "RelyingPartyContext lookup strategy cannot be null");
    }

    /** {@inheritDoc} */
    @Override
    protected boolean doPreExecute(@Nonnull final ProfileRequestContext profileRequestContext) throws ProfileException {
        rpContext = relyingPartyContextLookupStrategy.apply(profileRequestContext);
        if (rpContext == null) {
            log.debug("{} No relying party context available", getLogPrefix());
            ActionSupport.buildEvent(profileRequestContext, EventIds.INVALID_RELYING_PARTY_CTX);
            return false;
        }

        attributeContext = rpContext.getSubcontext(AttributeContext.class, false);
        if (attributeContext == null) {
            log.debug("{} No attribute context, no attributes to filter", getLogPrefix());
            ActionSupport.buildEvent(profileRequestContext, EventIds.INVALID_ATTRIBUTE_CTX);
            return false;
        }

        if (attributeContext.getIdPAttributes().isEmpty()) {
            log.debug("{} No attributes to filter", getLogPrefix());
            return false;
        }
        
        return true;
    }
    
    /** {@inheritDoc} */
    @Override
    protected void doExecute(@Nonnull final ProfileRequestContext profileRequestContext) throws ProfileException {

        // Get the filer context from the profile request
        // this may already exist but if not, auto-create it.
        final AttributeFilterContext filterContext = rpContext.getSubcontext(AttributeFilterContext.class, true);

        // If the filter context doesn't have a set of attributes to filter already
        // then look for them in the AttributeContext.
        if (filterContext.getPrefilteredIdPAttributes().isEmpty()) {
            filterContext.setPrefilteredIdPAttributes(attributeContext.getIdPAttributes().values());
        }

        try {
            filterEngine.filterAttributes(filterContext);
            rpContext.removeSubcontext(filterContext);

            attributeContext.setIdPAttributes(filterContext.getFilteredIdPAttributes().values());
        } catch (AttributeFilterException e) {
            log.error(getLogPrefix() + " Error encountered while filtering attributes", e);
            ActionSupport.buildEvent(profileRequestContext, EventIds.UNABLE_FILTER_ATTRIBS);
        }
    }
    
}