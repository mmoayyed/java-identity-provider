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

import net.shibboleth.idp.profile.spring.relyingparty.metadata.MetadataNamespaceHandler;
import net.shibboleth.idp.profile.spring.relyingparty.security.SecurityNamespaceHandler;
import net.shibboleth.idp.relyingparty.impl.DefaultRelyingPartyConfigurationResolver;
import net.shibboleth.idp.saml.metadata.impl.RelyingPartyMetadataProvider;
import net.shibboleth.idp.spring.SpringSupport;
import net.shibboleth.utilities.java.support.xml.ElementSupport;

import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.xml.AbstractSingleBeanDefinitionParser;
import org.springframework.beans.factory.xml.ParserContext;
import org.w3c.dom.Element;

/**
 * Parser for &lt;AnonymousRelyingParty&gt;<br/>
 * This parser summons up two beans: a {@link DefaultRelyingPartyConfigurationResolver} which deals with the
 * RelyingParty bit of the file, a series of {@link RelyingPartyMetadataProvider}s which deal with the metadata
 * configuration and a series of {@link TBD} which deals with the security configuration.
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
        builder.setLazyInit(true);
        builder.setInitMethodName("initialize");
        builder.setDestroyMethodName("destroy");
        

        builder.addPropertyValue("id", "RelyingPartyGroup["
                + parserContext.getReaderContext().getResource().getFilename()+"]");

        // All the Relying Parties
        final List<BeanDefinition> relyingParties =
                SpringSupport.parseCustomElements(configChildren.get(RelyingPartyParser.ELEMENT_NAME), parserContext);
        if (null != relyingParties && relyingParties.size() > 0) {
            builder.addPropertyValue("relyingPartyConfigurations", relyingParties);
        }
        final List<BeanDefinition> defaultRps =
                SpringSupport.parseCustomElements(configChildren.get(DefaultRelyingPartyParser.ELEMENT_NAME),
                        parserContext);
        builder.addPropertyValue("defaultConfiguration", defaultRps.get(0));

        final List<BeanDefinition> anonRps =
                SpringSupport.parseCustomElements(configChildren.get(AnonymousRelyingPartyParser.ELEMENT_NAME),
                        parserContext);
        builder.addPropertyValue("anonymousConfiguration", anonRps.get(0));
        
        //
        // The default security is specified in an external bean file.
        //
        builder.addPropertyReference("securityConfigurationMap", "shibboleth.default.security.configuration.map");

        // Metadata

        final List<BeanDefinition> metadataProviders =
                SpringSupport.parseCustomElements(configChildren.get(MetadataNamespaceHandler.METADATA_ELEMENT_NAME),
                        parserContext);

        if (metadataProviders != null && metadataProviders.size() > 0) {
            for (BeanDefinition metadataProvider : metadataProviders) {
                final BeanDefinitionBuilder metadataBuilder =
                        BeanDefinitionBuilder.genericBeanDefinition(RelyingPartyMetadataProvider.class);
                metadataBuilder.setLazyInit(true);
                metadataBuilder.setInitMethodName("initialize");
                metadataBuilder.setDestroyMethodName("destroy");

                metadataBuilder.setInitMethodName("initialize");
                metadataBuilder.setDestroyMethodName("destroy");
                metadataBuilder.addConstructorArgValue(metadataProvider);
                BeanDefinition rpDefinition = metadataBuilder.getBeanDefinition();
                parserContext.getRegistry().registerBeanDefinition(
                        parserContext.getReaderContext().generateBeanName(rpDefinition), rpDefinition);
            }
        }

        // <Credential> (for metadata & signing)
        SpringSupport.parseCustomElements(configChildren.get(SecurityNamespaceHandler.CREDENTIAL_ELEMENT_NAME),
                parserContext);
        // TODO Security
        // <TrustEngine> (for metadata)
        // <SecurityPolicy> (warn and ignore).

    }

    /** {@inheritDoc} */
    @Override protected boolean shouldGenerateId() {
        return true;
    }
}
