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

package net.shibboleth.idp.saml.profile;

/**
 * SAML-specific constants to use for {@link org.opensaml.profile.action.ProfileAction}
 * {@link org.opensaml.profile.context.EventContext}s.
 */
public final class SAMLEventIds {

    /** Is the SAML message version is incorrect (e.g., received a SAML 1.1 but expected SAML 2 message). */
    public static final String INVALID_MESSAGE_VERSION = "InvalidMessageVersion";

    /** ID of the event returned if some attributes cannot be encoded. */
    public static final String UNABLE_ENCODE_ATTRIBUTE = "UnableToEncodeAttribute";

    /**
     * ID of the event returned if no SAML response is associated with the
     * {@link org.opensaml.profile.context.ProfileRequestContext}.
     */
    public static final String NO_RESPONSE = "NoResponse";

    /** ID of the event returned if the outbound message context already contains a SAML response. */
    public static final String RESPONSE_EXISTS = "ResponseExists";

    /** ID of the event returned if the outbound response does not contain an assertion. */
    public static final String NO_ASSERTION = "NoAssertion";

    /** ID of the event returned if the inbound message did not contain an ID. */
    public static final String NO_IN_MSG_ID = "NoInboundMessageId";

    /** Constructor. */
    private SAMLEventIds() {
    }
}