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

package net.shibboleth.idp.attribute.filter.spring.matcher;

import javax.annotation.Nonnull;

import net.shibboleth.idp.attribute.filter.MatcherFromPolicy;
import net.shibboleth.idp.attribute.filter.PolicyFromMatcher;
import net.shibboleth.idp.attribute.filter.PolicyFromMatcherId;
import net.shibboleth.idp.attribute.filter.spring.BaseFilterParser;

import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.xml.ParserContext;
import org.w3c.dom.Element;

/**
 * Base function for all Attribute Value matchers. <br/>
 * 
 * This function takes care of the bean nesting needed to convert the bean (which is a natural matcher) into the correct
 * type. Specifically:<br/>
 * <table>
 * <tr>
 * <td>PolicyRule With Id</td>
 * <td>Nest inside a {@link PolicyFromMatcherId}</td>
 * </tr>
 * <tr>
 * <td>PolicyRule No Id</td>
 * <td>Nest inside a {@link PolicyFromMatcher}</td>
 * </tr>
 * <tr>
 * <td>Attribute Not Id</td>
 * <td>Native</td>
 * </tr>
 * <tr>
 * <td>Attribute ID Id</td>
 * <td>Nest inside a {@link PolicyFromMatcherId} inside a {@link MatcherFromPolicy}</td>
 * </tr>
 * </table>
 */
public abstract class BaseAttributeValueMatcherParser extends BaseFilterParser {

    /**
     * Helper function to determine if the Attribute Matcher has the attribute Id Specified. This influences decisions
     * bioth in parsing and in which bean to summon.
     * 
     * @param configElement the config element to inspect
     * @return whether here is a an attribute Id
     */
    protected boolean hasAttributeId(@Nonnull final Element configElement) {
        return configElement.hasAttributeNS(null, "attributeId");
    }

    /** {@inheritDoc} The table at the top describes the precise work. */
    @Nonnull protected Class getBeanClass(@Nonnull final Element element) {
        if (isPolicyRule(element)) {
            if (hasAttributeId(element)) {
                return PolicyFromMatcherId.class;
            } else {
                return PolicyFromMatcher.class;
            }
        } else {
            if (hasAttributeId(element)) {
                return MatcherFromPolicy.class;
            } else {
                return getNativeBeanClass();
            }
        }
    }

    /**
     * Parse bean definition. If needs be we inject it into a parent bean (or two). {@inheritDoc}
     */
    protected void doParse(@Nonnull final Element element, @Nonnull final ParserContext parserContext,
            @Nonnull final BeanDefinitionBuilder builder) {
        super.doParse(element, parserContext, builder);

        final String myId = builder.getBeanDefinition().getAttribute("qualifiedId").toString();

        builder.addPropertyValue("id", myId);

        if (isPolicyRule(element)) {
            BeanDefinitionBuilder childBuilder = BeanDefinitionBuilder.genericBeanDefinition(getNativeBeanClass());

            doNativeParse(element, parserContext, childBuilder);
            childBuilder.addPropertyValue("id", "PMId:" + myId);

            builder.addConstructorArgValue(childBuilder.getBeanDefinition());
            if (hasAttributeId(element)) {
                builder.addConstructorArgValue(element.getAttributeNS(null, "attributeId"));
            }
        } else if (hasAttributeId(element)) {
            // Bean inside PolicyFromMatcherId inside MatcherFromPolicy
            BeanDefinitionBuilder childBuilder = BeanDefinitionBuilder.genericBeanDefinition(PolicyFromMatcherId.class);
            BeanDefinitionBuilder grandChildBuilder = BeanDefinitionBuilder.genericBeanDefinition(getNativeBeanClass());

            doNativeParse(element, parserContext, grandChildBuilder);
            grandChildBuilder.addPropertyValue("id", "PMId:" + myId);

            childBuilder.addPropertyValue("id", "MfP:" + myId);
            childBuilder.addConstructorArgValue(grandChildBuilder.getBeanDefinition());
            childBuilder.addConstructorArgValue(element.getAttributeNS(null, "attributeId"));

            builder.addConstructorArgValue(childBuilder.getBeanDefinition());

        } else {
            doNativeParse(element, parserContext, builder);
        }
    }

    /**
     * Method return the native Matcher implementation.
     * 
     * @return the class.
     */
    @Nonnull protected abstract Class getNativeBeanClass();

    /**
     * Parse the native bean class. This is either called direct or then injected into the nesting class.
     * 
     * @param element the config
     * @param parserContext the context
     * @param builder the builder
     */
    protected abstract void doNativeParse(Element element, ParserContext parserContext, BeanDefinitionBuilder builder);
}