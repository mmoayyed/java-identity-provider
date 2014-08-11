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

package net.shibboleth.idp.ui.context;

import java.util.Arrays;

import org.opensaml.core.xml.XMLObjectBaseTestCase;
import org.opensaml.saml.saml2.metadata.AttributeConsumingService;
import org.opensaml.saml.saml2.metadata.EntityDescriptor;
import org.opensaml.saml.saml2.metadata.SPSSODescriptor;
import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

/**
 *
 */
public class RelyingPartyUIContextTest extends XMLObjectBaseTestCase {

    private EntityDescriptor theEntity;

    private SPSSODescriptor theSPSSO;

    private AttributeConsumingService theACS;

    private RelyingPartyUIContext getContext() {
        RelyingPartyUIContext result = new RelyingPartyUIContext();
        result.setRPAttributeConsumingService(theACS);
        result.setRPEntityDescriptor(theEntity);
        result.setRPSPSSODescriptor(theSPSSO);
        result.setBrowserLanguages(Arrays.asList("en", "fr"));
        return result;
    }

    @BeforeClass public void setup() {
        theEntity = unmarshallElement("/net/shibboleth/idp/ui/example-metadata.xml");
        theSPSSO = theEntity.getSPSSODescriptor("urn:oasis:names:tc:SAML:2.0:protocol");
        theACS = theSPSSO.getDefaultAttributeConsumingService();
        Assert.assertNotNull(theEntity);
        Assert.assertNotNull(theSPSSO);
        Assert.assertNotNull(theACS);
    }

    @Test public void givenName() {
        RelyingPartyUIContext ctx = getContext();

        Assert.assertEquals(ctx.getGivenName("administrative", null), "GNadministrative");
        Assert.assertEquals(ctx.getGivenName("billing", null), "GNbilling");
        Assert.assertEquals(ctx.getGivenName("other", "fallback"), "fallback");
        Assert.assertEquals(ctx.getGivenName("support", null), "GNsupport");
        Assert.assertEquals(ctx.getGivenName("technical", null), "GNtechnical");
    }

    @Test public void surName() {
        RelyingPartyUIContext ctx = getContext();

        Assert.assertEquals(ctx.getSurName("administrative", null), "Admin");
        Assert.assertEquals(ctx.getSurName("other", null), "OTHER");
        Assert.assertEquals(ctx.getSurName("billing", "Bond, James Bond"), "Bond, James Bond");
    }

    @Test public void email() {
        RelyingPartyUIContext ctx = getContext();

        Assert.assertEquals(ctx.getEmail("administrative", null), "mailto:administrative@example.org");
        Assert.assertEquals(ctx.getEmail("other", "http://example.org/"), "http://example.org/");
        Assert.assertEquals(ctx.getEmail("support", "http://example.org/DangerWillRoberts"),
                "http://example.org/DangerWillRoberts");
    }

    @Test public void organizationXX() {
        RelyingPartyUIContext ctx = getContext();

        Assert.assertEquals(ctx.getOrganizationDisplayName("DefODN"), "The Shibboleth Consortium");
        Assert.assertEquals(ctx.getOrganizationName("DefODN"), "DefODN");
        Assert.assertEquals(ctx.getOrganizationURL("DefOURL"), "DefOURL");
    }

    @Test public void service() {
        RelyingPartyUIContext ctx = getContext();

        Assert.assertEquals(ctx.getServiceName("DefaultServiceName"), "le Service Name");
        Assert.assertEquals(ctx.getServiceDescription("DefaultServiceName"), "The ServiceDescription");

        ctx = new RelyingPartyUIContext();
        ctx.setRPEntityDescriptor((EntityDescriptor)unmarshallElement("/net/shibboleth/idp/ui/example-metadata2.xml"));
        ctx.setBrowserLanguages(Arrays.asList("en", "fr"));
        Assert.assertEquals(ctx.getServiceName("DefaultServiceName"), "sp.example.org");
        
        ctx = new RelyingPartyUIContext();
        ctx.setRPEntityDescriptor((EntityDescriptor)unmarshallElement("/net/shibboleth/idp/ui/example-metadata3.xml"));
        ctx.setBrowserLanguages(Arrays.asList("en", "fr"));
        Assert.assertEquals(ctx.getServiceName("DefaultServiceName"), "urn:sp.example.org");

    }
}
