/*
 * Licensed to the University Corporation for Advanced Internet Development,
 * Inc. (UCAID) under one or more contributor license agreements.  See the
 * NOTICE file distributed with this work for additional information regarding
 * copyright ownership. The UCAID licenses this file to You under the Apache
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

package net.shibboleth.idp.cas.protocol;

import javax.annotation.Nullable;

import org.opensaml.messaging.context.BaseContext;

/**
 * Context container for CAS protocol request and response messages.
 *
 * @author Marvin S. Addison
 * 
 * @param <RequestType> request type
 * @param <ResponseType> response type
 */
public final class ProtocolContext<RequestType, ResponseType> extends BaseContext {
    
    /** CAS protocol request. */
    @Nullable private RequestType request;

    /** CAS protocol response. */
    @Nullable private ResponseType response;

    /**
     * Get the CAS protocol request.
     * 
     * @return CAS protocol request
     */
    @Nullable public RequestType getRequest() {
        return request;
    }

    /**
     * Set the CAS protocol request.
     *
     * @param req CAS protocol request.
     */
    public void setRequest(@Nullable final RequestType req) {
        request = req;
    }

    /**
     * Get the CAS protocol response.
     * 
     * @return CAS protocol response
     */
    @Nullable public ResponseType getResponse() {
        return response;
    }

    /**
     * Set the CAS protocol request.
     *
     * @param resp CAS protocol response.
     */
    public void setResponse(@Nullable final ResponseType resp) {
        response = resp;
    }
}
