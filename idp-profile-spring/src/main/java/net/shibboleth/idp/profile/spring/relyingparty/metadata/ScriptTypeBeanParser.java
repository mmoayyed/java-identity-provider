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

package net.shibboleth.idp.profile.spring.relyingparty.metadata;


import javax.annotation.Nonnull;
import net.shibboleth.utilities.java.support.primitive.StringSupport;
import net.shibboleth.utilities.java.support.xml.ElementSupport;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.w3c.dom.Element;

/**
 * Parser for elements derived from ScriptType in the various namespaces.
 * 
 * <p>The actual bean type is a runtime class so that different objects adhering to the general factory
 * contracts used with scripted beans will work.</p>
 */
public final class ScriptTypeBeanParser {

    /** Logger. */
    @Nonnull private static final Logger LOG = LoggerFactory.getLogger(ScriptTypeBeanParser.class);

    /** Private c'tor. */
    private ScriptTypeBeanParser() {
        
    }

    /**
     * Parse and return a builder for a bean adhering to the contract of ScriptedType.
     * 
     * @param type type of object to build
     * @param element root of XML configuration
     * 
     * @return bean definition builder
     */
    @Nonnull public static BeanDefinitionBuilder parseScriptType(@Nonnull final Class<?> type,
            @Nonnull final Element element) {
        
        final BeanDefinitionBuilder builder = BeanDefinitionBuilder.genericBeanDefinition(type);
        
        if (element.hasAttributeNS(null, "language")) {
            final String scriptLanguage = StringSupport.trimOrNull(element.getAttributeNS(null, "language"));
            builder.addConstructorArgValue(scriptLanguage);
        }
        final String customRef = StringSupport.trimOrNull(element.getAttributeNS(null, "customObjectRef"));
        if (null != customRef) {
            builder.addPropertyReference("customObject", customRef);
        }
        final Element scriptChild = ElementSupport.getFirstChildElement(element);
        builder.addConstructorArgValue(ElementSupport.getElementContentAsString(scriptChild));
        if (ElementSupport.isElementNamed(scriptChild, element.getNamespaceURI(), "Script")) {
            builder.setFactoryMethod("inlineScript");
        } else if (ElementSupport.isElementNamed(scriptChild, element.getNamespaceURI(), "ScriptFile")) {
            builder.setFactoryMethod("resourceScript");
        }
        
        return builder;
    }
    
}