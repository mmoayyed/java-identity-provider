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
package edu.internet2.middleware.shibboleth.common.config;

import edu.internet2.middleware.shibboleth.common.storage.Resource;

/**
 * A configurator that reads XML in order to configure another object.
 * 
 * The XML to load is expressed by a {@link Resource}.  An optional XPath 2.0
 * expression may be given to identify the root configuration element to use.  If 
 * not XPath expression is given then the document element is used.
 */
public interface XMLConfigurator<TargetType> extends Configurator<TargetType> {
    
    /**
     * Gets the configuration XML resource.
     * 
     * @return configuration XML resource
     */
    public String getConfigurationResource();
    
    /**
     * Sets the configuration XML resource.
     * 
     * @param configuration configuration XML resource
     */
    public void setConfigurationResource(Resource configuration);

    /**
     * Gets the XPath expression used to identify the beginning configuration element.
     * 
     * @return XPath expression used to identify the beginning configuration element
     */
    public String getConfigurationElementLocation();
    
    /**
     * Sets the XPath expression used to identify the beginning configuration element.
     * 
     * @param xpathLocation XPath expression used to identify the beginning configuration element
     */
    public void setConfigurationElementLocation(String xpathLocation);
}