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

import org.opensaml.common.binding.BindingException;
import org.opensaml.common.binding.MessageDecoder;
import org.opensaml.xml.XMLObject;

import edu.internet2.middleware.shibboleth.common.relyingparty.RelyingPartyConfiguration;
import edu.internet2.middleware.shibboleth.common.session.Session;

/**
 * Contextual information for requesting and receiving a response from a profile handler.
 * 
 * @param <RawRequestType> the type of the raw requests encapsulated in the profile request
 * @param <SessionType> the type of user session active during this request
 */
public interface ProfileRequest<RawRequestType, SessionType extends Session> {

    /**
     * Gets the raw, usually transport specific, request that was made.
     * 
     * @return raw request that was made
     */
    public RawRequestType getRawRequest();
    
    /**
     * Gets the current session for the user.
     * 
     * @return current session for the user
     */
    public SessionType getSession();
    
    /**
     * Gets a decoder that can be used to decode the servlet request.
     * 
     * @return message decoder
     */
    public MessageDecoder getMessageDecoder();
    
    /**
     * Gets configuration information for the requester (relying party).
     * 
     * @return configuration information for the requester
     */
    public RelyingPartyConfiguration getRelyingPartyConfiguration();
    
    /**
     * Gets the decoded request message.
     * 
     * @return decoded request message
     * 
     * @throws BindingException thrown if there is a problem decoding the message from the request
     */
    public XMLObject getRequest() throws BindingException;
}