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

import org.springframework.context.ApplicationEvent;

/**
 * An event representing the destruction of a Shibboleth session that occured 
 * because a user logged out of the system or because the application timed out.
 */
public class LogoutEvent extends ApplicationEvent {

    /** Serial version UID  */
    private static final long serialVersionUID = -1234450648177702760L;

    /**
     * Constructor
     *
     * @param session session of the user being logged out
     */
    public LogoutEvent(Session session){
        super(session);
    }
    
    /**
     * Gets the session for the user logging out.
     * 
     * @return session for the user logging out
     */
    public Session getUserSession(){
        return (Session) getSource();
    }
}