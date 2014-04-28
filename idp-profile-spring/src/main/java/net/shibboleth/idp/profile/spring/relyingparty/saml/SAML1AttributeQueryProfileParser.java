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

package net.shibboleth.idp.profile.spring.relyingparty.saml;

import javax.xml.namespace.QName;

import net.shibboleth.idp.saml.saml1.profile.config.AttributeQueryProfileConfiguration;
import net.shibboleth.utilities.java.support.primitive.StringSupport;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.w3c.dom.Element;

/**
 Parser to generate {@link AttributeQueryProfileConfiguration} from a
 * <code>saml:SAML1AttributeQueryProfile</code>.
 */
public class SAML1AttributeQueryProfileParser extends BaseSAMLProfileConfigurationParser {

    /** Element name. */
    public static final QName ELEMENT_NAME = new QName(RelyingPartySAMLNamespaceHandler.NAMESPACE,
            "SAML1AttributeQueryProfile");

    /** logger. */
    private Logger log = LoggerFactory.getLogger(SAML1AttributeQueryProfileParser.class);

    /** {@inheritDoc} */
    @Override
    protected Class<AttributeQueryProfileConfiguration> getBeanClass(Element element) {
        return AttributeQueryProfileConfiguration.class;
    }
    
    /** {@inheritDoc} */
    @Override protected String getProfileBeanNamePrefix() {
        return "shibboleth.SAML1.AttributeQuery.";
    }

    /** {@inheritDoc} */
    @Override
    protected String getSignResponsesDefault() {
        return "conditional";
    }

    /** {@inheritDoc} */
    @Override
    protected String getSignAssertionsDefault() {
        return "never";
    }
    
    /** {@inheritDoc} */
    @Override
    protected void doParse(Element element, BeanDefinitionBuilder builder) {
        super.doParse(element, builder);

        if (element.hasAttributeNS(null, "attributeAuthority")) {
            final String attributeAuthority =
                    StringSupport.trimOrNull(element.getAttributeNS(null, "attributeAuthority"));
            if (null != attributeAuthority && !"shibboleth.SAML1AttributeAuthority".equals(attributeAuthority)) {
                log.warn("Non default value for attributeAuthority of '{}' has been ignored", attributeAuthority);
            }
        }
        
        if (element.hasAttributeNS(null, "securityPolicyRef")) {
            //TODO
            log.warn("I do not (yet) know how to deal with 'securityPolicyRef=\"{}\"'",
                    element.getAttributeNS(null, "securityPolicyRef"));
        }

    }

}
