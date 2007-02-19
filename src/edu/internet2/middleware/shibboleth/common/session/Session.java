/*
 * Copyright [2006] [University Corporation for Advanced Internet Development, Inc.]
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

package edu.internet2.middleware.shibboleth.common.session;

import java.io.Serializable;
import java.net.InetAddress;

import org.joda.time.DateTime;

/**
 * Session information for user currently logged in.
 */
public interface Session extends Serializable {

    /**
     * Gets the unique identifier of the session.
     * 
     * @return unique identifier of the session
     */
    public String getSessionID();
    
    /**
     * Gets the IP address of the presenter.
     * 
     * @return IP address of the presenter
     */
    public InetAddress getPresenterAddress();

    /**
     * Gets the principal ID of the user.
     * 
     * @return principal ID of the user
     */
    public String getPrincipalID();

    /**
     * Gets the time of the last activity from the user.
     * 
     * @return time of the last activity from the user
     */
    public DateTime getLastActivityInstant();

    /**
     * Sets the time of the last activity from the user.
     * 
     * @param lastActivity time of the last activity from the user
     */
    public void setLastActivityInstant(DateTime lastActivity);
}