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
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import net.shibboleth.idp.attribute.AttributeContext;
import net.shibboleth.idp.attribute.filtering.AttributeFilterContext;
import net.shibboleth.idp.attribute.filtering.AttributeFilteringEngine;
import net.shibboleth.idp.attribute.filtering.AttributeFilteringException;
import net.shibboleth.idp.profile.AbstractProfileAction;
import net.shibboleth.idp.profile.ActionSupport;
import net.shibboleth.idp.profile.ProfileException;
import net.shibboleth.idp.profile.ProfileRequestContext;
import net.shibboleth.idp.relyingparty.RelyingPartyContext;
import net.shibboleth.utilities.java.support.component.ComponentSupport;
import net.shibboleth.utilities.java.support.logic.Constraint;

import org.opensaml.messaging.context.navigate.ChildContextLookup;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.webflow.execution.Event;
import org.springframework.webflow.execution.RequestContext;

import com.google.common.base.Function;

/** A stage which invokes the {@link AttributeFilteringEngine} for the current request. */
public class FilterAttributes extends AbstractProfileAction {

    /** Class logger. */
    private final Logger log = LoggerFactory.getLogger(FilterAttributes.class);

    /**
     * Strategy used to locate the {@link RelyingPartyContext} associated with a given {@link ProfileRequestContext}.
     */
    private Function<ProfileRequestContext, RelyingPartyContext> relyingPartyContextLookupStrategy;

    /** Resolver used to fetch attributes. */
    private AttributeFilteringEngine filterEngine;

    /** Constructor. The ID of this component is set to the name of this class. */
    public FilterAttributes() {
        super();

        setId(FilterAttributes.class.getName());
        relyingPartyContextLookupStrategy =
                new ChildContextLookup<ProfileRequestContext, RelyingPartyContext>(RelyingPartyContext.class,
                        false);
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
    protected Event doExecute(HttpServletRequest httpRequest, HttpServletResponse httpResponse,
            RequestContext springRequestContext, ProfileRequestContext profileRequestContext) throws ProfileException {

        final RelyingPartyContext relyingPartyCtx = relyingPartyContextLookupStrategy.apply(profileRequestContext);

        AttributeContext attributeContext = relyingPartyCtx.getSubcontext(AttributeContext.class, false);

        // Get the filer context from the profile request
        // this may already exist but if not, auto-create it
        final AttributeFilterContext filterContext =
                profileRequestContext.getSubcontext(AttributeFilterContext.class, true);

        // If the filter context doesn't have a set of attributes to filter already
        // then look for them in the profile request context
        if (filterContext.getPrefilteredAttributes().isEmpty() && attributeContext != null) {
            filterContext.setPrefilteredAttributes(attributeContext.getAttributes().values());
        }

        if (filterContext.getPrefilteredAttributes().isEmpty()) {
            log.debug("Action {}: No attributes available to filter, nothing to do", getId());
            return ActionSupport.buildProceedEvent(this);
        }

        try {
            filterEngine.filterAttributes(filterContext);
            profileRequestContext.removeSubcontext(filterContext);

            if (attributeContext == null) {
                attributeContext = new AttributeContext();
                relyingPartyCtx.addSubcontext(attributeContext);
            }

            attributeContext.setAttributes(filterContext.getFilteredAttributes().values());
        } catch (AttributeFilteringException e) {
            log.error("Action {}: Error encountered while filtering attributes", getId(), e);
            throw new UnableToFilterAttributesException(e);
        }

        return ActionSupport.buildProceedEvent(this);
    }

    /** Exception thrown if there is a problem filtering attributes. */
    public static class UnableToFilterAttributesException extends ProfileException {

        /** Serial version UID. */
        private static final long serialVersionUID = 4198028709026788847L;

        /** Constructor. */
        public UnableToFilterAttributesException() {
            super();
        }

        /**
         * Constructor.
         * 
         * @param message exception message
         */
        public UnableToFilterAttributesException(String message) {
            super(message);
        }

        /**
         * Constructor.
         * 
         * @param wrappedException exception to be wrapped by this one
         */
        public UnableToFilterAttributesException(Exception wrappedException) {
            super(wrappedException);
        }

        /**
         * Constructor.
         * 
         * @param message exception message
         * @param wrappedException exception to be wrapped by this one
         */
        public UnableToFilterAttributesException(String message, Exception wrappedException) {
            super(message, wrappedException);
        }
    }
}