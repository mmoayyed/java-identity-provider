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

import static org.testng.AssertJUnit.fail;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.UUID;

import org.joda.time.DateTime;
import org.opensaml.DefaultBootstrap;
import org.opensaml.saml2.metadata.provider.FilesystemMetadataProvider;
import org.opensaml.saml2.metadata.provider.MetadataProvider;
import org.opensaml.saml2.metadata.provider.MetadataProviderException;
import org.opensaml.xml.ConfigurationException;
import org.opensaml.xml.parse.BasicParserPool;
import org.testng.annotations.DataProvider;

import edu.internet2.middleware.shibboleth.idp.consent.components.DescriptionBuilder;
import edu.internet2.middleware.shibboleth.idp.consent.components.TermsOfUse;
import edu.internet2.middleware.shibboleth.idp.consent.entities.AgreedTermsOfUse;
import edu.internet2.middleware.shibboleth.idp.consent.entities.Attribute;
import edu.internet2.middleware.shibboleth.idp.consent.entities.AttributeReleaseConsent;
import edu.internet2.middleware.shibboleth.idp.consent.entities.Principal;
import edu.internet2.middleware.shibboleth.idp.consent.entities.RelyingParty;
import edu.internet2.middleware.shibboleth.idp.consent.mock.BaseAttribute;
import edu.internet2.middleware.shibboleth.idp.consent.mock.IdPContext;


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

    private static Locale getRandomLocale() {
        Locale[] locales = {Locale.ENGLISH, Locale.GERMAN, Locale.FRENCH};
        return locales[random.nextInt(locales.length)];
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
        Map<String,AttributeReleaseConsent> attributeReleaseConsents = new HashMap<String,AttributeReleaseConsent>();
        for (int i = 0; i < random.nextInt(10)+10; i++) {
        	AttributeReleaseConsent consent = createAttributeReleaseConsent();
        	
        	if (!attributeReleaseConsents.containsKey(consent.getAttribute().getId()))
        		attributeReleaseConsents.put(consent.getAttribute().getId(),consent);
        }
        return attributeReleaseConsents.values();     
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
    
    private static Map<String, BaseAttribute<String>> createBaseAttributes(Collection<Attribute> attributes) {
        
        Map<String, BaseAttribute<String>> baseAttributes = new HashMap<String, BaseAttribute<String>>();     
        for (Attribute attribute : attributes) {
            BaseAttribute<String> baseAttribute = new BaseAttribute<String>(attribute.getId(), attribute.getValues());
            baseAttributes.put(attribute.getId(), baseAttribute);
        }
        Attribute uniqueIdAttribute = createUniqueIdAttribute();
        BaseAttribute<String> baseAttribute = new BaseAttribute<String>(uniqueIdAttribute.getId(), uniqueIdAttribute.getValues());
        baseAttributes.put(uniqueIdAttribute.getId(), baseAttribute);        
        return baseAttributes;
    }
    
    private static Map<String, BaseAttribute<String>> createBaseAttributesWithInfo(Collection<Attribute> attributes, Locale[] locales) {
        Map<String, BaseAttribute<String>> baseAttributes = createBaseAttributes(attributes);
        for(BaseAttribute<String> baseAttribute : baseAttributes.values()) {
            for (Locale locale : locales) {
                //if(getRandomBoolean()) {
                baseAttribute.setDisplayName(locale, baseAttribute.getId()+"-"+locale.getLanguage()+"-name");
                //}
                //if(getRandomBoolean()) {
                baseAttribute.setDisplayDescription(locale, baseAttribute.getId()+"-"+locale.getLanguage()+"-description");
                //}
            }
        }
        return baseAttributes;
    }
    
    private static IdPContext createIdPContext() {
        return new IdPContext(getRandomEntityId(), createBaseAttributes(createAttributes()));
    }
    
    private static String extractUniqueId(Map<String, BaseAttribute<String>> attributes) {
        for (String id : attributes.keySet()) {
            if (id.equals("uniqueID")) {
                return attributes.get(id).getValues().iterator().next();
            }
        }
        return null;
    }
    
    private static MetadataProvider createMetadataProvider(String xml) {
        try {
            DefaultBootstrap.bootstrap();
        } catch (ConfigurationException e) {
            e.printStackTrace();
        } 
        
        FilesystemMetadataProvider metadataProvider = null;
        try {
            metadataProvider = new FilesystemMetadataProvider(new File(xml));
            metadataProvider.setParserPool(new BasicParserPool());
            metadataProvider.initialize();
        } catch (MetadataProviderException e) {
            e.printStackTrace();
        }
        
        return metadataProvider; 
    }
    
    private static DescriptionBuilder createDefautDescriptionBuilder(boolean enforced) {
        return new DescriptionBuilder(Locale.ENGLISH, enforced);
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
    
    
    @DataProvider(name = "idpContext")
    public static Object[][] idpContext() {         
        return new Object[][] {
                new Object[] {createIdPContext()}
      };
    }
    
    @DataProvider(name = "idpContextAndUniqueIdAndAgreedTermsOfUses")
    public static Object[][] idpContextAndUniqueIdAndAgreedTermsOfUses() {         
        IdPContext idpContext = createIdPContext();
        String uniqueId = extractUniqueId(idpContext.getReleasedAttributes());           
        return new Object[][] {
                new Object[] {idpContext, uniqueId, getRandomDate(), createAgreedTermsOfUses()}
      };
    }
    
    @DataProvider(name = "idpContextAndUniqueIdAndAttributeReleaseConsents")
    public static Object[][] idpContextAndUniqueIdAndAttributeReleaseConsents() {         
        IdPContext idpContext = createIdPContext();
        String uniqueId = extractUniqueId(idpContext.getReleasedAttributes());  
             
        return new Object[][] {
                new Object[] {idpContext, uniqueId, getRandomDate(), createAttributeReleaseConsents()}
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
    
    @DataProvider(name = "attachRelyingPartyInfoEnforced")
    public static Object[][] attachRelyingPartyInfoEnforced() {
        DescriptionBuilder descriptionBuilder = createDefautDescriptionBuilder(true);
        MetadataProvider metadataProvider = createMetadataProvider("src/test/resources/sp-metadata.xml");
        descriptionBuilder.setMetadataProvider(metadataProvider);
        RelyingParty relyingParty = new RelyingParty(-1, "https://sp.example.org/shibboleth");
        return new Object[][] {
                new Object[] {descriptionBuilder, relyingParty}
      };
    }
    
    @DataProvider(name = "attachRelyingPartyInfoNotEnforced")
    public static Object[][] attachRelyingPartyInfoNotEnforced() {
        DescriptionBuilder descriptionBuilder = createDefautDescriptionBuilder(false);
        MetadataProvider metadataProvider = createMetadataProvider("src/test/resources/sp-metadata.xml");
        descriptionBuilder.setMetadataProvider(metadataProvider);
        RelyingParty relyingParty = new RelyingParty(-1, "https://sp.example.org/shibboleth");
        return new Object[][] {
                new Object[] {descriptionBuilder, relyingParty}
      };
    }
    
    @DataProvider(name = "attachAttributeInfoEnforced")
    public static Object[][] attachAttributeInfoEnforced() {
        Collection <Attribute> attributes = createAttributes();
        return new Object[][] {
                new Object[] {createDefautDescriptionBuilder(true), createBaseAttributesWithInfo(attributes, new Locale[] {Locale.ENGLISH, Locale.GERMAN}), attributes}
      };
    }
    
    @DataProvider(name = "attachAttributeInfoNotEnforced")
    public static Object[][] attachAttributeInfoNotEnforced() {
        Collection <Attribute> attributes = createAttributes();
        return new Object[][] {
                new Object[] {createDefautDescriptionBuilder(false), createBaseAttributesWithInfo(attributes, new Locale[] {Locale.ENGLISH, Locale.GERMAN}), attributes}
      };
    }

  }
   