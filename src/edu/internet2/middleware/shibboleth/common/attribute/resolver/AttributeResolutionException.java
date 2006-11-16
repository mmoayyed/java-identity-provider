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

package edu.internet2.middleware.shibboleth.common.attribute.resolver;

import edu.internet2.middleware.shibboleth.common.ShibbolethException;


/**
 * An exception indicating a problem resolving a particular attribute.
 */
public class AttributeResolutionException extends ShibbolethException {

    /** Serial verison UID */
    private static final long serialVersionUID = -6467683432957797691L;

    /**
     * Constructor.
     */
    public AttributeResolutionException() {
        super();
    }
    
    /**
     * Constructor.
     * 
     * @param message exception message
     */
    public AttributeResolutionException(String message) {
        super(message);
    }
    
    /**
     * Constructor.
     * 
     * @param wrappedException exception to be wrapped by this one
     */
    public AttributeResolutionException(Exception wrappedException) {
        super(wrappedException);
    }
    
    /**
     * Constructor.
     * 
     * @param message exception message
     * @param wrappedException exception to be wrapped by this one
     */
    public AttributeResolutionException(String message, Exception wrappedException) {
        super(message, wrappedException);
    }
}