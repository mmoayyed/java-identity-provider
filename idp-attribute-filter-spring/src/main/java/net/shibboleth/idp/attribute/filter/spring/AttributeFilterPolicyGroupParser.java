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

import java.util.List;
import java.util.Map;

import javax.annotation.Nullable;
import javax.xml.namespace.QName;

import net.shibboleth.idp.attribute.filter.AttributeFilterImpl;
import net.shibboleth.idp.spring.SpringSupport;
import net.shibboleth.utilities.java.support.primitive.StringSupport;
import net.shibboleth.utilities.java.support.xml.ElementSupport;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.xml.AbstractSingleBeanDefinitionParser;
import org.springframework.beans.factory.xml.ParserContext;
import org.w3c.dom.Element;

/**
 * Bean definition parser for &lt;afp:AttributeFilterPolicyGroup&gt;, top top level of the filter "stack". <br.>
 * 
 * There is no bean being summoned up here. Rather we just parse all the children. Then over in the service all the *
 * {@link net.shibboleth.idp.attribute.filter.AttributeFilterPolicy} beans are sucked out of spring by type and injected
 * into a new {@link net.shibboleth.idp.attribute.filter.AttributeFilter}.
 */
public class AttributeFilterPolicyGroupParser extends AbstractSingleBeanDefinitionParser {
    /** Element name. */
    public static final QName ELEMENT_NAME = new QName(AttributeFilterNamespaceHandler.NAMESPACE,
            "AttributeFilterPolicyGroup");

    /** Schema type name. */
    public static final QName TYPE_NAME = new QName(AttributeFilterNamespaceHandler.NAMESPACE,
            "AttributeFilterPolicyGroupType");

    /** Local name of the policy requirement element. */
    public static final QName POLICY_REQUIREMENT_ELEMENT_NAME = new QName(AttributeFilterNamespaceHandler.NAMESPACE,
            "PolicyRequirement");

    /** Local name of the value filter element. */
    public static final QName PERMIT_VALUE_ELEMENT_NAME = new QName(AttributeFilterNamespaceHandler.NAMESPACE,
            "PermitValue");

    /** Class logger. */
    private final Logger log = LoggerFactory.getLogger(AttributeFilterPolicyGroupParser.class);

    /** {@inheritDoc} */
    protected Class<AttributeFilterImpl> getBeanClass(@Nullable Element element) {
        return AttributeFilterImpl.class;
    }

    /** {@inheritDoc} */
    protected void doParse(Element config, ParserContext context, BeanDefinitionBuilder builder) {

        String policyId = StringSupport.trimOrNull(config.getAttributeNS(null, "id"));

        log.debug("Parsing attribute filter policy group {}", policyId);

        List<Element> children;
        Map<QName, List<Element>> childrenMap = ElementSupport.getIndexedChildElements(config);

        //
        // Top level definitions
        //
        
        children = childrenMap.get(new QName(AttributeFilterNamespaceHandler.NAMESPACE, "PolicyRequirementRule"));
        SpringSupport.parseCustomElements(children, context);

        children = childrenMap.get(new QName(AttributeFilterNamespaceHandler.NAMESPACE, "AttributeRule"));
        SpringSupport.parseCustomElements(children, context);

        children = childrenMap.get(new QName(AttributeFilterNamespaceHandler.NAMESPACE, "PermitValueRule"));
        SpringSupport.parseCustomElements(children, context);

        children = childrenMap.get(new QName(AttributeFilterNamespaceHandler.NAMESPACE, "DenyValueRule"));
        SpringSupport.parseCustomElements(children, context);

        //
        // The actual policies
        //
        children = childrenMap.get(new QName(AttributeFilterNamespaceHandler.NAMESPACE, "AttributeFilterPolicy"));
        
        builder.addConstructorArgValue(policyId);
        builder.addConstructorArgValue(SpringSupport.parseCustomElements(children, context));
    }
}