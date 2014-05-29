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

import javax.xml.namespace.QName;

import net.shibboleth.idp.profile.logic.RelyingPartyIdPredicate;
import net.shibboleth.idp.saml.profile.logic.EntitiesDescriptorPredicate;
import net.shibboleth.utilities.java.support.primitive.StringSupport;

import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.xml.ParserContext;
import org.w3c.dom.Element;

import com.google.common.base.Predicates;

/**
 * Parser for the &lt:rp:relyingParty&gt; element.
 */
public class RelyingPartyParser extends AbstractRelyingPartyParser {

    /** Element name. */
    public static final QName ELEMENT_NAME = new QName(RelyingPartyNamespaceHandler.NAMESPACE, "RelyingParty");

    /**
     * {@inheritDoc} The construction of the activation Condition is more complicated than one might suppose. The
     * definition is that if the it matches the relyingPartyID *or* it matches the &lt;EntitiesDescriptor&gt;, then the
     * configuration matches. So we need to
     * {@link Predicates#or(com.google.common.base.Predicate, com.google.common.base.Predicate) a
     * {@link RelyingPartyIdPredicate} and a {@link EntitiesDescriptorPredicate} These however may have injected lookup
     * strategies and so these need to be constructed as a BeanDefinition.
     * */
    @Override protected void doParse(Element element, ParserContext parserContext, BeanDefinitionBuilder builder) {
        super.doParse(element, parserContext, builder);

        final String id = StringSupport.trimOrNull(element.getAttributeNS(null, "id"));
        builder.addPropertyValue("id", id);

        final BeanDefinitionBuilder rpPredicate =
                BeanDefinitionBuilder.genericBeanDefinition(RelyingPartyIdPredicate.class);
        rpPredicate.addConstructorArgValue(id);

        final BeanDefinitionBuilder egPredicate =
                BeanDefinitionBuilder.genericBeanDefinition(EntitiesDescriptorPredicate.class);
        egPredicate.addConstructorArgValue(id);

        BeanDefinitionBuilder orPredicate = BeanDefinitionBuilder.genericBeanDefinition(Predicates.class);
        orPredicate.setFactoryMethod("or");
        orPredicate.addConstructorArgValue(rpPredicate.getBeanDefinition());
        orPredicate.addConstructorArgValue(egPredicate.getBeanDefinition());

        builder.addPropertyValue("activationCondition", orPredicate.getBeanDefinition());
    }
    
}