/*
 * Licensed to the University Corporation for Advanced Internet Development, 
 * Inc. (UCAID) under one or more contributor license agreements.  See the 
 * NOTICE file distributed with this work for additional information regarding
 * copyright ownership. The UCAID licenses this file to You under the Apache 
 * License, Version 2.0 (the "License"); you may not use this file except in 
 * compliance with the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package net.shibboleth.idp.attribute.resolver.spring;

import java.util.List;

import javax.annotation.Nonnull;

import net.shibboleth.idp.spring.SpringSupport;
import net.shibboleth.utilities.java.support.primitive.StringSupport;
import net.shibboleth.utilities.java.support.xml.ElementSupport;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.xml.AbstractSingleBeanDefinitionParser;
import org.springframework.beans.factory.xml.ParserContext;
import org.w3c.dom.Element;

/** Bean definition parser for a {@link BaseResolverPlugin}. */
public abstract class BaseResolverPluginBeanDefinitionParser extends AbstractSingleBeanDefinitionParser {

    /** An Id for the definition, used for debugging messages. */
    private String defnId = "<Unnamed Attribute or Connector>";
    
    /** Class logger. */
    private final Logger log = LoggerFactory.getLogger(BaseResolverPluginBeanDefinitionParser.class);

    /** Helper for logging.
     * @return the definition ID
     */
    @Nonnull protected String getDefinitionId() {
        return defnId;
    }
    
    /** {@inheritDoc} */
    protected void doParse(Element config, ParserContext parserContext, BeanDefinitionBuilder builder) {
        String id = StringSupport.trimOrNull(config.getAttributeNS(null, "id"));
        log.info("Parsing configuration for {} plugin with id : {}", config.getLocalName(), id);
        builder.addPropertyValue("id", id);
        if (null != id) {
            defnId = id;
        }

        List<Element> dependencyElements =
                ElementSupport.getChildElements(config, ResolverPluginDependencyBeanDefinitionParser.ELEMENT_NAME);
        builder.addPropertyValue("dependencies", SpringSupport.parseCustomElements(dependencyElements, parserContext));
    }
}
