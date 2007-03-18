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

package edu.internet2.middleware.shibboleth.common.attribute.filtering.provider;

import java.util.Map;

import edu.internet2.middleware.shibboleth.common.attribute.Attribute;
import edu.internet2.middleware.shibboleth.common.attribute.provider.ShibbolethAttributeRequestContext;

/**
 * Contextual information for performing attribute filtering.
 */
public class ShibbolethFilteringContext {

    /** The attribute request. */
    private ShibbolethAttributeRequestContext attributeRequestContext;

    /** Attributes being filtered. */
    private Map<String, Attribute> attributes;

    /**
     * Constructor.
     * 
     * @param unfilteredAttributes unfiltered attribute set
     * @param context attribute request context
     */
    public ShibbolethFilteringContext(Map<String, Attribute> unfilteredAttributes,
            ShibbolethAttributeRequestContext context) {
        attributeRequestContext = context;
        attributes = unfilteredAttributes;
    }

    /**
     * Gets the context for the attribute request.
     * 
     * @return context for the attribute request
     */
    public ShibbolethAttributeRequestContext getAttribtueRequestContext() {
        return attributeRequestContext;
    }

    /**
     * Gets the attributes being filtered.
     * 
     * @return attributes being filtered
     */
    public Map<String, Attribute> getAttributes() {
        return attributes;
    }
}