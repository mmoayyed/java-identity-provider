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

package edu.internet2.middleware.shibboleth.common.config.service;

import org.opensaml.xml.util.DatatypeHelper;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.xml.ParserContext;
import org.w3c.dom.Element;

import edu.internet2.middleware.shibboleth.common.config.BaseReloadableService;

/**
 * Base bean definition parser for {@link BaseReloadableService}s.
 */
public abstract class AbstractReloadableServiceBeanDefinitionParser extends AbstractServiceBeanDefinitionParser {

    /** {@inheritDoc} */
    protected void doParse(Element configElement, ParserContext parserContext, BeanDefinitionBuilder builder) {
        super.doParse(configElement, parserContext, builder);

        if (configElement.hasAttributeNS(null, "configurationResourcePollingFrequency")
                && configElement.hasAttributeNS(null, "configurationResourcePollingRetryAttempts")) {
            
            builder.addConstructorArgReference(configElement.getAttributeNS(null, "timerId"));

            long frequency = Integer.parseInt(DatatypeHelper.safeTrimOrNullString(configElement.getAttributeNS(null,
                    "configurationResourcePollingFrequency"))) * 1000;
            builder.addConstructorArg(frequency);

            int retryAttempts = Integer.parseInt(DatatypeHelper.safeTrimOrNullString(configElement.getAttributeNS(null,
                    "configurationResourcePollingRetryAttempts")));
            builder.addConstructorArg(retryAttempts);
        }
    }
}