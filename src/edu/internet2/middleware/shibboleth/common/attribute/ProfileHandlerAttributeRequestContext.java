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

package edu.internet2.middleware.shibboleth.common.attribute;

import edu.internet2.middleware.shibboleth.common.profile.ProfileRequest;
import edu.internet2.middleware.shibboleth.common.relyingparty.ProfileConfiguration;
import edu.internet2.middleware.shibboleth.common.session.Session;

/**
 * {@link AttributeRequestContext} that adds in information of requests coming from a web application.
 */
public interface ProfileHandlerAttributeRequestContext extends AttributeRequestContext {

    /**
     * Gets the request to the profile that in turn is request attributes.
     * 
     * @return request from the client
     */
    public ProfileRequest<?> getRequest();

    /**
     * Gets the effective profile configuration for the attribute requester.
     * 
     * @return effective profile configuration for the attribute requester
     */
    public ProfileConfiguration getProfileConfiguration();
    
    /**
     * Gets the current user session. This may be null if, for example, an unsolicited SAML attribute query were made by
     * the SP.
     * 
     * @return current user session
     */
    public Session getUserSession();
}