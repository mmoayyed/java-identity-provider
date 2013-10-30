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

package net.shibboleth.idp.attribute.resolver.spring.dc;

import java.util.ArrayList;
import java.util.List;

import javax.xml.namespace.QName;

import net.shibboleth.idp.attribute.IdPAttribute;
import net.shibboleth.idp.attribute.StringAttributeValue;
import net.shibboleth.idp.attribute.resolver.impl.dc.StaticDataConnector;
import net.shibboleth.utilities.java.support.xml.ElementSupport;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.xml.ParserContext;
import org.w3c.dom.Element;

/** Bean definition Parser for a {@link StaticDataConnector}. */
public class StaticDataConnectorParser extends AbstractDataConnectorParser {

    /** Schema type name. */
    public static final QName TYPE_NAME = new QName(DataConnectorNamespaceHandler.NAMESPACE, "Static");

    /** Local name of attribute. */
    public static final QName ATTRIBUTE_ELEMENT_NAME = new QName(DataConnectorNamespaceHandler.NAMESPACE, "Attribute");

    /** Class logger. */
    private final Logger log = LoggerFactory.getLogger(StaticDataConnectorParser.class);

    /** {@inheritDoc} */
    protected Class getBeanClass(Element element) {
        return StaticDataConnector.class;
    }

    /** {@inheritDoc} */
    protected void doParse(Element config, ParserContext parserContext, BeanDefinitionBuilder builder) {
        super.doParse(config, parserContext, builder);

        List<Element> children = ElementSupport.getChildElements(config, ATTRIBUTE_ELEMENT_NAME);
        List<IdPAttribute> attributes = new ArrayList<IdPAttribute>();
        for (Element child : children) {
            String attrId = child.getAttributeNS(null, "id");
            IdPAttribute attribute = new IdPAttribute(attrId);
            List<Element> values =
                    ElementSupport.getChildElementsByTagNameNS(child, DataConnectorNamespaceHandler.NAMESPACE, "Value");
            for (Element val : values) {
                StringAttributeValue av = new StringAttributeValue(val.getTextContent());
                log.trace("{} Attribute: {}, adding value {} ", new Object[] {getLogPrefix(), attrId,
                        av.getValue(),});
                attribute.getValues().add(av);
            }
            log.debug("{} Adding Attribute: {} with {} values", new Object[] {getLogPrefix(), attrId,
                    attribute.getValues().size(),});
            attributes.add(attribute);
        }

        builder.addPropertyValue("values", attributes);
    }

}