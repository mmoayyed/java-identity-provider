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

package net.shibboleth.idp.saml.metadata;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;

import java.util.Locale;

import org.opensaml.core.testing.XMLObjectBaseTestCase;
import org.opensaml.core.xml.io.UnmarshallingException;
import org.opensaml.saml.saml2.metadata.AttributeConsumingService;
import org.testng.annotations.Test;

import net.shibboleth.utilities.java.support.xml.XMLParserException;

/**
 * test for the {@link ACSUIInfo}.
 */
@SuppressWarnings("javadoc")
public class ACSUIInfoTest extends XMLObjectBaseTestCase {

    @Test public void test() throws XMLParserException, UnmarshallingException {

        final AttributeConsumingService acs = unmarshallElement("/ACSUIInfo.xml", true);
        final ACSUIInfo info = new ACSUIInfo(acs); 

        assertEquals(info.getServiceNames().size(), 2);
        assertEquals(info.getServiceNames().get(Locale.forLanguageTag("en")), "ServiceName");
        assertEquals(info.getServiceDescriptions().size(), 1);        
    }

    @Test public void testBad() throws XMLParserException, UnmarshallingException {

        final AttributeConsumingService acs = unmarshallElement("/ACSUIInfoBad.xml", true);
        final ACSUIInfo info = new ACSUIInfo(acs); 

        assertTrue(info.getServiceNames().isEmpty());
        assertEquals(info.getServiceDescriptions().size(), 1);

    }
}
