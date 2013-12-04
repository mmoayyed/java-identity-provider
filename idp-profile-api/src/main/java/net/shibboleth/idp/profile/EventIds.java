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

// TODO: move attribute related events into that module?

/**
 * IdP-specific constants to use for {@link org.opensaml.profile.action.ProfileAction}
 * {@link org.opensaml.profile.context.EventContext}s.
 */
public final class EventIds {

    /**
     * ID of the event returned if a {@link net.shibboleth.idp.relyingparty.RelyingPartyContext} is missing or corrupt
     * in some way..
     */
    public static final String INVALID_RELYING_PARTY_CTX = "InvalidRelyingPartyContext";

    /**
     * ID of the event returned if a {@link net.shibboleth.idp.relyingparty.RelyingPartyConfiguration} is missing or
     * corrupt in some way..
     */
    public static final String INVALID_RELYING_PARTY_CONFIG = "InvalidRelyingPartyConfiguration";

    /**
     * ID of the event returned if a {@link net.shibboleth.idp.profile.config.ProfileConfiguration} is missing or
     * corrupt in some way.
     */
    public static final String INVALID_PROFILE_CONFIG = "InvalidProfileConfiguration";

    /**
     * ID of the event returned if a {@link net.shibboleth.idp.attribute.AttributeContext} is missing or corrupt in some
     * way.
     */
    public static final String INVALID_ATTRIBUTE_CTX = "InvalidAttributeContext";

    /** ID of the event returned when there is a problem resolving attributes. */
    public static final String UNABLE_RESOLVE_ATTRIBS = "UnableToResolveAttributes";
 
    /** ID of event indicating that the attribute filtering process failed. */
    public static final String UNABLE_FILTER_ATTRIBS = "UnableToFilterAttributes";
    
    /** Constructor. */
    private EventIds() {
        
    }
    
}