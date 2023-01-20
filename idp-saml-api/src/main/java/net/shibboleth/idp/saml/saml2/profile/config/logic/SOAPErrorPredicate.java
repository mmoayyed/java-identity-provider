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

package net.shibboleth.idp.saml.saml2.profile.config.logic;

import java.util.function.Function;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.opensaml.profile.context.EventContext;
import org.opensaml.profile.context.ProfileRequestContext;
import org.opensaml.profile.context.navigate.CurrentOrPreviousEventLookup;

import net.shibboleth.idp.profile.config.ProfileConfiguration;
import net.shibboleth.idp.profile.context.RelyingPartyContext;
import net.shibboleth.idp.profile.logic.AbstractRelyingPartyPredicate;
import net.shibboleth.idp.saml.saml2.profile.config.ECPProfileConfiguration;
import net.shibboleth.shared.logic.Constraint;
import net.shibboleth.shared.primitive.LoggerFactory;

import org.slf4j.Logger;

/**
 * Predicate that decides whether to handle an error by returning a SOAP fault to a requester
 * or fail locally.
 * 
 * <p>Most ECP errors that don't result in a SAML Response should be handled with a SOAP fault,
 * but this can be overriden to accomodate special needs of clients, particularly when dealing
 * with login failure.</p>
 */
public class SOAPErrorPredicate extends AbstractRelyingPartyPredicate {

    /** Class logger. */
    @Nonnull private final Logger log = LoggerFactory.getLogger(SOAPErrorPredicate.class);
    
    /** Strategy function for access to {@link EventContext} to check. */
    @Nonnull private Function<ProfileRequestContext,EventContext> eventContextLookupStrategy;
    
    /** Constructor. */
    public SOAPErrorPredicate() {
        eventContextLookupStrategy = new CurrentOrPreviousEventLookup();
    }
    
    /**
     * Set lookup strategy for {@link EventContext} to check.
     * 
     * @param strategy  lookup strategy
     */
    public void setEventContextLookupStrategy(@Nonnull final Function<ProfileRequestContext,EventContext> strategy) {
        eventContextLookupStrategy = Constraint.isNotNull(strategy, "EventContext lookup strategy cannot be null");
    }
    
    /** {@inheritDoc} */
    public boolean test(@Nullable final ProfileRequestContext input) {
        
        final RelyingPartyContext rpCtx = getRelyingPartyContextLookupStrategy().apply(input);
        if (rpCtx == null) {
            log.debug("No RelyingPartyContext found, assuming error handled with SOAP fault");
            return true;
        }
        
        final EventContext eventCtx = eventContextLookupStrategy.apply(input);
        final Object event = eventCtx != null ? eventCtx.getEvent() : null;
        if (event == null) {
            log.debug("No event found, assuming error handled with SOAP fault");
            return true;
        }
        
        final ProfileConfiguration pc = rpCtx.getProfileConfig();
        if (!(pc instanceof ECPProfileConfiguration)) {
            log.debug("No ECP profile configuration found, assuming error handled with SOAP fault");
            return true;
        }
        
        final String eventString = event.toString();
        if (((ECPProfileConfiguration) pc).getLocalEvents(input).contains(eventString)) {
            log.debug("Error event {} will be handled locally", eventString);
            return false;
        }
        log.debug("Error event {} will be handled with SOAP fault", eventString);
        return true;
    }

}