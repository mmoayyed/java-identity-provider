/*
 * Copyright 2010 University Corporation for Advanced Internet Development, Inc.
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

package edu.internet2.middleware.shibboleth.idp.attribute.resolver;

import edu.internet2.middleware.shibboleth.idp.attribute.AttributeException;

/** An exception indicating a problem resolving attribute data. */
public class AttributeResolutionException extends AttributeException {

    /** Serial version UID. */
    private static final long serialVersionUID = -3921717206902437845L;

    /** Constructor. */
    public AttributeResolutionException() {
        super();
    }

    /**
     * Constructor.
     * 
     * @param message exception message
     */
    public AttributeResolutionException(final String message) {
        super(message);
    }

    /**
     * Constructor.
     * 
     * @param wrappedException exception to be wrapped by this one
     */
    public AttributeResolutionException(final Exception wrappedException) {
        super(wrappedException);
    }

    /**
     * Constructor.
     * 
     * @param message exception message
     * @param wrappedException exception to be wrapped by this one
     */
    public AttributeResolutionException(final String message, final Exception wrappedException) {
        super(message, wrappedException);
    }
}