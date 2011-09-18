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

package net.shibboleth.idp.profile;

/** Exception thrown if the inbound message context was missing or contains invalid information. */
public class InvalidInboundMessageContextException extends ProfileException {

    /** Serial version UID. */
    private static final long serialVersionUID = 6823004133079557088L;

    /** Constructor. */
    public InvalidInboundMessageContextException() {
        super();
    }

    /**
     * Constructor.
     * 
     * @param message exception message
     */
    public InvalidInboundMessageContextException(String message) {
        super(message);
    }

    /**
     * Constructor.
     * 
     * @param wrappedException exception to be wrapped by this one
     */
    public InvalidInboundMessageContextException(Exception wrappedException) {
        super(wrappedException);
    }

    /**
     * Constructor.
     * 
     * @param message exception message
     * @param wrappedException exception to be wrapped by this one
     */
    public InvalidInboundMessageContextException(String message, Exception wrappedException) {
        super(message, wrappedException);
    }
}