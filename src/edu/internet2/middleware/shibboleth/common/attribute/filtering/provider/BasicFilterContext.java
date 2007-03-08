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
import edu.internet2.middleware.shibboleth.common.attribute.filtering.FilterContext;

/**
 * A basic implementation of the {@link FilterContext}.
 */
public class BasicFilterContext implements FilterContext {

    /** Requester of the attributes. */
    private String attributeRequester;
    
    /** Producer of the attributes. */
    private String attributeProducer;
    
    /** Principal name of the user the attributes are about. */
    private String princpialName;
    
    /** Attributes to be filtered with a map key of the attribute's ID. */
    private Map<String, Attribute> attributes;
    
    /**
     * Constructor.
     *
     * @param requester entity ID of the attribute requester
     * @param producer ID of the producer of the attribute
     * @param principal principal name of the user the attributes describe
     * @param attribs attributes about the user
     */
    public BasicFilterContext(String requester, String producer, String principal, Map<String, Attribute> attribs){
        attributeRequester = requester;
        attributeProducer = producer;
        princpialName = principal;
        attributes = attribs;
    }
    
    /** {@inheritDoc} */
    public String getAttributeProducer() {
        return attributeProducer;
    }

    /** {@inheritDoc} */
    public String getAttributeRequester() {
        return attributeRequester;
    }

    /** {@inheritDoc} */
    public String getPrincipalName() {
        return princpialName;
    }

    /** {@inheritDoc} */
    public Map<String, Attribute> getAttributes() {
        return attributes;
    }
}