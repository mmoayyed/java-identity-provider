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

import javax.servlet.ServletResponse;

import org.opensaml.common.binding.MessageEncoder;

/**
 * Contextual information for receiving a response from a profile handler.
 */
public interface ProfileResponse {

    /**
     * Gets the servlet response where the encoded profile response can be written.
     * 
     * @return servlet response where a encoded message can be written
     */
    public ServletResponse getResponse();

    /**
     * Gets an encoder that can be used to encode the profile response.
     * 
     * @return message encoder
     */
    public MessageEncoder<ServletResponse> getMessageEncoder();

}
