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

import javax.servlet.http.HttpSession;
import javax.servlet.http.HttpSessionEvent;
import javax.servlet.http.HttpSessionListener;

/**
 * A listener that listens for the destruction of {@link HttpSession}s.  This allows
 * the {@link SessionManager} to appropriately destroy a shibboleth session whether the 
 * HTTP session is destroyed explicitly or through inactivity.
 */
public class ContainerSessionListener implements HttpSessionListener {

    /**
     * A no-op operation.
     */
    public void sessionCreated(HttpSessionEvent arg0) {
        // we don't care about session creations
    }

    /** {@inheritDoc} */
    public void sessionDestroyed(HttpSessionEvent arg0) {
        // TODO Auto-generated method stub

    }
}