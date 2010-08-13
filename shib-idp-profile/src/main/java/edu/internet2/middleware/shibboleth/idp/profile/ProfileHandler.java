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

/** 
 * A profile handler encapsulates the logic of particular message exchanges with defined semantics.  Such 
 * defined exchanges are called "profiles" in many standards, hence the name profile handler.  Because some 
 * profiles can very in subtle manners unknown until after a modest amount of processing, each profile handler
 * is mapped to a specific URL path.  It is then the responsibility of the invoker to pick the correct handler
 * based on the specific profile they are attempting to use.
 * 
 * Some examples of such exchanges are SAML 2 SSO requests and OpenID association request. 
 */
public interface ProfileHandler {

    /**
     * Gets the path to which the profile is bound.  Note, this path does not include the Servlet context or any
     * path mapping for profile requests (i.e. if only /profiles/* are mapped to the profile request dispatcher the
     * path component '/profiles' will not show up in this string).
     * 
     * 
     * @return path to which the profile is bound, never null
     */
    public String getProfilePath();
    
    /**
     * Processes an incoming request.
     * 
     * @param request the inbound HTTP request
     * @param response the outbound HTTP response
     */
    public void processRequest(HttpServletRequest request, HttpServletResponse response);

}