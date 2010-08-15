/*
 * Copyright 2010 University Corporation for Advanced Internet Development, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package edu.internet2.middleware.shibboleth.idp.profile;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.opensaml.messaging.context.SubcontextContainer;
import org.opensaml.messaging.context.impl.BaseSubcontext;
import org.opensaml.util.Assert;
import org.opensaml.util.Strings;

/**
 * A {@link ProfileHandler} based on the idea of a conversation. Such a conversation may span multiple request/response
 * pairs. When a response is sent back to the user-agent the conversation is either completed or suspended. If
 * suspended, it may be resumed when another request comes in. The conversation is identified by the
 * {@value #CONV_ID_QUERY_PARAM} query parameter. As such, multiple conversations may exist, for a single user-agent, at
 * any given time.
 * 
 * Conversations are suspended by means of invoking the {@link #redirectTo(String, ProfileContext)} method and resumed
 * by means of the {@link #returnToProfileHandler(HttpServletRequest, HttpServletResponse)} method.
 */
public abstract class AbstractConversationalProfileHandler<InboundMessageType, OutboundMessageType> implements
        ProfileHandler {

    /** URL query parameter that holds the conversation ID. */
    private final static String CONV_ID_QUERY_PARAM = "convid";

    /**
     * URL path for this profile. This path does not contain any of the path used by the
     * {@link edu.internet2.middleware.shibboleth.idp.profile.ProfileRequestDispatcherServlet} to route the request to
     * this profile.
     */
    private final String profilePath;
    
    /** Whether this profile is passive or active.*/
    private boolean passiveProfile;

    /**
     * Constructor.
     * 
     * @param path path used to identify this profile handler
     */
    protected AbstractConversationalProfileHandler(String path) {
        String trimmedPath = Strings.trimOrNull(path);
        Assert.isNotNull(trimmedPath, "Profile path may not be null or empty");
        if (!trimmedPath.startsWith("/")) {
            profilePath = "/" + trimmedPath;
        } else {
            profilePath = trimmedPath;
        }
        profilePath.intern();
    }

    /** {@inheritDoc} */
    public String getProfilePath() {
        return profilePath;
    }
    
    /** {@inheritDoc} */
    public boolean isPassiveProfile() {
        return passiveProfile;
    }
    
    /**
     * Sets whether this profile is passive.
     * 
     * @param isPassive whether this profile is passive
     */
    public void setPassiveProfile(boolean isPassive) {
        passiveProfile = isPassive;
    }

    /** {@inheritDoc} */
    public void processRequest(HttpServletRequest request, HttpServletResponse response) {
        ProfileContext<InboundMessageType, OutboundMessageType> profileContext;

        if (!isReturningRequest(request)) {
            profileContext = createProfileContext(request, response);
            initiateConversation(profileContext);
        } else {
            profileContext = retrieveProfileContext(request, response);
            // TODO deal with case where no context may exist for the request
            resumeConversation(profileContext);
        }
    }

    /**
     * Checks to see if the given HTTP request represents a user-agent that had been interacting with the profile
     * handler previously and is resuming the operation.
     * 
     * @param request current HTTP request
     * 
     * @return true if the given request represents the return of a user-agent that had been interacting with the
     *         profile handler previously, false if not
     */
    protected boolean isReturningRequest(final HttpServletRequest request) {
        if (request.getParameter(CONV_ID_QUERY_PARAM) != null) {
            return true;
        }
        return false;
    }

    /**
     * Creates a profile for newly initiated conversation. This method creates a new {@link PipelineProfileContext} and
     * sets the return to profile URL and HTTP Servlet request and response properties on the profile.
     * 
     * @param request current HTTP request
     * @param response current HTTP response
     * 
     * @return the created profile context
     */
    protected ProfileContext<InboundMessageType, OutboundMessageType> createProfileContext(
            final HttpServletRequest request, final HttpServletResponse response) {
        ProfileContext<InboundMessageType, OutboundMessageType> profileContext = new ProfileContext<InboundMessageType, OutboundMessageType>();
        profileContext.setHttpRequest(request);
        profileContext.setHttpResponse(response);

        // TODO generate return URL
        String returnUrl = null;
        ConversationContext convContext = new ConversationContext(returnUrl, profileContext);
        profileContext.addSubcontext(convContext);

        return profileContext;
    }

    /**
     * Retrieves the profile context of a paused conversation. This method will also set the {@link HttpServletRequest}
     * and {@link HttpServletResponse} within the profile context to the current request and response.
     * 
     * @param request current HTTP request
     * @param response current HTTP response
     * 
     * @return retrieved profile context or null if no profile context exists
     */
    protected ProfileContext<InboundMessageType, OutboundMessageType> retrieveProfileContext(
            final HttpServletRequest request, final HttpServletResponse response) {
        // TODO get profile context from somewhere
        ProfileContext<InboundMessageType, OutboundMessageType> profileContext = null;
        profileContext.setHttpRequest(request);
        profileContext.setHttpResponse(response);
        return profileContext;
    }

    /**
     * Begins a new conversation.
     * 
     * @param profileContext current profile context with HTTP request/response, never null
     */
    protected abstract void initiateConversation(
            final ProfileContext<InboundMessageType, OutboundMessageType> profileContext);

    /**
     * Resumes a conversation that has previously been initiated.
     * 
     * @param current profile context with HTTP request/response, never null
     */
    protected abstract void resumeConversation(
            final ProfileContext<InboundMessageType, OutboundMessageType> profileContext);

    /**
     * Redirects the user to the given URL or path. If a path is given it is assumed to be the complete path and may
     * also include query parameters or fragments. Whether a fully forms URL is given or a path, an HTTP 302 is used to
     * redirect the user to that location. This allows for things like cookies to be set (or expired) in the user agent
     * before the user reaches the specified location.
     * 
     * <strong>NOTE:</strong> It is the responsibility of the stage to save its state in the profile context prior
     * invoking this method.
     * 
     * @param location full URL or path, may include query parameters and fragments, never null
     * @param profileContext current profile context
     */
    public static void redirectTo(final String location, final ProfileContext<?, ?> profileContext) {
        // TODO generate URL to this profile handler, append a conversation ID (UUID)
        // store return URL into profile context, store profile context indexed by ID,
        // get HTTP servlet response and send redirect, null out request/respnse from profile context
    }

    /**
     * Redirects the user agent back to this profile handler for continued processing.
     * 
     * <strong>NOTE:</strong> This will invoke the {@link PipelineStage#processRequest(PipelineProfileContext)}, with
     * the profile context given when {@link #redirectTo(String, PipelineProfileContext)} was invoked, of the stage that
     * triggered the redirect. It is the stage's responsibility to detect the return of the request and resume
     * processing from where it left off.
     * 
     * @param request current HTTP request
     * @param response current HTTP response
     */
    public static void returnToProfileHandler(final HttpServletRequest request, final HttpServletResponse response) {
        // TODO get conversation ID from request, lookup profile context, get return URL, send HTTP redirect
    }

    /** Subcontext that holds the state necessary to suspend and resume a conversation. */
    public final static class ConversationContext extends BaseSubcontext {

        /** The URL that will cause the conversation to be resumed. */
        private String profileHandlerUrl;

        /**
         * Constructor.
         * 
         * @param returnUrl URL that will cause the conversation to be resumed
         * @param owner {@link ProfileContext} that owns this subcontext
         */
        public ConversationContext(String returnUrl, SubcontextContainer owner) {
            super(owner);

            profileHandlerUrl = Strings.trimOrNull(returnUrl);
            Assert.isNotNull(profileHandlerUrl, "Profile handler URL may not be null or empty");
        }

        /**
         * Gets the URL that will return a request to the profile handler processing the current conversation.
         * 
         * @return URL that will return a request to the profile handler processing the current conversation
         */
        public String getReturnToProfileHandlerUrl() {
            return profileHandlerUrl;
        }
    }
}