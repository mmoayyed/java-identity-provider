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

/** Constants to use for {@link org.springframework.webflow.execution.Event} IDs. */
public final class EventIds {

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

    /**
     * ID of an Event indicating that the action completed successfully and processing should move on to the next step.
     */
    public static final String PROCEED_EVENT_ID = "proceed";

    /** Constructor. */
    private EventIds() {
    }
}