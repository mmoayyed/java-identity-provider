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

package net.shibboleth.idp.attribute.consent;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Random;
import java.util.UUID;

import net.shibboleth.idp.attribute.IdPAttribute;
import net.shibboleth.idp.attribute.IdPAttributeValue;
import net.shibboleth.idp.attribute.StringAttributeValue;

import org.cryptacular.util.CodecUtil;
import org.cryptacular.util.HashUtil;
import org.joda.time.DateTime;
import org.opensaml.saml.metadata.resolver.MetadataResolver;
import org.testng.annotations.DataProvider;

/**
 *
 */
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
        String[] attributes =
                {"surname", "givenName", "email", "telephoneNumber", "postalAddress", "mobile", "eduPersonAffiliation",
                        "eduPersonEntitlement"};
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
        return CodecUtil.hex(HashUtil.sha256(bytes), true);
    }

    private static DateTime getRandomDate() {
        // Fri, Jan 01 2010 12:00:00 UTC | Tue, Feb 02 2010 12:00:00 UTC | Wed, Mar 03 2010 12:00:00 UTC
        DateTime[] dates = {new DateTime(1262347200000L), new DateTime(1265112000000L), new DateTime(1267617600000L)};
        return dates[random.nextInt(dates.length)];
    }

    private static User getRandomUser() {
        return new User(getRandomUserId(), getRandomBoolean());
    }

    private static Collection<IdPAttributeValue<String>> getRandomAttributeValues() {
        Collection<IdPAttributeValue<String>> values = new HashSet<IdPAttributeValue<String>>();
        if (getRandomBoolean()) {
            // single value
            values.add(new StringAttributeValue(getRandomString()));
        } else {
            // multi values 2-3
            for (int i = 0; i < random.nextInt(2) + 2; i++) {
                values.add(new StringAttributeValue(getRandomString()));
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

    private static IdPAttribute getRandomAttribute() {
        IdPAttribute attribute = new IdPAttribute(getRandomAttributeId());
        attribute.setValues(getRandomAttributeValues());
        attribute.setDisplayNames(getRandomDisplayNames());
        attribute.setDisplayDescriptions(getRandomDisplayDescriptions());
        return attribute;
    }

    private static IdPAttribute getRandomNumberedAttribute() {
        return new IdPAttribute("attribute_" + (random.nextInt(9) + 1));
    }

    private static Collection<IdPAttribute> getRandomAttributes() {
        Map<String, IdPAttribute> attributes = new HashMap<String, IdPAttribute>();
        // 1-10
        for (int i = 0; i < random.nextInt(10) + 1; i++) {
            IdPAttribute attribute = getRandomAttribute();
            attributes.put(attribute.getId(), attribute);
        }
        return attributes.values();
    }

    private static Collection<IdPAttribute> getRandomNumberedAttributes() {
        Map<String, IdPAttribute> attributes = new HashMap<String, IdPAttribute>();
        // 0-10
        for (int i = 0; i < random.nextInt(10) + 1; i++) {
            IdPAttribute attribute = getRandomNumberedAttribute();
            attributes.put(attribute.getId(), attribute);
        }
        return attributes.values();
    }

    private static Collection<IdPAttribute> getRandomAttributesWithUserIdAttribute() {
        Collection<IdPAttribute> attributes = new HashSet<IdPAttribute>(getRandomNumberedAttributes());
        IdPAttribute userIdAttribute = new IdPAttribute("userId");
        userIdAttribute.setValues(Arrays.asList(new StringAttributeValue[] {new StringAttributeValue("userId-value")}));
        attributes.add(userIdAttribute);
        return attributes;
    }

    private static Collection<IdPAttribute> createLocalizedAttributes(Locale locale) {
        Map<String, IdPAttribute> attributes = new HashMap<String, IdPAttribute>();
        // 1-10
        for (int i = 0; i < random.nextInt(10) + 1; i++) {
            IdPAttribute attribute = getRandomAttribute();

            Map<Locale, String> displayNames = new HashMap<Locale, String>();
            displayNames.put(locale, attribute.getId() + "-" + locale.getLanguage() + "-name");
            attribute.setDisplayNames(displayNames);

            Map<Locale, String> displayDescriptions = new HashMap<Locale, String>();
            displayDescriptions.put(locale, attribute.getId() + "-" + locale.getLanguage() + "-description");
            attribute.setDisplayDescriptions(displayDescriptions);

            attributes.put(attribute.getId(), attribute);
        }
        return attributes.values();
    }

    private static MetadataResolver createMetadataProvider(String file) {
        //TODO this probably shouldn't be here, this should be taken care of by the tested class, not by this data provider class
 /*       try {
            InitializationService.initialize();
        } catch (InitializationException e) {
            Assert.fail("Initialization of OpenSAML failed");
        }

        FilesystemMetadataProvider metadataProvider = null;
        try {
            metadataProvider = new FilesystemMetadataProvider(new File(file));
            metadataProvider.setParserPool(new BasicParserPool());
            metadataProvider.initialize();
        } catch (MetadataProviderException e) {
            e.printStackTrace();
        }

        return metadataProvider; */
        return null;
    }

    @DataProvider(name = "userIdGlobalConsent")
    public static Object[][] userIdGlobalConsent() {
        return new Object[][] {new Object[] {getRandomUserId(), getRandomBoolean()}};
    }

    @DataProvider(name = "userRelyingPartyIdAttributeIdHashDate")
    public static Object[][] userRelyingPartyIdAttributeIdHashDate() {
        return new Object[][] {new Object[] {getRandomUser(), getRandomRelyingPartyId(), getRandomAttributeId(),
                getRandomHash(), getRandomDate()}};
    }

    @DataProvider(name = "attributes")
    public static Object[][] attributes() {
        return new Object[][] {new Object[] {getRandomAttributes()}};
    }

    @DataProvider(name = "numberedAttributes")
    public static Object[][] numberedAttributes() {
        return new Object[][] {new Object[] {getRandomNumberedAttributes()}};
    }

    @DataProvider(name = "attributesAttributesWithUserIdAttribute")
    public static Object[][] attributesAttributesWithUserIdAttribute() {
        return new Object[][] {new Object[] {getRandomAttributes(), getRandomAttributesWithUserIdAttribute()}};
    }

    @DataProvider(name = "attributesDate")
    public static Object[][] attributesDate() {
        return new Object[][] {new Object[] {getRandomAttributes(), getRandomDate()}};
    }

    @DataProvider(name = "attributesDateAttribute")
    public static Object[][] attributesDateAttribute() {
        return new Object[][] {new Object[] {getRandomAttributes(), getRandomDate(), getRandomAttribute()}};
    }

    @DataProvider(name = "userRelyingPartyIdAttributesDateAttributes")
    public static Object[][] userRelyingPartyIdAttributesDateAttributes() {
        return new Object[][] {new Object[] {getRandomUser(), getRandomRelyingPartyId(), getRandomAttributes(),
                getRandomDate(), getRandomAttributes()}};
    }

    @DataProvider(name = "metadataProviderRelyingPartyId")
    public static Object[][] metadataProviderRelyingPartyId() {
        return new Object[][] {new Object[] {createMetadataProvider("src/test/resources/sp-metadata.xml"),
                "https://sp.example.org/shibboleth"}};
    }

    @DataProvider(name = "attributesLocaleLocale")
    public static Object[][] attributesLocaleLocale() {
        return new Object[][] {new Object[] {createLocalizedAttributes(Locale.ENGLISH), Locale.ENGLISH, Locale.CHINESE}};
    }
}