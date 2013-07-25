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

package net.shibboleth.idp.authn;

/**
 * Constants to use for {@link org.opensaml.profile.action.ProfileAction}
 * {@link org.opensaml.profile.context.EventContext}s.
 */
public final class AuthnEventIds {

    /** ID of event that indicates the AuthenticationContext is missing or corrupt in some way. */
    public static final String INVALID_AUTHN_CTX = "InvalidAuthenticationContext";

    /** ID of event returned if there are no authentication flows that could be used to authenticate the user. */
    public static final String NO_POTENTIAL_FLOW = "NoPotentialFlow";

    /** ID of event returned if there no authentication flows that can satisfy the request's requirements. */
    public static final String NO_REQUESTED_FLOW = "NoRequestedFlow";
    
    /** ID of event returned if there are no credentials available in the request. */
    public static final String NO_CREDENTIALS = "NoCredentials";

    /** ID of event returned if there given credentials are invalid. */
    public static final String INVALID_CREDENTIALS = "InvalidCredentials";

    /** Constructor. */
    private AuthnEventIds() {
    }
}