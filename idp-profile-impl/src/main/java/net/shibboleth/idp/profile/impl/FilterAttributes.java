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
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import net.shibboleth.ext.spring.webflow.Event;
import net.shibboleth.ext.spring.webflow.Events;
import net.shibboleth.idp.attribute.AttributeContext;
import net.shibboleth.idp.attribute.filtering.AttributeFilterContext;
import net.shibboleth.idp.attribute.filtering.AttributeFilteringEngine;
import net.shibboleth.idp.attribute.filtering.AttributeFilteringException;
import net.shibboleth.idp.profile.AbstractProfileAction;
import net.shibboleth.idp.profile.ActionSupport;
import net.shibboleth.idp.profile.EventIds;
import net.shibboleth.idp.profile.ProfileException;
import net.shibboleth.idp.profile.ProfileRequestContext;
import net.shibboleth.idp.relyingparty.RelyingPartyContext;
import net.shibboleth.utilities.java.support.component.ComponentSupport;
import net.shibboleth.utilities.java.support.logic.Constraint;

import org.opensaml.messaging.context.navigate.ChildContextLookup;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.webflow.execution.RequestContext;

import com.google.common.base.Function;

/** A stage which invokes the {@link AttributeFilteringEngine} for the current request. */
@Events({@Event(id = EventIds.PROCEED_EVENT_ID),
        @Event(id = EventIds.NO_RELYING_PARTY_CTX, description = "No relying party context available for request"),
        @Event(id = EventIds.NO_ATTRIBUTE_CTX, description = "No attributes were available for filtering"),
        @Event(id = FilterAttributes.UNABLE_FILTER_ATTRIBS, description = "Error in filtering attributes")})
public class FilterAttributes extends AbstractProfileAction {

    /** ID of event indicating that the attribute filtering process failed. */
    public static final String UNABLE_FILTER_ATTRIBS = "UnableToFilterAttributes";

    /** Class logger. */
    private final Logger log = LoggerFactory.getLogger(FilterAttributes.class);

    /** Engine used to fetch attributes. */
    private final AttributeFilteringEngine filterEngine;

    /**
     * Strategy used to locate the {@link RelyingPartyContext} associated with a given {@link ProfileRequestContext}.
     */
    private Function<ProfileRequestContext, RelyingPartyContext> relyingPartyContextLookupStrategy;

    /**
     * Constructor. Initializes {@link #relyingPartyContextLookupStrategy} to {@link ChildContextLookup}.
     * 
     * @param engine engine used to filter attributes
     */
    public FilterAttributes(@Nonnull final AttributeFilteringEngine engine) {
        super();

        filterEngine = Constraint.isNotNull(engine, "Attribute filtering engine can not be null");

        relyingPartyContextLookupStrategy =
                new ChildContextLookup<ProfileRequestContext, RelyingPartyContext>(RelyingPartyContext.class, false);
    }

    /**
     * Gets the engine used to filter attributes.
     * 
     * @return engine used to filter attributes
     */
    @Nonnull public AttributeFilteringEngine getAttributeFilteringEnginer() {
        return filterEngine;
    }

    /**
     * Gets the strategy used to locate the {@link RelyingPartyContext} associated with a given
     * {@link ProfileRequestContext}.
     * 
     * @return strategy used to locate the {@link RelyingPartyContext} associated with a given
     *         {@link ProfileRequestContext}
     */
    @Nonnull public Function<ProfileRequestContext, RelyingPartyContext> getRelyingPartyContextLookupStrategy() {
        return relyingPartyContextLookupStrategy;
    }

    /**
     * Sets the strategy used to locate the {@link RelyingPartyContext} associated with a given
     * {@link ProfileRequestContext}.
     * 
     * @param strategy strategy used to locate the {@link RelyingPartyContext} associated with a given
     *            {@link ProfileRequestContext}
     */
    public synchronized void setRelyingPartyContextLookupStrategy(
            @Nonnull final Function<ProfileRequestContext, RelyingPartyContext> strategy) {
        ComponentSupport.ifInitializedThrowUnmodifiabledComponentException(this);

        relyingPartyContextLookupStrategy =
                Constraint.isNotNull(strategy, "RelyingPartyContext lookup strategy can not be null");
    }

    /** {@inheritDoc} */
    protected org.springframework.webflow.execution.Event doExecute(@Nullable final HttpServletRequest httpRequest,
            @Nullable final HttpServletResponse httpResponse, @Nullable final RequestContext springRequestContext,
            @Nonnull final ProfileRequestContext profileRequestContext) throws ProfileException {

        final RelyingPartyContext relyingPartyCtx = relyingPartyContextLookupStrategy.apply(profileRequestContext);
        if (relyingPartyCtx == null) {
            log.debug("Action {}: No relying party context available.", getId());
            return ActionSupport.buildEvent(this, EventIds.NO_RELYING_PARTY_CTX);
        }

        final AttributeContext attributeContext = relyingPartyCtx.getSubcontext(AttributeContext.class, false);
        if (attributeContext == null) {
            log.debug("Action {}: No attribute context, no attributes to filter", getId());
            return ActionSupport.buildEvent(this, EventIds.NO_ATTRIBUTE_CTX);
        }

        if (attributeContext.getAttributes().isEmpty()) {
            log.debug("Action {}: No attributes to filter", getId());
            return ActionSupport.buildProceedEvent(this);
        }

        // Get the filer context from the profile request
        // this may already exist but if not, auto-create it
        final AttributeFilterContext filterContext = relyingPartyCtx.getSubcontext(AttributeFilterContext.class, true);

        // If the filter context doesn't have a set of attributes to filter already
        // then look for them in the profile request context
        if (filterContext.getPrefilteredAttributes().isEmpty() && attributeContext != null) {
            filterContext.setPrefilteredAttributes(attributeContext.getAttributes().values());
        }

        try {
            filterEngine.filterAttributes(filterContext);
            // TODO should we do this
            // relyingPartyCtx.removeSubcontext(filterContext);

            attributeContext.setAttributes(filterContext.getFilteredAttributes().values());
        } catch (AttributeFilteringException e) {
            log.error("Action {}: Error encountered while filtering attributes", getId(), e);
            return ActionSupport.buildEvent(this, UNABLE_FILTER_ATTRIBS);
        }

        return ActionSupport.buildProceedEvent(this);
    }
}