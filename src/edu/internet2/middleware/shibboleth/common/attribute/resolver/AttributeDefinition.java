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

import java.util.List;

import edu.internet2.middleware.shibboleth.common.attribute.Attribute;
import edu.internet2.middleware.shibboleth.common.attribute.AttributeEncoder;

/**
 * A plugin used to create attributes or refine attributes provided from {@link DataConnector}s.
 * 
 * Attribute definitions must be stateless and thread-safe as a single instance may be used to service every request.
 */
public interface AttributeDefinition extends ResolutionPlugIn<Attribute> {

    /**
     * Gets the list of encoders to be added to attributes produced by this definition.
     * 
     * @return encoders to be added to attributes produced by this definition
     */
    public List<AttributeEncoder> getAttributeEncoders();
    
    /**
     * Gets whether this attribute definition is only a dependency and thus its values should never be released outside
     * the resolver.
     * 
     * @return whether this attribute definition is only a dependency
     */
    public boolean isDependencyOnly();
}