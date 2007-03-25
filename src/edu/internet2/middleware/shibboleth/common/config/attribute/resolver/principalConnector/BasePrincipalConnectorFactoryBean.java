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

package edu.internet2.middleware.shibboleth.common.config.attribute.resolver.principalConnector;

import edu.internet2.middleware.shibboleth.common.config.attribute.resolver.AbstractResolutionPluginFactoryBean;

/**
 * Base Spring factory bean that produces principal connectors.
 */
public abstract class BasePrincipalConnectorFactoryBean extends AbstractResolutionPluginFactoryBean {

    /** Format of the NameID the connector operates on. */
    private String nameIdFormat;

    /**
     * Gets the format of the NameID the connector operates on.
     * 
     * @return format of the NameID the connector operates on
     */
    public String getNameIdFormat() {
        return nameIdFormat;
    }

    /**
     * Sets the format of the NameID the connector operates on.
     * 
     * @param format format of the NameID the connector operates on
     */
    public void setNameIdFormat(String format) {
        nameIdFormat = format;
    }
}