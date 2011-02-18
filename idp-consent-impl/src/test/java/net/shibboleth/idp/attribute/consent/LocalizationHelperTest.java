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

package net.shibboleth.idp.attribute.consent;

import static org.testng.AssertJUnit.assertEquals;
import static org.testng.AssertJUnit.assertNotNull;
import static org.testng.AssertJUnit.assertNull;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Locale;

import javax.annotation.Resource;

import net.shibboleth.idp.attribute.Attribute;

import org.opensaml.saml2.metadata.provider.MetadataProvider;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.testng.AbstractTestNGSpringContextTests;
import org.testng.annotations.Test;

/**
 * Tests LocalizationHelper.
 */

@ContextConfiguration("classpath:/consent-test-context.xml")
@Test(dataProviderClass = TestData.class)
public class LocalizationHelperTest extends AbstractTestNGSpringContextTests {

    @Resource(name = "consent.config.localizationHelper")
    private LocalizationHelper localizationHelper;

    public void configuredLocalizationHelper() {
        assertNotNull(localizationHelper);
    }

    public void selectLocale() {
        List<Locale> availableLocales = Arrays.asList(new Locale[] {Locale.ENGLISH, Locale.FRENCH, Locale.GERMAN});

        localizationHelper.setLocaleEnforcement(false);
        localizationHelper.setPreferredLocale(Locale.ENGLISH);
        assertEquals(Locale.GERMAN, localizationHelper.selectLocale(availableLocales, Locale.GERMAN));
        assertEquals(Locale.ENGLISH, localizationHelper.selectLocale(availableLocales, Locale.ITALIAN));

        localizationHelper.setPreferredLocale(Locale.CHINESE);
        assertEquals(Locale.GERMAN, localizationHelper.selectLocale(availableLocales, Locale.GERMAN));
        assertNull(localizationHelper.selectLocale(availableLocales, Locale.ITALIAN));

        localizationHelper.setLocaleEnforcement(true);
        localizationHelper.setPreferredLocale(Locale.ENGLISH);
        assertEquals(Locale.ENGLISH, localizationHelper.selectLocale(availableLocales, Locale.GERMAN));
        assertEquals(Locale.ENGLISH, localizationHelper.selectLocale(availableLocales, Locale.ITALIAN));

        localizationHelper.setPreferredLocale(Locale.CHINESE);
        assertNull(localizationHelper.selectLocale(availableLocales, Locale.GERMAN));
        assertNull(localizationHelper.selectLocale(availableLocales, Locale.ITALIAN));
    }

    @Test(dataProvider = "metadataProviderRelyingPartyId")
    public void getRelyingPartyNameAndDescription(MetadataProvider metadataProvider, String relyingPartyId) {

        localizationHelper.setPreferredLocale(Locale.ENGLISH);
        localizationHelper.setLocaleEnforcement(false);
        localizationHelper.setMetadataProvider(metadataProvider);
        assertEquals("german-name", localizationHelper.getRelyingPartyName(relyingPartyId, Locale.GERMAN));
        assertEquals("german-description", localizationHelper.getRelyingPartyDescription(relyingPartyId, Locale.GERMAN));

        localizationHelper.setLocaleEnforcement(true);
        assertEquals("english-name", localizationHelper.getRelyingPartyName(relyingPartyId, Locale.GERMAN));
        assertEquals("english-description",
                localizationHelper.getRelyingPartyDescription(relyingPartyId, Locale.GERMAN));

        assertEquals("english-name", localizationHelper.getRelyingPartyName(relyingPartyId, Locale.FRENCH));
        assertEquals("english-description",
                localizationHelper.getRelyingPartyDescription(relyingPartyId, Locale.FRENCH));

        localizationHelper.setPreferredLocale(Locale.CHINESE);
        assertEquals(relyingPartyId, localizationHelper.getRelyingPartyName(relyingPartyId, Locale.GERMAN));
        assertEquals("", localizationHelper.getRelyingPartyDescription(relyingPartyId, Locale.GERMAN));
    }

    @Test(dataProvider = "attributesLocaleLocale")
    public void getAttributeNameAndDescription(Collection<Attribute> attributes, Locale locale, Locale otherLocale) {
        localizationHelper.setLocaleEnforcement(false);

        for (Attribute attribute : attributes) {
            localizationHelper.setPreferredLocale(Locale.ENGLISH);
            String name = attribute.getId() + "-" + locale.getLanguage() + "-name";
            String description = attribute.getId() + "-" + locale.getLanguage() + "-description";

            assertEquals(name, localizationHelper.getAttributeName(attribute, locale));
            assertEquals(description, localizationHelper.getAttributeDescription(attribute, locale));

            localizationHelper.setPreferredLocale(Locale.CHINESE);
            assertEquals(attribute.getId(), localizationHelper.getAttributeName(attribute, otherLocale));
            assertEquals("", localizationHelper.getAttributeDescription(attribute, otherLocale));
        }
    }
}