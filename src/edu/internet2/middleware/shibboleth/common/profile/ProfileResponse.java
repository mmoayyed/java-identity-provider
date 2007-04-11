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
import org.opensaml.common.binding.MessageEncoder;
import org.opensaml.xml.XMLObject;

/**
 * Contextual information for receiving a response from a profile handler.
 * 
 * @param <RawResponseType> the type of the raw response encapsulated in the profile response
 */
public interface ProfileResponse<RawResponseType> {

    /**
     * Gets the raw, usually transport specific, response.
     * 
     * @return raw response
     */
    public RawResponseType getRawResponse();
    
    /**
     * Gets an encoder that can be used to encode the profile response.
     * 
     * @return message encoder
     */
    public MessageEncoder getMessageEncoder();

    /**
     * Encodes and sends the response back to the peer.
     * 
     * @param response the response to send
     * 
     * @throws BindingException thrown if the message can not be encoded and sent to the relying party
     */
    public void sendResponse(XMLObject response) throws BindingException;
}