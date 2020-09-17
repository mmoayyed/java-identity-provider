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
import static org.testng.Assert.assertNotNull;

import java.util.Locale;

import org.opensaml.core.testing.XMLObjectBaseTestCase;
import org.opensaml.saml.ext.saml2mdui.UIInfo;
import org.testng.annotations.Test;

/**
 * test for the {@link IdPUIInfo}.
 */
@SuppressWarnings("javadoc")
public class IdPUIInfoTest extends XMLObjectBaseTestCase {

    @Test public void test() {

        final UIInfo samluiinfo = unmarshallElement("/UIInfo.xml");
        final IdPUIInfo uiInfo = new IdPUIInfo(samluiinfo);

        assertEquals(uiInfo.getDisplayNames().size(), 2);
        assertNotNull(uiInfo.getDisplayNames().get(Locale.forLanguageTag("en-us")));
        assertEquals(uiInfo.getDescriptions().size(), 1);
        assertEquals(uiInfo.getKeywords().size(), 1);
        assertEquals(uiInfo.getLocaleLogos().size(), 1);
        assertEquals(uiInfo.getLocaleLogos().get(Locale.forLanguageTag("en")).size(), 2);
        assertEquals(uiInfo.getNonLocaleLogos().size(), 1);
        assertEquals(uiInfo.getInformationURLs().size(), 1);
        assertEquals(uiInfo.getPrivacyStatementURLs().size(), 1);
    }
    @Test public void testBad() {

        final UIInfo samluiinfo = unmarshallElement("/UIInfoBad.xml");
        final IdPUIInfo uiInfo = new IdPUIInfo(samluiinfo);

        assertEquals(uiInfo.getDisplayNames().size(), 1);
        assertEquals(uiInfo.getDescriptions().size(), 1);
        assertEquals(uiInfo.getKeywords().size(), 2); // one of which empty
        assertEquals(uiInfo.getLocaleLogos().size(), 1); 
        assertEquals(uiInfo.getNonLocaleLogos().size(), 2);
        assertEquals(uiInfo.getInformationURLs().size(), 1);
        assertEquals(uiInfo.getPrivacyStatementURLs().size(), 1);
    }
}
