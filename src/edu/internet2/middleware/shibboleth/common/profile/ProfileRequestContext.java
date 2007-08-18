/*
 * Copyright [2007] [University Corporation for Advanced Internet Development, Inc.]
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

import java.util.Collection;

import org.opensaml.ws.message.MessageContext;

import edu.internet2.middleware.shibboleth.common.relyingparty.ProfileConfiguration;
import edu.internet2.middleware.shibboleth.common.relyingparty.RelyingPartyConfiguration;
import edu.internet2.middleware.shibboleth.common.session.Session;

/**
 * Extension to the basic message conext that carries profile request specific information.
 * 
 * @param <ProfileConfigurationType> profile configuration type for current request
 */
public interface ProfileRequestContext<ProfileConfigurationType extends ProfileConfiguration> extends MessageContext {

    /**
     * Gets the configuration for the profile for the relying party.
     * 
     * @return configuration for the profile for the relying party
     */
    public ProfileConfigurationType getProfileConfiguration();

    /**
     * Gets the configuration for the relying party for this request.
     * 
     * @return configuration for the relying party for this request
     */
    public RelyingPartyConfiguration getRelyingPartyConfiguration();

    /**
     * Gets the current user session, if there is one.
     * 
     * @return current user session
     */
    public Session getUserSession();

    /**
     * Sets the configuration for the profile for the relying party.
     * 
     * @param configuration configuration for the profile for the relying party
     */
    public void setProfileConfiguration(ProfileConfigurationType configuration);

    /**
     * Sets the configuration for the relying party for this request.
     * 
     * @param configuration configuration for the relying party for this request
     */
    public void setRelyingPartyConfiguration(RelyingPartyConfiguration configuration);

    /**
     * Sets the current user session.
     * 
     * @param session current user session
     */
    public void setUserSession(Session session);

    /**
     * Gets the attributes, by ID, released to the peer.
     * 
     * @return attributes released to the peer
     */
    public Collection<String> getReleasedAttributes();

    /**
     * Sets the attributes, by ID, released to the peer.
     * 
     * @param attributeIds ids of the attributes released to the peer
     */
    public void setReleasedAttributes(Collection<String> attributeIds);
}