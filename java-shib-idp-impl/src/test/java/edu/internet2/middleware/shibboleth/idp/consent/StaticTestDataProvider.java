/*
 * Copyright 2010 University Corporation for Advanced Internet Development, Inc.
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

package edu.internet2.middleware.shibboleth.idp.consent;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Random;
import java.util.Set;
import java.util.UUID;

import org.joda.time.DateTime;
import org.testng.annotations.DataProvider;


import edu.internet2.middleware.shibboleth.idp.consent.components.TermsOfUse;
import edu.internet2.middleware.shibboleth.idp.consent.entities.AgreedTermsOfUse;
import edu.internet2.middleware.shibboleth.idp.consent.entities.Attribute;
import edu.internet2.middleware.shibboleth.idp.consent.entities.AttributeReleaseConsent;
import edu.internet2.middleware.shibboleth.idp.consent.entities.Principal;
import edu.internet2.middleware.shibboleth.idp.consent.entities.RelyingParty;
import edu.internet2.middleware.shibboleth.idp.consent.mock.IdPMock;


public class StaticTestDataProvider {
    
    private static final Random random = new Random();
    
    private static String getRandomUniqueId() {
        String uniqueId = "@example.org";
        int id = random.nextInt(100000) + 100000;
        return id + uniqueId;
    }
    
    private static String getRandomEntityId() {
        String pre = "https://sp";
        String post = ".example.org/shibboleth";
        int id = random.nextInt(100) + 100;
        return pre + id + post;
    }
        
    private static String getRandomAttributeId() {
        String[] attributes = {"surname", "givenName", "email", "telephoneNumber", "postalAddress", "mobile", "eduPersonAffiliation", "eduPersonEntitlement"}; 
        return attributes[random.nextInt(attributes.length)];
    }
    
    
    private static String getRandomString() {
        return UUID.randomUUID().toString();
    }
    
    
    private static DateTime getRandomDate() {
        // Fri Jan 01 12:00:00 EST 2010 | Tue Feb 02 12:00:00 EST 2010 | Wed Mar 03 12:00:00 EST 2010
    	DateTime[] dates = {new DateTime(1262365200000L), new DateTime(1265130000000L), new DateTime(1267635600000L)};
        return dates[random.nextInt(dates.length)];
    }
    
    private static Boolean getRandomBoolean() {
        return random.nextBoolean();
    }
    
    private static long getRandomId() {
        return random.nextInt(100000)+100000;
    }
    
    private static String getRandomVersion() {
        int i1 = random.nextInt(10)+1;
        int i2 = random.nextInt(10)+1;
        return i1 + "." + i2;
    }
    
    private static Principal createPrincipal() {
        Principal principal = new Principal(-1, getRandomUniqueId(), getRandomDate(), getRandomDate(), false);
        principal.setAgreedTermsOfUses(createAgreedTermsOfUses());
        return principal;
    }
    
    private static RelyingParty createRelyingParty() {
    	return new RelyingParty(-1, getRandomEntityId());
    }
          
    private static TermsOfUse createTermsOfUse() {
        return new TermsOfUse(getRandomVersion(), "This an example ToU text");
    }
    
    private static AgreedTermsOfUse createAgreedTermsOfUse() {
        return new AgreedTermsOfUse(createTermsOfUse(), getRandomDate());
    }

    private static Collection<AgreedTermsOfUse> createAgreedTermsOfUses() {
        Set<AgreedTermsOfUse> agreedTermsOfUses = new HashSet<AgreedTermsOfUse>();
        
        //for (int i = 0; i < random.nextInt(5)+5; i++) {

        for (int i = 0; i <2; i++) {
        	
        	AgreedTermsOfUse agreed = createAgreedTermsOfUse();
        	if (!agreedTermsOfUses.contains(agreed))
        		agreedTermsOfUses.add(agreed);
        }        
        return agreedTermsOfUses;
    }
    
    private static AttributeReleaseConsent createAttributeReleaseConsent() {
        Attribute attribute;
        if (random.nextBoolean()) {
        	attribute = createAttribute();
        } else {
            attribute = createMultiValueAttribute();
        }
        return new AttributeReleaseConsent(attribute, getRandomDate());
    }
    
    private static Collection<AttributeReleaseConsent> createAttributeReleaseConsents() {
        Set<AttributeReleaseConsent> attributeReleaseConsents = new HashSet<AttributeReleaseConsent>();
        for (int i = 0; i < random.nextInt(10)+10; i++) {
        	AttributeReleaseConsent consent = createAttributeReleaseConsent();
        	if (!attributeReleaseConsents.contains(consent))
        		attributeReleaseConsents.add(consent);
        }
        return attributeReleaseConsents;     
    }
    
    
    private static Attribute createAttribute() {
        Collection<String> values = new ArrayList<String>();
        values.add(getRandomString());
    	return new Attribute(getRandomAttributeId(), values);
    }
    
    private static Attribute createUniqueIdAttribute() {
        Collection<String> values = new ArrayList<String>();
        values.add(getRandomUniqueId());
    	return new Attribute("uniqueID", values);
    }
    
    private static Attribute createMultiValueAttribute() {
    	Collection<String> values = new ArrayList<String>();   	
        for (int i = 0; i < random.nextInt(5)+5; i++) {
        	values.add(getRandomString());
        }
        return new Attribute(getRandomAttributeId(), values);
    }
    
    private static Collection<Attribute> createAttributes() {
        Set<Attribute> attributes = new HashSet<Attribute>();
        for (int i = 0; i < random.nextInt(10)+10; i++) {
            if (random.nextBoolean()) {
                attributes.add(createAttribute());
            } else {
                attributes.add(createMultiValueAttribute());
            }
        }
                
        attributes.add(createUniqueIdAttribute());        
        return attributes;
    }
    
    private static IdPMock createIdPMock() {
        return new IdPMock(getRandomEntityId(), createAttributes());
    }
    
   
    @DataProvider(name = "crudPrincipalTest")
    public static Object[][] createCrudPrincipalTest() {      
        return new Object[][] {
        new Object[] {getRandomUniqueId(), getRandomDate(), getRandomDate()}
      };
    }
    
    @DataProvider(name = "crudRelyingPartyTest")
    public static Object[][] createCrudRelyingPartyTest() {        
        return new Object[][] {
        new Object[] {getRandomEntityId()}
      };
    }
    
    @DataProvider(name = "crudAgreedTermsOfUseTest")
    public static Object[][] createCrudAgreedTermsOfUseTest() {         
        return new Object[][] {
        new Object[] {getRandomUniqueId(), getRandomDate(), createTermsOfUse(), getRandomDate()}
      };
    }
    
    @DataProvider(name = "crudAttributeReleaseConsentTest")
    public static Object[][] crudAttributeReleaseConsentTest() {         
        return new Object[][] {
                new Object[] {getRandomUniqueId(), getRandomDate(), getRandomEntityId(), createAttribute(), getRandomDate()},
                new Object[] {getRandomUniqueId(), getRandomDate(), getRandomEntityId(), createMultiValueAttribute(), getRandomDate()}
      };
    }
    
    
    @DataProvider(name = "dummyIdP")
    public static Object[][] IdPMock() {         
        return new Object[][] {
                new Object[] {createIdPMock()}
      };
    }
    
    @DataProvider(name = "dummyIdPAndUniqueIdAndAttributeReleaseConsents")
    public static Object[][] idPMockAndPrincipalAndAttributeReleaseConsents() {         
        IdPMock dummyIdP = createIdPMock();
        String uniqueId = null;
        for (Attribute attribute : createAttributes()) {
            if (attribute.getId().equals("uniqueID")) {
            	uniqueId = attribute.getValues().iterator().next();
            }
        }
             
        return new Object[][] {
                new Object[] {dummyIdP, uniqueId, getRandomDate(), createAgreedTermsOfUses(), createAttributeReleaseConsents()}
      };
    }
    

	@DataProvider(name = "attributeList")
    public static Object[][] attributeList() {
        
        Collection<Attribute> attributes = new HashSet<Attribute>();
        for (int i = 0; i < random.nextInt(10) + 5; i++) {
            Attribute attribute = new Attribute("attribute_"+(random.nextInt(9) + 1), "someValueHash");
            attributes.add(attribute);
        }
        
        return new Object[][] {
                new Object[] {attributes}
      };
    }
    
  }
   