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

package edu.internet2.middleware.shibboleth.common.profile.provider;

import java.util.ArrayList;
import java.util.List;

import edu.internet2.middleware.shibboleth.common.profile.AbstractProfileHandler;
import edu.internet2.middleware.shibboleth.common.relyingparty.RelyingPartyConfigurationManager;
import edu.internet2.middleware.shibboleth.common.session.Session;

/**
 * A specialization of profile handlers that binds the handler to a particular servlet request path. This assume the
 * request is an HTTP request.
 * 
 * @param <RPManagerType> type of relying party configuration manager used by this profile handler
 * @param <SessionType> type of sessions managed by the session manager used by this profile handler
 */
public abstract class AbstractRequestBoundProfileHandler<RPManagerType extends RelyingPartyConfigurationManager, SessionType extends Session>
        extends AbstractProfileHandler<RPManagerType, SessionType> {

    /** Request paths that to which this profile handler will respond. */
    private List<String> requestPaths;

    /** Constructor. */
    public AbstractRequestBoundProfileHandler() {
        super();
        requestPaths = new ArrayList<String>();
    }

    /**
     * Gets the request paths that to which this profile handler will respond.
     * 
     * @return request paths that to which this profile handler will respond
     */
    public List<String> getRequestPaths() {
        return requestPaths;
    }

    /**
     * Sets the request paths that to which this profile handler will respond.
     * 
     * @param paths request paths that to which this profile handler will respond
     */
    public void setRequestPaths(List<String> paths) {
        requestPaths = paths;
    }
}