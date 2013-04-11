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

package net.shibboleth.idp.attribute.resolver.spring.ad;

import java.util.regex.Pattern;

import javax.xml.namespace.QName;

import net.shibboleth.idp.attribute.resolver.impl.ad.RegexSplitAttributeDefinition;
import net.shibboleth.utilities.java.support.primitive.StringSupport;
import net.shibboleth.utilities.java.support.xml.AttributeSupport;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.BeanCreationException;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.xml.ParserContext;
import org.w3c.dom.Element;

/**
 * Spring Bean Definition Parser for static data connector.
 */
public class RegexSplitAttributeDefinitionBeanDefinitionParser extends BaseAttributeDefinitionBeanDefinitionParser {

    /** Schema type name. */
    public static final QName TYPE_NAME = new QName(AttributeDefinitionNamespaceHandler.NAMESPACE, "RegexSplit");
    
    /** Logger.*/
    private final Logger log = LoggerFactory.getLogger(RegexSplitAttributeDefinitionBeanDefinitionParser.class); 

    /** {@inheritDoc} */
    protected Class getBeanClass(Element element) {
        return RegexSplitAttributeDefinition.class;
    }

    /** {@inheritDoc} */
    protected void doParse(Element config, ParserContext parserContext, BeanDefinitionBuilder builder) {
        super.doParse(config, parserContext, builder);

        final String regexp = StringSupport.trimOrNull(config.getAttributeNS(null, "regex"));
        
        if (null == regexp) {
            log.error("Attribute definition {}: No regexp specified", getDefinitionId());
            throw new BeanCreationException("Attribute definition " + getDefinitionId()
                    + ": No regexp text provided"); 
        }
        
        boolean caseSensitive = true;
        if (config.hasAttributeNS(null, "caseSensitive")) {
            caseSensitive =
                    AttributeSupport.getAttributeValueAsBoolean(config.getAttributeNodeNS(null, "caseSensitive"));
        }
        
        Pattern pattern;
        if (caseSensitive) {
            pattern = Pattern.compile(regexp);
        } else {
            pattern = Pattern.compile(regexp, Pattern.CASE_INSENSITIVE);
        }
        
        
        builder.addPropertyValue("regularExpression", pattern);
    }
}