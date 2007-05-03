/*
 * Copyright [2007] [University Corporation for Advanced Internet Development, Inc.]
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

package edu.internet2.middleware.shibboleth.common.profile.provider;

import javax.servlet.RequestDispatcher;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;

import org.apache.log4j.Logger;
import org.opensaml.xml.util.DatatypeHelper;

import edu.internet2.middleware.shibboleth.common.profile.AbstractErrorHandler;
import edu.internet2.middleware.shibboleth.common.profile.ProfileRequest;
import edu.internet2.middleware.shibboleth.common.profile.ProfileResponse;

/**
 * An error handler that forwards information to a JSP page for display to the end user.
 * 
 * The path to the JSP page provided to this handler may be absolute (i.e. start with a "/"), in which case the path is
 * assumed to be from the root of the servlet context, or relative, in which case the page is assumed to be relative
 * from the request dispatcher location. Deployers are strongly encouraged to use absolute paths.
 * 
 * The following request attributes are available to the JSP page:
 * 
 * <table>
 * <th>
 * <td>Attribute Name</td>
 * <td>Object Type</td>
 * <td>Value</td>
 * </th>
 * <tr>
 * <td>requestError</td>
 * <td>{@link Throwable}</td>
 * <td>Error that was thrown that triggered the invocation of this handler. </td>
 * </tr>
 * </table>
 */
public class JSPErrorHandler extends AbstractErrorHandler {
    
    /** Class logger. */
    private final Logger log = Logger.getLogger(JSPErrorHandler.class);

    /** Path to JSP page. */
    private String jspPage;

    /**
     * Constructor.
     * 
     * @param page path to JSP page
     */
    public JSPErrorHandler(String page) {
        jspPage = DatatypeHelper.safeTrimOrNullString(page);
        if (jspPage == null) {
            throw new IllegalArgumentException("JSP Error page may not be null.");
        }
    }

    /** {@inheritDoc} */
    public void processRequest(ProfileRequest<ServletRequest> request, ProfileResponse<ServletResponse> response) {
        RequestDispatcher dispatcher = request.getRawRequest().getRequestDispatcher(jspPage);
        try{
            ServletRequest servletRequest = request.getRawRequest();
            servletRequest.setAttribute("requestError", getError());
            dispatcher.forward(request.getRawRequest(), response.getRawResponse());
        }catch(Throwable t){
            log.error("Could not dispatch to error JSP page: " + jspPage, t);
        }
        return;
    }
}