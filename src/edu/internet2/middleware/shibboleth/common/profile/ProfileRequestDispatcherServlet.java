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
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.opensaml.ws.transport.http.HTTPInTransport;
import org.opensaml.ws.transport.http.HTTPOutTransport;
import org.opensaml.ws.transport.http.HttpServletRequestAdapter;
import org.opensaml.ws.transport.http.HttpServletResponseAdapter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import edu.internet2.middleware.shibboleth.common.log.AccessLogEntry;

/**
 * Servlet responsible for dispatching incoming requests to the appropriate {@link ProfileHandler}.
 */
public class ProfileRequestDispatcherServlet extends HttpServlet {

    /** Serial version UID. */
    private static final long serialVersionUID = 3750548606378986211L;

    /** Class logger. */
    private final Logger log = LoggerFactory.getLogger(ProfileRequestDispatcherServlet.class);

    /** Access logger. */
    private final Logger accessLog = LoggerFactory.getLogger(AccessLogEntry.ACCESS_LOGGER_NAME);

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
            accessLog.info(accessEntry.toString());
        }

        HTTPInTransport profileReq = new HttpServletRequestAdapter(httpRequest);
        HTTPOutTransport profileResp = new HttpServletResponseAdapter(httpResponse, httpRequest.isSecure());

        AbstractErrorHandler errorHandler = getHandlerManager().getErrorHandler();
        ProfileHandler handler = getHandlerManager().getProfileHandler(httpRequest);
        if (handler != null) {
            try {
                handler.processRequest(profileReq, profileResp);
                return;
            } catch (Throwable t) {
                if (t.getMessage() != null) {
                    httpRequest.setAttribute(AbstractErrorHandler.ERROR_KEY, t);
                } else {
                    httpRequest.setAttribute(AbstractErrorHandler.ERROR_KEY,
                            "Unknown. Please refer to the process log for the full error message");
                }
            }
        } else {
            log.warn("No profile handler configured for request at path: {}", httpRequest.getPathInfo());
            httpRequest.setAttribute(AbstractErrorHandler.ERROR_KEY, new NoProfileHandlerException(
                    "No profile handler configured for request at path: " + httpRequest.getPathInfo()));
        }

        errorHandler.processRequest(profileReq, profileResp);
        return;
    }
}