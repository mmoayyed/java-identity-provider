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

import java.util.Date;
import java.util.HashSet;
import java.util.Random;
import java.util.Set;
import java.util.UUID;

import org.testng.annotations.DataProvider;

import edu.internet2.middleware.shibboleth.idp.consent.entities.Attribute;
import edu.internet2.middleware.shibboleth.idp.consent.entities.Principal;
import edu.internet2.middleware.shibboleth.idp.consent.entities.RelyingParty;
import edu.internet2.middleware.shibboleth.idp.consent.entities.TermsOfUse;


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
    
    public static String getRandomString() {
        return UUID.randomUUID().toString();
    }
    
    private static Date getRandomDate() {
        // Fri Jan 01 12:00:00 EST 2010 | Tue Feb 02 12:00:00 EST 2010 | Wed Mar 03 12:00:00 EST 2010
        Date[] dates = {new Date(1262365200000L), new Date(1265130000000L), new Date(1267635600000L)};
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
        Principal principal = new Principal();
        principal.setUniqueId(getRandomUniqueId());
        principal.setFirstAccess(getRandomDate());
        principal.setLastAccess(getRandomDate());
        principal.setGlobalConsent(false);
        return principal;
    }
    
    private static RelyingParty createRelyingParty() {
        RelyingParty relyingParty = new RelyingParty();
        relyingParty.setEntityId(getRandomEntityId());
        return relyingParty;
    }
          
    private static TermsOfUse createTermsOfUse() {
        TermsOfUse termsOfUse = new TermsOfUse();
        termsOfUse.setVersion(getRandomVersion());
        termsOfUse.setText("This an example ToU text");
        return termsOfUse;
    }
    
    private static Attribute createAttribute() {
        Attribute attribute = new Attribute();
        attribute.setId(getRandomAttributeId());
        attribute.addValue(getRandomString());
        return attribute;
    }
    
    private static Attribute createMultiValueAttribute() {
        Attribute attribute = new Attribute();
        attribute.setId(getRandomAttributeId());
        for (int i = 0; i < random.nextInt(3)+3; i++) {
            attribute.addValue(getRandomString());
        }
        return attribute;
    }
    
    private Set<Attribute> createSetOfAttributes() {
        Set<Attribute> attributes = new HashSet<Attribute>();
        for (int i = 0; i < random.nextInt(10)+5; i++) {
            if (random.nextBoolean()) {
                attributes.add(createAttribute());
            } else {
                attributes.add(createMultiValueAttribute());
            }
        }
        return attributes;
    }
    
   
    @DataProvider(name = "crudPrincipalTest")
    public static Object[][] createCrudPrincipalTest() {      
        return new Object[][] {
        new Object[] {createPrincipal()}
      };
    }
    
    @DataProvider(name = "crudRelyingPartyTest")
    public static Object[][] createCrudRelyingPartyTest() {        
        return new Object[][] {
        new Object[] {createRelyingParty()}
      };
    }
    
    @DataProvider(name = "crudAgreedTermsOfUseTest")
    public static Object[][] createCrudAgreedTermsOfUseTest() {         
        return new Object[][] {
        new Object[] {createPrincipal(), createTermsOfUse(), getRandomDate()}
      };
    }
    
    @DataProvider(name = "crudAttributeReleaseConsentTest")
    public static Object[][] crudAttributeReleaseConsentTest() {         
        return new Object[][] {
                new Object[] {createPrincipal(), createRelyingParty(), createAttribute(), getRandomDate()},
                new Object[] {createPrincipal(), createRelyingParty(), createMultiValueAttribute(), getRandomDate()}
      };
    }
    

    
    
    
  }
   