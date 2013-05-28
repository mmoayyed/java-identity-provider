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

package net.shibboleth.idp.attribute.filter.spring;

import net.shibboleth.idp.attribute.filter.Matcher;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.support.AbstractBeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.xml.ParserContext;
import org.w3c.dom.Element;

// TODO incomplete v2 port
/**
 * Spring bean definition parser to configure an {@link Matcher}.
 */
public abstract class MatcherParser extends BaseFilterParser {

    /** Class logger. */
    private final Logger log = LoggerFactory.getLogger(MatcherParser.class);

    /**
     * {@inheritDoc}
     * 
     * Calculate the qualified id once, and set both the id property as well as a qualified id metadata attribute used
     * by the resolveId() method.
     */
    // TODO verify qualified id creation and storage
    protected void doParse(Element element, ParserContext parserContext, BeanDefinitionBuilder builder) {
        super.doParse(element, parserContext, builder);

        String id = null;

        if (element.hasAttributeNS(null, "id")) {
            id = element.getAttributeNS(null, "id");
        } else {
            // TODO logging
            log.warn("Attribute filter elements should contain an 'id' attribute. This is not currently required but will be in future versions.");
            id = getQualifiedId(element, element.getLocalName(), element.getAttributeNS(null, "id"));
        }

        // Set id property.
        builder.addPropertyValue("id", id);

        // Set qualifiedId metadata used later by resolveId().
        builder.getBeanDefinition().setAttribute("qualifiedId", id);
    }

    /** {@inheritDoc} */
    protected String
            resolveId(Element configElement, AbstractBeanDefinition beanDefinition, ParserContext parserContext) {
        return beanDefinition.getAttribute("qualifiedId").toString();
    }
}