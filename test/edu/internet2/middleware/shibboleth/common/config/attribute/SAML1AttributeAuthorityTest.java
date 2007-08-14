/*
 * Copyright [2007] [University Corporation for Advanced Internet Development, Inc.]
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package edu.internet2.middleware.shibboleth.common.config.attribute;

import java.util.Map;

import org.opensaml.saml2.metadata.provider.HTTPMetadataProvider;
import org.opensaml.xml.parse.BasicParserPool;
import org.opensaml.xml.parse.ParserPool;
import org.springframework.context.ApplicationContext;

import edu.internet2.middleware.shibboleth.common.attribute.BaseAttribute;
import edu.internet2.middleware.shibboleth.common.attribute.provider.SAML1AttributeAuthority;
import edu.internet2.middleware.shibboleth.common.config.BaseConfigTestCase;
import edu.internet2.middleware.shibboleth.common.profile.provider.BaseSAMLProfileRequestContext;
import edu.internet2.middleware.shibboleth.common.relyingparty.RelyingPartyConfiguration;

/**
 * Unit tests for {@link SAML1AttributeAuthority}.
 */
public class SAML1AttributeAuthorityTest extends BaseConfigTestCase {

    public void testResolution() throws Exception {
        ApplicationContext ac = createSpringContext(DATA_PATH + "/config/attribute/service-config.xml");

        ParserPool parserPool = new BasicParserPool();

        HTTPMetadataProvider mdProvider = new HTTPMetadataProvider(
                "http://wayf.incommonfederation.org/InCommon/InCommon-metadata.xml", 5000);
        mdProvider.setParserPool(parserPool);
        mdProvider.initialize();

        RelyingPartyConfiguration rpConfig = new RelyingPartyConfiguration("mySP", "myIdP");

        BaseSAMLProfileRequestContext context = new BaseSAMLProfileRequestContext();
        context.setMetadataProvider(mdProvider);
        context.setRelyingPartyConfiguration(rpConfig);
        context.setPrincipalName("aUser");

        SAML1AttributeAuthority aa = (SAML1AttributeAuthority) ac.getBean("shibboleth.SAML1AttributeAuthority");
        Map<String, BaseAttribute> attributes = aa.getAttributes(context);

        assertEquals(3, attributes.size());
        
        assertNotNull(aa.buildAttributeStatement(null, attributes.values()));
    }
}