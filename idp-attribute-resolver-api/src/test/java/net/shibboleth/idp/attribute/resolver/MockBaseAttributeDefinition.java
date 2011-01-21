/*
 * Copyright 2011 University Corporation for Advanced Internet Development, Inc.
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

package net.shibboleth.idp.attribute.resolver;

import net.shibboleth.idp.attribute.Attribute;

/** Mock implementation of {@link BaseAttributeDefinition}. */
public class MockBaseAttributeDefinition extends BaseAttributeDefinition {

    /** Static attribute value returned from resolution. */
    private Attribute staticAttribute;

    /**
     * Constructor.
     * 
     * @param id id of the attribute definition, never null or empty
     * @param attribute value returned from the resolution of this attribute, may be null
     */
    public MockBaseAttributeDefinition(String id, Attribute<?> attribute) {
        super(id);
        staticAttribute = attribute;
    }

    /** {@inheritDoc} */
    protected Attribute<?> doAttributeResolution(AttributeResolutionContext resolutionContext)
            throws AttributeResolutionException {
        return staticAttribute;
    }
}