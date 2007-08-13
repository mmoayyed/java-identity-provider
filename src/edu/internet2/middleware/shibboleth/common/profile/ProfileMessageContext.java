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
import java.util.Map;

import org.opensaml.ws.message.MessageContext;

import edu.internet2.middleware.shibboleth.common.attribute.BaseAttribute;
import edu.internet2.middleware.shibboleth.common.relyingparty.ProfileConfiguration;
import edu.internet2.middleware.shibboleth.common.relyingparty.RelyingPartyConfiguration;
import edu.internet2.middleware.shibboleth.common.session.Session;

/**
 * Extension to the basic message conext that carries profile request specific information.
 * 
 * @param <ProfileConfigurationType> profile configuration type for current request
 */
public interface ProfileMessageContext<ProfileConfigurationType extends ProfileConfiguration> extends MessageContext {

    /**
     * Gets the attributes retrieved for the principal.
     * 
     * @return attributes retrieved for the principal
     */
    public Map<String, BaseAttribute> getPrincipalAttributes();

    /**
     * Gets the method used to authenticate the principal.
     * 
     * @return method used to authenticate the principal
     */
    public String getPrincipalAuthenticationMethod();

    /**
     * Gets the principal name of the subject of the request.
     * 
     * @return principal name of the subject of the request
     */
    public String getPrincipalName();

    /** 
     * Gets the configuration for the profile for the relying party.
     * 
     * @return configuration for the profile for the relying party
     */
    public ProfileConfigurationType getProfileConfiguration();

    /**
     * Gets the IDs of the attributes retrieved for the principal that were released to the relying party.
     * 
     * @return IDs of the attributes retrieved for the principal that were released to the relying party
     */
    public Collection<String> getReleasedPrincipalAttributeIds();

    /**
     * Gets the configuration for the relying party for this request.
     * 
     * @return configuration for the relying party for this request
     */
    public RelyingPartyConfiguration getRelyingPartyConfiguration();

    /**
     * Gets the collection of IDs for the attributes being requested by the relying party.
     * 
     * @return collection of IDs for the attributes being requested by the relying party
     */
    public Collection<String> getRequestedAttributesIds();

    /**
     * Gets the current user session, if there is one.
     * 
     * @return current user session
     */
    public Session getUserSession();

    /**
     * Sets the attributes retrieved for the principal.
     * 
     * @param attributes attributes retrieved for the principal
     */
    public void setPrincipalAttributes(Map<String, BaseAttribute> attributes);

    /**
     * Sets the method used to authenticate the principal.
     * 
     * @param method method used to authenticate the principal
     */
    public void setPrincipalAuthenticationMethod(String method);
    
    /**
     * Sets the principal name of the subject of the request.
     * 
     * @param name principal name of the subject of the request
     */
    public void setPrincipalName(String name);
    
    /**
     * Sets the configuration for the profile for the relying party.
     * 
     * @param configuration configuration for the profile for the relying party
     */
    public void setProfileConfiguration(ProfileConfigurationType configuration);

    /**
     * Sets the IDs of the attributes retrieved for the principal that were released to the relying party.
     * 
     * @param ids IDs of the attributes retrieved for the principal that were released to the relying party
     */
    public void setReleasedPrincipalAttributeIds(Collection<String> ids);

    /**
     * Sets the configuration for the relying party for this request.
     * 
     * @param configuration configuration for the relying party for this request
     */
    public void setRelyingPartyConfiguration(RelyingPartyConfiguration configuration);

    /**
     * Sets the collection of IDs for the attributes being requested by the relying party.
     * 
     * @param ids collection of IDs for the attributes being requested by the relying party
     */
    public void setRequestedAttributes(Collection<String> ids);

    /**
     * Sets the current user session.
     * 
     * @param session current user session
     */
    public void setUserSession(Session session);
}