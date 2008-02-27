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

package edu.internet2.middleware.shibboleth.common.attribute.resolver.provider.attributeDefinition;

import java.util.List;
import java.util.Locale;
import java.util.Map;

import edu.internet2.middleware.shibboleth.common.attribute.BaseAttribute;
import edu.internet2.middleware.shibboleth.common.attribute.encoding.AttributeEncoder;
import edu.internet2.middleware.shibboleth.common.attribute.resolver.provider.ResolutionPlugIn;
import edu.internet2.middleware.shibboleth.common.attribute.resolver.provider.dataConnector.DataConnector;

/**
 * A plugin used to create attributes or refine attributes provided from {@link DataConnector}s.
 * 
 * Attribute definitions must be stateless and thread-safe as a single instance may be used to service every request.
 */
public interface AttributeDefinition extends ResolutionPlugIn<BaseAttribute> {

    /**
     * Gets the localized human readable description of attribute.
     * 
     * @return human readable description of attribute
     */
    public Map<Locale, String> getDisplayDescriptions();

    /**
     * Gets the localized human readable name of the attribute.
     * 
     * @return human readable name of the attribute
     */
    public Map<Locale, String> getDisplayNames();
    
    /**
     * Gets the map of encoders to be added to attributes produced by this definition, keyed on encoder category.
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