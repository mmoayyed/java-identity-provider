/*
 * Licensed to the University Corporation for Advanced Internet Development, Inc.
 * under one or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information regarding
 * copyright ownership. The ASF licenses this file to You under the Apache 
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

package net.shibboleth.idp.profile;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import net.shibboleth.idp.Component;

import org.opensaml.util.Assert;
import org.opensaml.util.StringSupport;
import org.springframework.webflow.context.ExternalContext;
import org.springframework.webflow.core.collection.AttributeMap;
import org.springframework.webflow.execution.Event;
import org.springframework.webflow.execution.RequestContext;

/** Helper class for {@link org.springframework.webflow.execution.Action} operations. */
public final class ActionSupport {

    /**
     * ID of an Event indicating that the action completed successfully and processing should move on to the next
     * action.
     */
    public static final String PROCEED_EVENT_ID = "proceed";

    /**
     * ID of an Event indicating that the action completed successfully and that the profile request processing is
     * complete and a successful message should be sent back to the requester.
     */
    public static final String SUCCESS_EVENT_ID = "success";

    /**
     * ID of an Event indicating that the action encountered an error and that the profile request processing is
     * complete and an error should be sent back to the requester.
     */
    public static final String ERROR_EVENT_ID = "error";

    /**
     * Event attribute name under which the {@link Throwable} thrown by the {@link #doExecute(ProfileRequestContext)}
     * method is stored.
     */
    public static final String ERROR_THROWABLE_ID = "exception";

    /** Constructor. */
    private ActionSupport() {
    }

    /**
     * Gets the current {@link HttpServletRequest} from the current WebFlow request context.
     * 
     * @param requestContext current request context, may be null
     * 
     * @return the current Servlet request or null if the given context, external context, or Servlet request is null
     */
    public static HttpServletRequest getHttpServletRequest(RequestContext requestContext) {
        if (requestContext == null) {
            return null;
        }

        ExternalContext externalContext = requestContext.getExternalContext();
        if (externalContext == null) {
            return null;
        }

        return (HttpServletRequest) externalContext.getNativeRequest();
    }

    /**
     * Gets the current {@link HttpServletResponse} from the current WebFlow request context.
     * 
     * @param requestContext current request context, may be null
     * 
     * @return the current Servlet response or null if the given context, external context, or Servlet response is null
     */
    public static HttpServletResponse getHttpServletResponse(RequestContext requestContext) {
        if (requestContext == null) {
            return null;
        }

        ExternalContext externalContext = requestContext.getExternalContext();
        if (externalContext == null) {
            return null;
        }

        return (HttpServletResponse) externalContext.getNativeResponse();
    }

    /**
     * Builds an event, to be returned by the given component.
     * 
     * @param source IdP component that will return the constructed event, never null
     * @param eventId ID of the event, never null or empty
     * @param eventAttributes attributes associated with the event, may be null
     * 
     * @return the constructed {@link Event}
     */
    public static Event buildEvent(Component source, String eventId, AttributeMap eventAttributes) {
        Assert.isNotNull(source, "Component may not be null");

        final String trimmedEventId = StringSupport.trimOrNull(eventId);
        Assert.isNotNull(trimmedEventId, "ID of event for action " + source.getId() + " may not be null");

        if (eventAttributes == null || eventAttributes.isEmpty()) {
            return new Event(source.getId(), eventId);
        } else {
            return new Event(source.getId(), eventId, eventAttributes);
        }
    }
}