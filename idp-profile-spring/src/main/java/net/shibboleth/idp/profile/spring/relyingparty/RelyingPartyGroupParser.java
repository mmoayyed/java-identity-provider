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

package net.shibboleth.idp.profile.spring.relyingparty;

import java.util.List;
import java.util.Map;

import javax.xml.namespace.QName;

import net.shibboleth.idp.profile.spring.relyingparty.metadata.MetadataProviderParser;
import net.shibboleth.idp.relyingparty.impl.DefaultRelyingPartyConfigurationResolver;
import net.shibboleth.idp.spring.SpringSupport;
import net.shibboleth.utilities.java.support.xml.ElementSupport;

import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.ManagedList;
import org.springframework.beans.factory.xml.AbstractSingleBeanDefinitionParser;
import org.springframework.beans.factory.xml.ParserContext;
import org.w3c.dom.Element;

/**
 * TODO.
 * 
 * Parser for &lt;AnonymousRelyingParty&gt;<br/> This parser summons up two (TODO three) beans
 * a  {@link DefaultRelyingPartyConfigurationResolver} which deals with the RelyingParty bit 
 * of the file and a {@link TBD} which deals with the metadata and security configuration.  
 */
public class RelyingPartyGroupParser extends AbstractSingleBeanDefinitionParser {

    /** Element name. */
    public static final QName ELEMENT_NAME = new QName(RelyingPartyNamespaceHandler.NAMESPACE, "RelyingPartyGroup");

    /** {@inheritDoc} */
    @Override protected Class<DefaultRelyingPartyConfigurationResolver> getBeanClass(Element element) {
        return DefaultRelyingPartyConfigurationResolver.class;
    }

    /** {@inheritDoc} */
    @Override protected void doParse(Element element, ParserContext parserContext, BeanDefinitionBuilder builder) {
        super.doParse(element, parserContext, builder);
        final Map<QName, List<Element>> configChildren = ElementSupport.getIndexedChildElements(element);

        // All the Relying Parties
        final List<BeanDefinition> relyingParties =
                SpringSupport.parseCustomElements(configChildren.get(RelyingPartyParser.ELEMENT_NAME), parserContext);
        final List<BeanDefinition> defaultRps =
                SpringSupport.parseCustomElements(configChildren.get(DefaultRelyingPartyParser.ELEMENT_NAME),
                        parserContext);
        final List<BeanDefinition> anonRps =
                SpringSupport.parseCustomElements(configChildren.get(AnonymousRelyingPartyParser.ELEMENT_NAME),
                        parserContext);

        int count = 0;
        if (null != relyingParties) {
            count += relyingParties.size();
        }
        if (null != defaultRps) {
            count += defaultRps.size();
        }
        if (null != anonRps) {
            count += anonRps.size();
        }

        List<BeanDefinition> newList = new ManagedList<>(count);

        // Construct this list so that the default is at the end
        if (null != relyingParties) {
            for (final BeanDefinition defn : relyingParties) {
                newList.add(defn);
            }
        }
        if (null != anonRps) {
            for (final BeanDefinition defn : anonRps) {
                newList.add(defn);
            }
        }
        if (null != defaultRps) {
            for (final BeanDefinition defn : defaultRps) {
                newList.add(defn);
            }
        }
        builder.addPropertyValue("relyingPartyConfigurations", newList);

        // TODO Metadata
        SpringSupport.parseCustomElements(configChildren.get(MetadataProviderParser.ELEMENT_NAME), parserContext);

        // TODO Security
    }

    /** {@inheritDoc} */
    @Override protected boolean shouldGenerateId() {
        return true;
    }
}
