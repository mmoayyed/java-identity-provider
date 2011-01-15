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

import java.io.File;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Random;
import java.util.UUID;

import org.joda.time.DateTime;
import org.opensaml.DefaultBootstrap;
import org.opensaml.saml2.metadata.provider.FilesystemMetadataProvider;
import org.opensaml.saml2.metadata.provider.MetadataProvider;
import org.opensaml.saml2.metadata.provider.MetadataProviderException;
import org.opensaml.xml.ConfigurationException;
import org.opensaml.xml.parse.BasicParserPool;
import org.testng.annotations.DataProvider;

import edu.internet2.middleware.shibboleth.idp.attribute.Attribute;
import edu.vt.middleware.crypt.digest.SHA256;
import edu.vt.middleware.crypt.util.HexConverter;

public class TestData {
    
    private static final Random random = new Random();
    
    private static String getRandomString() {
        return String.valueOf(UUID.randomUUID());
    }
    
    private static String getRandomUserId() {
        return getRandomString();
    }
    
    private static String getRandomRelyingPartyId() {
        String pre = "https://sp";
        String post = ".example.org/shibboleth";
        int id = random.nextInt(100) + 100;
        return pre + id + post;
    }
        
    private static String getRandomAttributeId() {
        String[] attributes = {"surname", "givenName", "email", "telephoneNumber", "postalAddress", "mobile", "eduPersonAffiliation", "eduPersonEntitlement"}; 
        return attributes[random.nextInt(attributes.length)];
    }
    
    private static Boolean getRandomBoolean() {
        return random.nextBoolean();
    }
    
    private static Locale getRandomLocale() {
        Locale[] locales = {Locale.ENGLISH, Locale.GERMAN, Locale.FRENCH};
        return locales[random.nextInt(locales.length)];
    }
    
    private static String getRandomHash() {
        byte[] bytes = new byte[4096];
        random.nextBytes(bytes);
        return new SHA256().digest(bytes, new HexConverter(true));
    }
    
    private static DateTime getRandomDate() {
        // Fri, Jan 01 2010 12:00:00 UTC | Tue, Feb 02 2010 12:00:00 UTC | Wed, Mar 03 2010 12:00:00 UTC
        DateTime[] dates = {new DateTime(1262347200000L), new DateTime(1265112000000L), new DateTime(1267617600000L)};
        return dates[random.nextInt(dates.length)];
    }
    
    private static User getRandomUser() {
        return new User(getRandomUserId(), getRandomBoolean());
    }
    
    private static Collection<String> getRandomAttributeValues() {
        Collection<String> values = new HashSet<String>();
        if (getRandomBoolean()) {
            // single value
            values.add(getRandomString());
        } else {
            // multi values 2-3
            for (int i = 0; i < random.nextInt(2)+2; i++) {
                values.add(getRandomString());
            }
        }
        return values;
    }
    
    private static Map<Locale, String> getRandomDisplayNames() {
        Map<Locale, String> displayNames = new HashMap<Locale, String>();
        // 0-2
        for (int i = 0; i < random.nextInt(3); i++) {
            displayNames.put(getRandomLocale(), getRandomString());
        }
        return displayNames;
    }
    
    private static Map<Locale, String> getRandomDisplayDescriptions() {
        Map<Locale, String> displayDescriptions = new HashMap<Locale, String>();
        // 0-2
        for (int i = 0; i < random.nextInt(3); i++) {
            displayDescriptions.put(getRandomLocale(), getRandomString());
        }
        return displayDescriptions;
    }
    
    private static Attribute<?> getRandomAttribute() {
        Attribute<String> attribute = new Attribute<String>(getRandomAttributeId());
        attribute.getValues().addAll(getRandomAttributeValues());        
        attribute.getDisplayNames().putAll(getRandomDisplayNames());
        attribute.getDisplayDescriptions().putAll(getRandomDisplayDescriptions());
        return attribute;
    }
    
    private static Attribute<?> getRandomNumberedAttribute() {
        return new Attribute("attribute_"+(random.nextInt(9) + 1));
    }
    
    private static Collection<Attribute<?>> getRandomAttributes() {
        Map<String, Attribute<?>> attributes = new HashMap<String, Attribute<?>>();
        // 1-10
        for (int i = 0; i < random.nextInt(10)+1; i++) {
            Attribute<?> attribute = getRandomAttribute();
            attributes.put(attribute.getId(), attribute);
        }
        return attributes.values();
    }
    
    private static Collection<Attribute<?>> getRandomNumberedAttributes() {
        Map<String, Attribute<?>> attributes = new HashMap<String, Attribute<?>>();
        // 0-10
        for (int i = 0; i < random.nextInt(10)+1; i++) {
            Attribute<?> attribute = getRandomNumberedAttribute();
            attributes.put(attribute.getId(), attribute);
        }
        return attributes.values();
    }
    
    private static Collection<Attribute<?>> getRandomAttributesWithUserIdAttribute() {
        Collection<Attribute<?>> attributes = new HashSet<Attribute<?>>(getRandomNumberedAttributes());
        Attribute<String> userIdAttribute = new Attribute<String>("userId");
        userIdAttribute.getValues().add("userId-value");        
        attributes.add(userIdAttribute);
        return attributes;
    }
    
    private static Collection<Attribute<?>> createLocalizedAttributes(Locale locale) {
        Map<String, Attribute<?>> attributes = new HashMap<String, Attribute<?>>();
        // 1-10
        for (int i = 0; i < random.nextInt(10)+1; i++) {
            Attribute<?> attribute = getRandomAttribute();
            attribute.getDisplayNames().put(locale, attribute.getId()+"-"+locale.getLanguage()+"-name");
            attribute.getDisplayDescriptions().put(locale, attribute.getId()+"-"+locale.getLanguage()+"-description");
            attributes.put(attribute.getId(), attribute);
        }
        return attributes.values();
    }
    
    private static MetadataProvider createMetadataProvider(String file) {
        try {
            DefaultBootstrap.bootstrap();
        } catch (ConfigurationException e) {
            e.printStackTrace();
        } 
        
        FilesystemMetadataProvider metadataProvider = null;
        try {
            metadataProvider = new FilesystemMetadataProvider(new File(file));
            metadataProvider.setParserPool(new BasicParserPool());
            metadataProvider.initialize();
        } catch (MetadataProviderException e) {
            e.printStackTrace();
        }
        
        return metadataProvider; 
    }

    @DataProvider(name = "userIdGlobalConsent")
    public static Object[][] userIdGlobalConsent() {      
        return new Object[][] {
        new Object[] {getRandomUserId(), getRandomBoolean()}
      };
    }
    
    @DataProvider(name = "userRelyingPartyIdAttributeIdHashDate")
    public static Object[][] userRelyingPartyIdAttributeIdHashDate() {      
        return new Object[][] {
        new Object[] {getRandomUser(), getRandomRelyingPartyId(), getRandomAttributeId(), getRandomHash(), getRandomDate()}
      };
    }
    
    @DataProvider(name = "attributes")
    public static Object[][] attributes() {      
        return new Object[][] {
        new Object[] {getRandomAttributes()}
      };
    }
    
    @DataProvider(name = "numberedAttributes")
    public static Object[][] numberedAttributes() {      
        return new Object[][] {
        new Object[] {getRandomNumberedAttributes()}
      };
    }
    
    @DataProvider(name = "attributesAttributesWithUserIdAttribute")
    public static Object[][] attributesAttributesWithUserIdAttribute() {      
        return new Object[][] {
        new Object[] {getRandomAttributes(), getRandomAttributesWithUserIdAttribute()}
      };
    }
    
    
    @DataProvider(name = "attributesDate")
    public static Object[][] attributesDate() {      
        return new Object[][] {
        new Object[] {getRandomAttributes(), getRandomDate()}
      };
    }
    
    @DataProvider(name = "attributesDateAttribute")
    public static Object[][] attributesDateAttribute() {      
        return new Object[][] {
        new Object[] {getRandomAttributes(), getRandomDate(), getRandomAttribute()}
      };
    }
    
    @DataProvider(name = "userRelyingPartyIdAttributesDateAttributes")
    public static Object[][] userRelyingPartyIdAttributesDateAttributes() {      
        return new Object[][] {
        new Object[] {getRandomUser(), getRandomRelyingPartyId(), getRandomAttributes(), getRandomDate(), getRandomAttributes()}
      };
    }
    
    @DataProvider(name = "metadataProviderRelyingPartyId")
    public static Object[][] metadataProviderRelyingPartyId() {      
        return new Object[][] {
        new Object[] {createMetadataProvider("src/test/resources/sp-metadata.xml"), "https://sp.example.org/shibboleth"}
      };
    }
    
    @DataProvider(name = "attributesLocaleLocale")
    public static Object[][] attributesLocaleLocale() {      
        return new Object[][] {
        new Object[] {createLocalizedAttributes(Locale.ENGLISH), Locale.ENGLISH, Locale.CHINESE}
      };
    }
}   