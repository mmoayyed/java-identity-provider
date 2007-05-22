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

package edu.internet2.middleware.shibboleth.common.config.profile;

import java.util.List;

import edu.internet2.middleware.shibboleth.common.profile.AbstractErrorHandler;
import edu.internet2.middleware.shibboleth.common.profile.ProfileHandler;

/**
 * Container for a single profile handler group configuration.
 */
public class ProfileHandlerGroup {

    /** Error handler for the group. */
    private AbstractErrorHandler errorHandler;

    /** List of profile handlers for the group. */
    private List<ProfileHandler> profileHandlers;

    /**
     * Gets the error handler for the group.
     * 
     * @return error handler for the group
     */
    public AbstractErrorHandler getErrorHandler() {
        return errorHandler;
    }

    /**
     * Sets the error handler for the group.
     * 
     * @param handler error handler for the group
     */
    public void setErrorHandler(AbstractErrorHandler handler) {
        errorHandler = handler;
    }

    /**
     * Gets the profile handlers for the group.
     * 
     * @return profile handlers for the group
     */
    public List<ProfileHandler> getProfileHandlers() {
        return profileHandlers;
    }

    /**
     * Sets the profile handlers for the group.
     * 
     * @param handlers profile handlers for the group
     */
    public void setProfileHandlers(List<ProfileHandler> handlers) {
        profileHandlers = handlers;
    }
}