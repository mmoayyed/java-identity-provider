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

import org.apache.log4j.Logger;

import edu.internet2.middleware.shibboleth.common.attribute.Attribute;
import edu.internet2.middleware.shibboleth.common.attribute.resolver.AttributeResolutionException;
import edu.internet2.middleware.shibboleth.common.attribute.resolver.provider.ShibbolethResolutionContext;

/**
 * Data connector implementation that returns staticly defined attributes.
 */
public class StaticDataConnector extends BaseDataConnector {

    /** Log4j logger. */
    private static Logger log = Logger.getLogger(StaticDataConnector.class.getName());

    /** Source Data. */
    private Map<String, Attribute> attributes;

    /**
     * Constructor.
     * 
     * @param staticAttributes attributes this data connector will return
     */
    public StaticDataConnector(List<Attribute<String>> staticAttributes) {
        if(staticAttributes != null){
            attributes = new HashMap<String, Attribute>();
            for (Attribute<String> attribute : staticAttributes) {
                attributes.put(attribute.getId(), attribute);
            }
        }
    }

    /** {@inheritDoc} */
    public Map<String, Attribute> resolve(ShibbolethResolutionContext resolutionContext)
            throws AttributeResolutionException {
        log.debug("Resolving connector: (" + getId() + ") for principal: ("
                + resolutionContext.getAttributeRequestContext().getPrincipalName() + ")");

        return attributes;
    }

    /** {@inheritDoc} */
    public void validate() throws AttributeResolutionException {
        // Do nothing
    }
}