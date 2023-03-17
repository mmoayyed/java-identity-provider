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

package net.shibboleth.idp.profile.context.navigate;

import java.util.Objects;
import java.util.function.Function;

import javax.annotation.Nullable;

import net.shibboleth.idp.profile.context.SpringRequestContext;

import org.opensaml.profile.context.EventContext;
import org.opensaml.profile.context.PreviousEventContext;
import org.opensaml.profile.context.ProfileRequestContext;
import org.springframework.webflow.execution.RequestContext;

/**
 * A {@link Function} that checks for cases in which the webflow's current event is not reflected by
 * an attached {@link EventContext} and compensates, along with returning a suitably populated context.
 */
public class WebFlowCurrentEventLookupFunction implements Function<ProfileRequestContext,EventContext> {

    /** {@inheritDoc} */
    @Nullable public EventContext apply(@Nullable final ProfileRequestContext input) {
        if (input == null) {
            return null;
        }
        
        EventContext eventCtx = input.getSubcontext(EventContext.class);
        if (eventCtx == null || eventCtx.getEvent() == null) {
            eventCtx = input.getSubcontext(PreviousEventContext.class);
        }
        
        final SpringRequestContext springContext = input.getSubcontext(SpringRequestContext.class);
        final RequestContext springRequest = springContext != null ? springContext.getRequestContext() : null;

        
        // If nothing in the Spring layer, just return what we have.
        if (springRequest == null || springRequest.getCurrentEvent() == null) {
            return eventCtx;
        }

        final Object current = eventCtx != null ? eventCtx.getEvent() : null;
        if (current == null || !Objects.equals(current.toString(), springRequest.getCurrentEvent().getId())) {
            eventCtx = input.ensureSubcontext(EventContext.class);
            eventCtx.setEvent(springRequest.getCurrentEvent());
        }
        
        return eventCtx;
    }
    
}