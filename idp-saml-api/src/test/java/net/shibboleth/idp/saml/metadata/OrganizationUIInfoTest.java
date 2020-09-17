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

import java.util.Locale;

import org.opensaml.core.testing.XMLObjectBaseTestCase;
import org.opensaml.core.xml.io.UnmarshallingException;
import org.opensaml.saml.saml2.metadata.Organization;
import org.testng.annotations.Test;

import net.shibboleth.utilities.java.support.xml.XMLParserException;

/**
 * test for the {@link OrganizationUIInfo}.
 */
@SuppressWarnings("javadoc")
public class OrganizationUIInfoTest extends XMLObjectBaseTestCase {
    
    @Test public void test() throws XMLParserException, UnmarshallingException {

        final Organization acs = unmarshallElement("/OrganizationUIInfo.xml", true);
        final OrganizationUIInfo info = new OrganizationUIInfo(acs); 

        assertEquals(info.getOrganizationNames().size(), 2);
        assertEquals(info.getOrganizationNames().get(Locale.forLanguageTag("en")), "org");
        assertEquals(info.getOrganizationDisplayNames().size(), 1);
        assertEquals(info.getOrganizationUrls().size(), 1);
    }

    @Test public void testBad() throws XMLParserException, UnmarshallingException {

        final Organization acs = unmarshallElement("/OrganizationUIInfoBad.xml", true);
        final OrganizationUIInfo info = new OrganizationUIInfo(acs); 

        assertEquals(info.getOrganizationNames().size(), 1);
        assertEquals(info.getOrganizationNames().get(Locale.forLanguageTag("en")), "OrgName");
        assertEquals(info.getOrganizationDisplayNames().size(), 1);
        assertEquals(info.getOrganizationUrls().size(), 1);
    }

}
