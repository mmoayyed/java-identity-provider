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

import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;


/**
 * A processor for a communication profile supported by the IdP.
 * 
 * Profile handlers must be stateless and thread-safe as a single instance may be used to service every incoming request.
 */
public interface ProfileHandler {
    
    /**
     * Processes an incoming request.
     * 
     * @param request the request
     * @param response the response
     * 
     * @return true if this handler has processed the request, false if not
     * 
     * @throws ServletException throw if there was a problem while processing the request
     */
    public boolean processRequest(ServletRequest request, ServletResponse response) throws ServletException;
}