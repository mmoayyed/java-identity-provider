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

import net.shibboleth.idp.saml.profile.config.saml2.AttributeQueryProfileConfiguration;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.xml.ParserContext;
import org.w3c.dom.Element;

/**
 * Parser to generate {@link AttributeQueryProfileConfiguration} from a
 * <code>saml:SAML2ArtifactResolutionProfile</code>.
 */
public class SAML2AttributeQueryProfileParser extends BaseSAML2ProfileConfigurationParser {

    /** Element name. */
    public static final QName ELEMENT_NAME = new QName(RelyingPartySAMLNamespaceHandler.NAMESPACE,
            "SAML2AttributeQueryProfile");

    /** logger. */
    private Logger log = LoggerFactory.getLogger(SAML2ArtifactResolutionProfileParser.class);

    /** {@inheritDoc} */
    @Override protected Class<AttributeQueryProfileConfiguration> getBeanClass(Element element) {
        return AttributeQueryProfileConfiguration.class;
    }

    /** {@inheritDoc} */
    @Override protected String getArtifactResolutionServiceURLRef() {
        return "shibboleth.SAML2.AttributeQuery.ServiceURL";
    }

    /** {@inheritDoc} */
    @Override protected String getArtifactResolutionServiceIndexRef() {
        return "shibboleth.SAML2.AttributeQuery.ServiceIndex";
    }

    /** {@inheritDoc} */
    @Override protected String getSignResponsesDefault() {
        return "conditional";
    }

    /** {@inheritDoc} */
    @Override protected String getSignAssertionsDefault() {
        return "never";
    }

    /** {@inheritDoc} */
    @Override protected void doParse(Element element, ParserContext parserContext, BeanDefinitionBuilder builder) {
        super.doParse(element, parserContext, builder);

        if (element.hasAttributeNS(null, "securityPolicyRef")) {
            log.warn("I do not (yet) know how to deal with 'securityPolicyRef=\"{}\"'",
                    element.getAttributeNS(null, "securityPolicyRef"));
        }
    }

}
