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

/** Constants to use for {@link org.springframework.webflow.execution.Event} IDs. */
public final class EventIds {

    /** Is the SAML message version is incorrect (e.g., received a SAML 1.1 but expected SAML 2 message). */
    public static final String INVALID_MESSAGE_VERSION = "InvalidMessageVersion";

    /**
     * ID of the event returned if no {@link net.shibboleth.idp.relyingparty.RelyingPartyContext} is associated with the
     * {@link net.shibboleth.idp.profile.ProfileRequestContext}.
     */
    public static final String NO_RELYING_PARTY_CTX = "NoRelyingPartyContext";

    /**
     * ID of the event returned if no {@link net.shibboleth.idp.attribute.AttributeContext} is associated with the
     * {@link net.shibboleth.idp.relyingparty.RelyingPartyContext}.
     */
    public static final String NO_ATTRIBUTE_CTX = "NoAttributeContext";

    /** ID of the transition returned if some attributes can not be encoded. */
    public static final String UNABLE_ENCODE_ATTRIBUTE = "UnableToEncodeAttribute";

    /**
     * ID of the event returned if no SAML response is associated with the
     * {@link net.shibboleth.idp.profile.ProfileRequestContext}.
     */
    public static final String NO_RESPONSE = "NoResponse";

    /** The outbound message context already contains a SAML response. */
    public static final String RESPONSE_EXISTS = "ResponseExists";

    /**
     * ID of the event returned if the outbound response does not contain an assertion to which audiences can be added.
     */
    public static final String NO_ASSERTION = "NoAssertion";

    /** The inbound message did not contain an ID. */
    public static final String NO_IN_MSG_ID = "NoInboundMessageId";

    /** Constructor. */
    private EventIds() {
    }
}