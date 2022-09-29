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

package net.shibboleth.idp.consent.logic.impl;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Function;

import jakarta.servlet.http.HttpServletRequest;

import net.shibboleth.idp.attribute.IdPAttribute;
import net.shibboleth.idp.attribute.transcoding.AttributeTranscoderRegistry;
import net.shibboleth.idp.attribute.transcoding.TranscodingRule;
import net.shibboleth.shared.component.ComponentInitializationException;
import net.shibboleth.shared.service.ReloadableService;
import net.shibboleth.shared.service.ServiceableComponent;

import org.springframework.mock.web.MockHttpServletRequest;
import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

/**
 * {@link AttributeDisplayNameFunction} and {@link AttributeDisplayDescriptionFunction} unit tests.
 */
@SuppressWarnings("javadoc")
public class AttributeDisplayNameDescriptionFunctionTest {
    
    private IdPAttribute testAttribute;

    private MockService service = new MockService();

    private final Map<Locale, String> names = new HashMap<>(3);

    private final Map<Locale, String> descriptions = new HashMap<>(3);

    @BeforeClass public void constructAttribute() {
        final IdPAttribute attr = new IdPAttribute("What");

        names.put(new Locale("en"), "EN locale Name");
        names.put(new Locale("fr"), "FR locale Name");
        names.put(new Locale("de"), "DE locale Name");

        descriptions.put(new Locale("en"), "EN locale Description");
        descriptions.put(new Locale("fr"), "FR locale Description");
        descriptions.put(new Locale("de"), "DE locale Description");

        testAttribute = attr;
    }
    
    private HttpServletRequest getMockRequest(String... languages) {
        
        final MockHttpServletRequest request = new MockHttpServletRequest();
        final List<Locale> locales = new ArrayList<>(languages.length);
        for (String language: languages) {
            locales.add(new Locale(language));
        }
        request.setPreferredLocales(locales);
        return request;
    }
    
    @Test public void testNameHttpOnly() {
        Function<IdPAttribute, String> func = new AttributeDisplayNameFunction(getMockRequest("fr", "de", "en"), null, service);
        Assert.assertEquals(func.apply(testAttribute), "FR locale Name");

        func = new AttributeDisplayNameFunction(getMockRequest("pt", "es"), null, service);
        Assert.assertEquals(func.apply(testAttribute), testAttribute.getId());
    }

    @Test public void testNameWithDefault() {
        List<String> fallback = List.of("en", "fr", "de");
        
        Function<IdPAttribute, String> func = new AttributeDisplayNameFunction(getMockRequest("fr", "de", "en"), fallback, service);
        Assert.assertEquals(func.apply(testAttribute), "FR locale Name");

        func = new AttributeDisplayNameFunction(getMockRequest("pt", "es"), fallback, service);
        Assert.assertEquals(func.apply(testAttribute), "EN locale Name");
    }

    @Test public void testDescHttpOnly() {
        Function<IdPAttribute, String> func = new AttributeDisplayDescriptionFunction(getMockRequest("fr", "de", "en"), null, service);
        Assert.assertEquals(func.apply(testAttribute), "FR locale Description");

        func = new AttributeDisplayDescriptionFunction(getMockRequest("pt", "es"), null, service);
        Assert.assertEquals(func.apply(testAttribute), testAttribute.getId());
    }

    @Test public void testDescWithDefault() {
        List<String> fallback = List.of("en", "fr", "de");
        
        Function<IdPAttribute, String> func = new AttributeDisplayDescriptionFunction(getMockRequest("fr", "de", "en"), fallback, service);
        Assert.assertEquals(func.apply(testAttribute), "FR locale Description");

        func = new AttributeDisplayDescriptionFunction(getMockRequest("pt", "es"), fallback, service);
        Assert.assertEquals(func.apply(testAttribute), "EN locale Description");
    }

    @Test public void testNullFallbackLanguage() {
        List<String> fallback = new ArrayList<>(2);
        fallback.add(null);
        fallback.add("");
        fallback.add("fr");
        
        Function<IdPAttribute, String> displayNameFunc = new AttributeDisplayNameFunction(
                getMockRequest("pt", "es"), fallback, service);
        Assert.assertEquals(displayNameFunc.apply(testAttribute), "FR locale Name");
        
        Function<IdPAttribute, String> descFunc = new AttributeDisplayDescriptionFunction(
                getMockRequest("pt", "es"), fallback, service);
        Assert.assertEquals(descFunc.apply(testAttribute), "FR locale Description");
    }

    private final class MockService implements
        ReloadableService<AttributeTranscoderRegistry>,
        ServiceableComponent<AttributeTranscoderRegistry>,
        AttributeTranscoderRegistry
    {

        /** {@inheritDoc} */
        public boolean isInitialized() {
            return true;
        }

        /** {@inheritDoc} */
        public void initialize() throws ComponentInitializationException {
        }

        /** {@inheritDoc} */
        public Instant getLastSuccessfulReloadInstant() {
            return null;
        }

        /** {@inheritDoc} */
        public Instant getLastReloadAttemptInstant() {
            return null;
        }

        /** {@inheritDoc} */
        public Throwable getReloadFailureCause() {
            return null;
        }

        /** {@inheritDoc} */
        public void reload() {
        }

        /** {@inheritDoc} */
        public ServiceableComponent<AttributeTranscoderRegistry> getServiceableComponent() {
            return this;
        }

        /** {@inheritDoc} */
        public String getId() {
            return null;
        }

        /** {@inheritDoc} */
        public Map<Locale, String> getDisplayNames(IdPAttribute attribute) {
            return names;
        }

        /** {@inheritDoc} */
        public Map<Locale, String> getDescriptions(IdPAttribute attribute) {
            return descriptions;
        }

        /** {@inheritDoc} */
        public Collection<TranscodingRule> getTranscodingRules(IdPAttribute from, Class<?> to) {
            return null;
        }

        /** {@inheritDoc} */
        public <T> Collection<TranscodingRule> getTranscodingRules(T from) {
            return null;
        }

        /** {@inheritDoc} */
        public AttributeTranscoderRegistry getComponent() {
            return this;
        }

        /** {@inheritDoc} */
        public void close() {
        }
    }
}
