/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
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

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.opensaml.messaging.context.BaseContext;
import org.opensaml.profile.action.EventIds;

import net.shibboleth.idp.profile.interceptor.ExternalInterceptor;
import net.shibboleth.shared.logic.Constraint;

/**
 * A context representing the state of an externalized interceptor flow.
 * 
 * @parent {@link ProfileInterceptorContext}
 * @added Before dispatching control to an external interceptor flow
 * 
 * @since 4.0.0
 */
public final class ExternalInterceptorContext extends BaseContext {
    
    /** Implementation object. */
    @Nonnull private final ExternalInterceptor externalInterceptor; 
    
    /** Value of flowExecutionUrl on branching from flow. */
    @Nullable private String flowExecutionUrl;

    /** Event to signal. */
    @Nullable private String eventId;
    
    /**
     * Constructor.
     * 
     * @param interceptor implementation object
     */
    public ExternalInterceptorContext(@Nonnull final ExternalInterceptor interceptor) {
        externalInterceptor = Constraint.isNotNull(interceptor, "ExternalInterceptor cannot be null");
        eventId = EventIds.PROCEED_EVENT_ID;
    }
    
    /**
     * Get the {@link ExternalInterceptor} installed in the context.
     * 
     * @return the interceptor implementation
     */
    @Nonnull public ExternalInterceptor getExternalInterceptor() {
        return externalInterceptor;
    }
    
    /**
     * Get the flow execution URL to return control to.
     * 
     * @return return location
     */
    @Nullable public String getFlowExecutionUrl() {
        return flowExecutionUrl;
    }
    
    /**
     * 
     * Set the flow execution URL to return control to.
     * 
     * @param url   return location
     * 
     * @return this context
     */
    @Nonnull public ExternalInterceptorContext setFlowExecutionUrl(@Nullable final String url) {
        flowExecutionUrl = url;
        
        return this;
    }

    /**
     * Get the event ID to signal as the result of this flow.
     * 
     * @return event ID
     */
    @Nullable public String getEventId() {
        return eventId;
    }

    /**
     * Set the event ID to signal as the result of this flow.
     * 
     * @param id event ID
     * 
     * @return this context
     */
    @Nonnull public ExternalInterceptorContext setEventId(@Nullable final String id) {
        eventId = id;
        
        return this;
    }

}