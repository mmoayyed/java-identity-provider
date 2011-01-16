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

package net.shibboleth.idp.attribute.filtering;

import net.shibboleth.idp.attribute.AttributeException;

/** Indicates that an error has occurred during an attribute filtering process. */
public class AttributeFilteringException extends AttributeException {

    /** Serial version UID. */
    private static final long serialVersionUID = 2699384174240632113L;

    /** Constructor. */
    public AttributeFilteringException() {
        super();
    }

    /**
     * Constructor.
     * 
     * @param message exception message
     */
    public AttributeFilteringException(final String message) {
        super(message);
    }

    /**
     * Constructor.
     * 
     * @param wrappedException exception to be wrapped by this one
     */
    public AttributeFilteringException(final Exception wrappedException) {
        super(wrappedException);
    }

    /**
     * Constructor.
     * 
     * @param message exception message
     * @param wrappedException exception to be wrapped by this one
     */
    public AttributeFilteringException(final String message, final Exception wrappedException) {
        super(message, wrappedException);
    }
}