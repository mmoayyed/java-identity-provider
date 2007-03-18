/*
 * Copyright [2005] [University Corporation for Advanced Internet Development, Inc.] Licensed under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except in compliance with the License. You may obtain a copy
 * of the License at http://www.apache.org/licenses/LICENSE-2.0 Unless required by applicable law or agreed to in
 * writing, software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS
 * OF ANY KIND, either express or implied. See the License for the specific language governing permissions and
 * limitations under the License.
 */

package edu.internet2.middleware.shibboleth.common.attribute.resolver.provider.dataConnector;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;

import edu.internet2.middleware.shibboleth.common.attribute.Attribute;
import edu.internet2.middleware.shibboleth.common.attribute.provider.BasicAttribute;
import edu.internet2.middleware.shibboleth.common.attribute.resolver.AttributeResolutionException;
import edu.internet2.middleware.shibboleth.common.attribute.resolver.provider.ShibbolethResolutionContext;

/**
 * <code>DataConnectorPlugIn</code> implementation that allows static values to be declared in the resolver
 * configuration.
 */
public class StaticDataConnector extends BaseDataConnector {

    /** Log4j logger. */
    private static Logger log = Logger.getLogger(StaticDataConnector.class.getName());

    /** Source Data. */
    private List<BasicAttribute<String>> sourceData;

    /** Constructor. */
    public StaticDataConnector() {
        sourceData = new ArrayList<BasicAttribute<String>>();
    }

    /** {@inheritDoc} */
    public Map<String, Attribute> resolve(ShibbolethResolutionContext resolutionContext) throws AttributeResolutionException {
        log.debug("Resolving connector: (" + getId() + ") for principal: (" + resolutionContext.getAttributeRequestContext().getPrincipalName()
                + ")");

        Map<String, Attribute> attributes = new HashMap<String, Attribute>();
        for (Attribute<String> a : sourceData) {
            BasicAttribute<String> newAttribute = new BasicAttribute<String>();
            newAttribute.setId(a.getId());

            for (String value : a.getValues()) {
                String newValue = value.replaceAll("%PRINCIPAL%", resolutionContext.getAttributeRequestContext().getPrincipalName());
                newAttribute.getValues().add(newValue);
            }

            attributes.put(newAttribute.getId(), newAttribute);
        }

        return attributes;
    }

    /**
     * Get the source attribute data.
     * 
     * @return list of source attributes.
     */
    public List<BasicAttribute<String>> getSourceData() {
        return sourceData;
    }

    /**
     * Replace the current list of source attributes with the given list.
     * 
     * @param newSourceData new data to replace existing data
     */
    public void setSourceData(List<BasicAttribute<String>> newSourceData) {
        sourceData.clear();
        for (BasicAttribute<String> attribute : newSourceData) {
            sourceData.add(attribute);
        }
    }

    /** {@inheritDoc} */
    public void validate() throws AttributeResolutionException {
        // Do nothing
    }
}