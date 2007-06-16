/*
 * Copyright [2006] [University Corporation for Advanced Internet Development, Inc.]
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

package edu.internet2.middleware.shibboleth.common.profile;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Logger;
import org.opensaml.log.Level;

import edu.internet2.middleware.shibboleth.common.log.AccessLogEntry;

/**
 * Servlet responsible for dispatching incoming requests to the appropriate {@link AbstractProfileHandler}.
 */
public abstract class BaseServletProfileRequestDispatcher extends HttpServlet {

    /** Class logger. */
    private final Logger log = Logger.getLogger(BaseServletProfileRequestDispatcher.class);

    /** Access logger. */
    private final Logger accessLog = Logger.getLogger(AccessLogEntry.ACCESS_LOGGER_NAME);

    /**
     * Gets the manager used to retrieve handlers for requests.
     * 
     * @return manager used to retrieve handlers for requests
     */
    public ProfileHandlerManager getHandlerManager() {
        return (ProfileHandlerManager) getServletContext().getAttribute("handlerManager");
    }

    /** {@inheritDoc} */
    @SuppressWarnings("unchecked")
    protected void service(HttpServletRequest httpRequest, HttpServletResponse httpResponse) throws ServletException,
            IOException {
        if (accessLog.isInfoEnabled()) {
            AccessLogEntry accessEntry = new AccessLogEntry(httpRequest);
            accessLog.log(Level.CRITICAL, accessEntry);
        }

        ProfileRequest profileReq = getProfileRequest(httpRequest);
        ProfileResponse profileResp = getProfileResponse(httpResponse);

        AbstractErrorHandler errorHandler = getHandlerManager().getErrorHandler();
        ProfileHandler handler = getHandlerManager().getProfileHandler(httpRequest);
        if (handler != null) {
            try {
                handler.processRequest(profileReq, profileResp);
                return;
            } catch (Throwable t) {
                log.error("Encountered error processing request to " + httpRequest.getPathInfo()
                        + ", invoking error handler", t);
                httpRequest.setAttribute(AbstractErrorHandler.ERROR_KEY, t);
            }
        } else {
            log.warn("No profile handler for request to " + httpRequest.getPathInfo() + ", invoking error handler");
            httpRequest.setAttribute(AbstractErrorHandler.ERROR_KEY, new NoProfileHandlerException());
        }

        errorHandler.processRequest(profileReq, profileResp);
    }

    /**
     * Creates a profile request from a servlet request.
     * 
     * @param request the servlet request
     * 
     * @return the created profile request
     */
    protected abstract ProfileRequest getProfileRequest(ServletRequest request);

    /**
     * Creates a profile response from a servlet response.
     * 
     * @param response the servlet response
     * 
     * @return the created profile response
     */
    protected abstract ProfileResponse getProfileResponse(ServletResponse response);
}