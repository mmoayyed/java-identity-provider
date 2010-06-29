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

import java.io.IOException;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.opensaml.util.Strings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Servlet responsible for dispatching incoming requests to the appropriate {@link ProfileHandler}. */
public class ProfileRequestDispatcherServlet extends HttpServlet {

    /** Serial version UID. */
    // TODO private static final long serialVersionUID = 3750548606378986211L;

    /** Class logger. */
    private final Logger log = LoggerFactory.getLogger(ProfileRequestDispatcherServlet.class);

    /** Access logger. */
    // TODO private final Logger accessLog;

    /** Profile handler manager. */
    private ProfileHandlerManager handlerManager;

    /** {@inheritDoc} */
    public void init(ServletConfig config) throws ServletException {
        super.init(config);

        String handlerManagerId = config.getInitParameter("handlerManagerId");
        if (Strings.isNullOrEmpty(handlerManagerId)) {
            handlerManagerId = "shibboleth.HandlerManager";
        }

        handlerManager = (ProfileHandlerManager) getServletContext().getAttribute(handlerManagerId);
    }

    /** {@inheritDoc} */
    @SuppressWarnings("unchecked")
    protected void service(HttpServletRequest httpRequest, HttpServletResponse httpResponse) throws ServletException,
            IOException {
        // TODO log access message

        ProfileHandler handler = handlerManager.getProfileHandler(httpRequest);
        if (handler != null) {
            try {
                handler.processRequest(httpRequest, httpResponse);
                return;
            } catch (Throwable t) {
                // TODO use error handler of last resort
                log.error("Error occured while processing request", t);
            }
        } else {
            log.warn("No profile handler configured for request at path: {}", httpRequest.getPathInfo());
            // TODO 404 page
        }

        return;
    }
}