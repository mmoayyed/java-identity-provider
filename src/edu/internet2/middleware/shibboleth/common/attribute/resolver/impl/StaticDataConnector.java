/*
 * Copyright [2005] [University Corporation for Advanced Internet Development, Inc.] Licensed under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except in compliance with the License. You may obtain a copy
 * of the License at http://www.apache.org/licenses/LICENSE-2.0 Unless required by applicable law or agreed to in
 * writing, software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS
 * OF ANY KIND, either express or implied. See the License for the specific language governing permissions and
 * limitations under the License.
 */

package edu.internet2.middleware.shibboleth.common.attribute.resolver.impl;

import java.util.List;

import javolution.util.FastList;

import org.apache.log4j.Logger;

import edu.internet2.middleware.shibboleth.common.attribute.Attribute;
import edu.internet2.middleware.shibboleth.common.attribute.impl.BaseAttribute;
import edu.internet2.middleware.shibboleth.common.attribute.resolver.AttributeResolutionException;
import edu.internet2.middleware.shibboleth.common.attribute.resolver.ResolutionContext;

/**
 * <code>DataConnectorPlugIn</code> implementation that allows static values to be declared in the resolver
 * configuration.
 */
public class StaticDataConnector extends BaseDataConnector {

    /** Log4j logger. */
    private static Logger log = Logger.getLogger(StaticDataConnector.class.getName());

    /** Source Data. */
    private List<BaseAttribute<String>> sourceData;

    /** Constructor. */
    public StaticDataConnector() {
        sourceData = new FastList<BaseAttribute<String>>();
    }

    /** {@inheritDoc} */
    public List<Attribute> resolve(ResolutionContext resolutionContext) throws AttributeResolutionException {
        log.debug("Resolving connector: (" + getId() + ")");
        log.debug(getId() + " resolving for principal: (" + resolutionContext.getPrincipalName() + ")");

        List<Attribute> attributes = new FastList<Attribute>();
        for (Attribute<String> a : sourceData) {
            BaseAttribute<String> newAttribute = new BaseAttribute<String>();
            newAttribute.setId(a.getId());

            for (String value : a.getValues()) {
                String newValue = value.replaceAll("%PRINCIPAL%", resolutionContext.getPrincipalName());
                newAttribute.getValues().add(newValue);
            }

            attributes.add(newAttribute);
        }

        return attributes;
    }

    /**
     * Get the source attribute data.
     * 
     * @return list of source attributes.
     */
    public List<BaseAttribute<String>> getSourceData() {
        return sourceData;
    }

    /** {@inheritDoc} */
    public void validate() throws AttributeResolutionException {
        // Do nothing
    }
}