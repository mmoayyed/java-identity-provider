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

import org.opensaml.Configuration;
import org.opensaml.saml1.core.AttributeStatement;
import org.opensaml.xml.io.Marshaller;
import org.opensaml.xml.util.XMLHelper;
import org.springframework.context.ApplicationContext;
import org.w3c.dom.Element;

import edu.internet2.middleware.shibboleth.common.attribute.SAML1AttributeAuthority;
import edu.internet2.middleware.shibboleth.common.attribute.provider.ShibbolethAttributeRequestContext;
import edu.internet2.middleware.shibboleth.common.config.BaseConfigTestCase;

/**
 * Unit tests for {@link SAML1AttributeAuthority}.
 */
public class SAML1AttributeAuthorityTest extends BaseConfigTestCase {

    /** Application Context. */
    private ApplicationContext ac;

    /** {@inheritDoc} */
    public void setUp() throws Exception {
        super.setUp();
        String[] configs = { "shibboleth-2.0-config-internal.xml",
                "data/edu/internet2/middleware/shibboleth/common/config/resolver/resolver-db.xml", };

        ac = createSpringContext(configs);
    }

    public void testResolution() throws Exception {
        SAML1AttributeAuthority aa = (SAML1AttributeAuthority) ac.getBean("shibboleth.SAML1AttributeAuthority");

        ShibbolethAttributeRequestContext requestContext = new ShibbolethAttributeRequestContext();
        requestContext.setPrincipalName("ptracy");

        AttributeStatement attributes = aa.performAttributeQuery(requestContext);

        Marshaller marshaller = Configuration.getMarshallerFactory().getMarshaller(attributes);
        Element asElem = marshaller.marshall(attributes);
        System.out.println(XMLHelper.nodeToString(asElem));
    }
}