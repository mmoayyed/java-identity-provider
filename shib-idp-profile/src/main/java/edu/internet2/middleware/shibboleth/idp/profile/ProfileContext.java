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

import org.opensaml.messaging.context.impl.BasicInOutOperationContext;

/** Context that holds the ongoing state of a profile conversation. */
public class ProfileContext<InboundMessageType, OutboundMessageType> extends BasicInOutOperationContext<InboundMessageType, OutboundMessageType> {
        
    /** Current HTTP request. */
    private transient HttpServletRequest httpRequest;
    
    /** Current HTTP response. */
    private transient HttpServletResponse httpResponse;
    
    /** Constructor. */
    public ProfileContext() {
        super();
    }
    
    /**
     * Gets the current HTTP request if available.
     * 
     * @return current HTTP request
     */
    public HttpServletRequest getHttpRequest() {
        return httpRequest;
    }
    
    /**
     * Sets the current HTTP request.
     * 
     * @param request current HTTP request
     */
    public void setHttpRequest(HttpServletRequest request) {
        httpRequest = request;
    }
    
    /**
     * Gets the current HTTP response.
     * 
     * @return current HTTP response
     */
    public HttpServletResponse getHttpResponse() {
        return httpResponse;
    }
    
    /**
     * Sets the current HTTP response.
     * 
     * @param response current HTTP response
     */
    public void setHttpResponse(HttpServletResponse response) {
        httpResponse = response;
    }
}