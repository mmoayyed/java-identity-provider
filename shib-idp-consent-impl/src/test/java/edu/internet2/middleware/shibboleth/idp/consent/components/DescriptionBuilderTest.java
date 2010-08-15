/*
 * Copyright 2009 University Corporation for Advanced Internet Development, Inc.
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

package edu.internet2.middleware.shibboleth.idp.consent.components;

import java.util.Collection;
import java.util.Locale;
import java.util.Map;

import javax.annotation.Resource;

import org.springframework.beans.factory.annotation.Autowired;
import org.testng.annotations.Test;

import static org.testng.AssertJUnit.fail;
import static org.testng.AssertJUnit.assertEquals;
import static org.testng.AssertJUnit.assertNotNull;
import edu.internet2.middleware.shibboleth.common.attribute.BaseAttribute;
import edu.internet2.middleware.shibboleth.idp.consent.BaseTest;
import edu.internet2.middleware.shibboleth.idp.consent.UserConsentException;
import edu.internet2.middleware.shibboleth.idp.consent.entities.Attribute;
import edu.internet2.middleware.shibboleth.idp.consent.entities.RelyingParty;

/**
 * Tests AttributeList.
 */

@Test
public class DescriptionBuilderTest extends BaseTest {

    @Resource(name="descriptionBuilder")
    private DescriptionBuilder configuredDescriptionBuilder;
    
    @Test
    public void configuredDescriptionBuilder() {
        assertNotNull(configuredDescriptionBuilder);
    }
    
    @Test(dataProvider="attachRelyingPartyInfoEnforced")
    public void attachRelyingPartyInfoEnforcedLocale(DescriptionBuilder descriptionBuilder, RelyingParty relyingParty) {
         
        try {
            descriptionBuilder.attachDescription(relyingParty, Locale.GERMAN);
        } catch (UserConsentException e) {
            fail(e.getMessage());
        }
           
        assertEquals("english-name", relyingParty.getDisplayName());
        assertEquals("english-description", relyingParty.getDisplayDescription());
        
    }
    
    @Test(dataProvider="attachRelyingPartyInfoNotEnforced")
    public void attachRelyingPartyInfoUserLocale(DescriptionBuilder descriptionBuilder, RelyingParty relyingParty) {
        try {
            descriptionBuilder.attachDescription(relyingParty, Locale.GERMAN);
        } catch (UserConsentException e) {
            fail(e.getMessage());
        }
           
        assertEquals("german-name", relyingParty.getDisplayName());
        assertEquals("german-description", relyingParty.getDisplayDescription());
    }
    
    @Test(dataProvider="attachRelyingPartyInfoNotEnforced")
    public void attachRelyingPartyInfoPreferredLocale(DescriptionBuilder descriptionBuilder, RelyingParty relyingParty) {
        try {
            descriptionBuilder.attachDescription(relyingParty, Locale.FRENCH);
        } catch (UserConsentException e) {
            fail(e.getMessage());
        }
        
        assertEquals("english-name", relyingParty.getDisplayName());
        assertEquals("english-description", relyingParty.getDisplayDescription());
    }
    
    @Test(dataProvider="attachAttributeInfoEnforced")
    public void attachAttributeInfoEnforcedLocale(DescriptionBuilder descriptionBuilder, Map<String, BaseAttribute> baseAttributes, Collection<Attribute> attributes) {         
        try {
            descriptionBuilder.attachDescription(baseAttributes, attributes, Locale.GERMAN);
        } catch (UserConsentException e) {
            fail(e.getMessage());
        }      
        for (Attribute attribute : attributes) {
            assertEquals(attribute.getId()+"-"+Locale.ENGLISH.getLanguage()+"-name", attribute.getDisplayName());
            assertEquals(attribute.getId()+"-"+Locale.ENGLISH.getLanguage()+"-description", attribute.getDisplayDescription());
        }
        
    }
    
    @Test(dataProvider="attachAttributeInfoNotEnforced")
    public void attachAttributeInfoUserLocale(DescriptionBuilder descriptionBuilder, Map<String, BaseAttribute> baseAttributes, Collection<Attribute> attributes) {
        try {
            descriptionBuilder.attachDescription(baseAttributes, attributes, Locale.GERMAN);
        } catch (UserConsentException e) {
            fail(e.getMessage());
        }      
        for (Attribute attribute : attributes) {
            assertEquals(attribute.getId()+"-"+Locale.GERMAN.getLanguage()+"-name", attribute.getDisplayName());
            assertEquals(attribute.getId()+"-"+Locale.GERMAN.getLanguage()+"-description", attribute.getDisplayDescription());
        }
        
    }
    
    @Test(dataProvider="attachAttributeInfoNotEnforced")
    public void attachAttributeInfoPreferredLocale(DescriptionBuilder descriptionBuilder, Map<String, BaseAttribute> baseAttributes, Collection<Attribute> attributes) {
        try {
            descriptionBuilder.attachDescription(baseAttributes, attributes, Locale.FRENCH);
        } catch (UserConsentException e) {
            fail(e.getMessage());
        }
        for (Attribute attribute : attributes) {
            assertEquals(attribute.getId()+"-"+Locale.ENGLISH.getLanguage()+"-name", attribute.getDisplayName());
            assertEquals(attribute.getId()+"-"+Locale.ENGLISH.getLanguage()+"-description", attribute.getDisplayDescription());
        }
    }

}
