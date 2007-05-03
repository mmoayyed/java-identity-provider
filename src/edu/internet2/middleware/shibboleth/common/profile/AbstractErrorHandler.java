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

import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;

/**
 * Error handlers are invoked when an error is encountered during request processing.
 */
public abstract class AbstractErrorHandler implements RequestHandler {

    /** Error that occured during request processing. */
    private Throwable error;

    /**
     * Gets the error that occured during request processing.
     * 
     * @return error that occured during request processing
     */
    public Throwable getError() {
        return error;
    }

    /**
     * Sets the error that occured during request processing.
     * 
     * @param e error that occured during request processing
     */
    public void setError(Throwable e) {
        error = e;
    }

    /** {@inheritDoc} */
    public abstract void processRequest(ProfileRequest<ServletRequest> request,
            ProfileResponse<ServletResponse> response);
}