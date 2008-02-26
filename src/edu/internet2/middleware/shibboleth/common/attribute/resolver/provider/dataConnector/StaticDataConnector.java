/*
 * Copyright [2005] [University Corporation for Advanced Internet Development, Inc.] Licensed under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except in compliance with the License. You may obtain a copy
 * of the License at http://www.apache.org/licenses/LICENSE-2.0 Unless required by applicable law or agreed to in
 * writing, software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS
 * OF ANY KIND, either express or implied. See the License for the specific language governing permissions and
 * limitations under the License.
 */

package edu.internet2.middleware.shibboleth.common.attribute.resolver.provider.dataConnector;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import edu.internet2.middleware.shibboleth.common.attribute.BaseAttribute;
import edu.internet2.middleware.shibboleth.common.attribute.resolver.AttributeResolutionException;
import edu.internet2.middleware.shibboleth.common.attribute.resolver.provider.ShibbolethResolutionContext;

/**
 * Data connector implementation that returns staticly defined attributes.
 */
public class StaticDataConnector extends BaseDataConnector {

    /** Source Data. */
    private Map<String, BaseAttribute> attributes;

    /**
     * Constructor.
     * 
     * @param staticAttributes attributes this data connector will return
     */
    public StaticDataConnector(List<BaseAttribute<String>> staticAttributes) {
        attributes = new HashMap<String, BaseAttribute>();
        if (staticAttributes != null) {            
            for (BaseAttribute<String> attribute : staticAttributes) {
                attributes.put(attribute.getId(), attribute);
            }
        }
    }

    /** {@inheritDoc} */
    public Map<String, BaseAttribute> resolve(ShibbolethResolutionContext resolutionContext)
            throws AttributeResolutionException {
        return attributes;
    }

    /** {@inheritDoc} */
    public void validate() throws AttributeResolutionException {
        // Do nothing
    }
}