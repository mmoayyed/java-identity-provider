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

package net.shibboleth.idp.consent.logic;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import net.shibboleth.idp.attribute.IdPAttribute;

import org.springframework.mock.web.MockHttpServletRequest;
import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.google.common.base.Function;
import com.google.common.collect.Lists;

/**
 * test for {@link AttributeDisplayNameFunction}.
 */
public class AttributeDisplayNameFunctionTest {
    
    private IdPAttribute testAttribute;

    @BeforeClass public void constructAttribute() {
        final IdPAttribute attr = new IdPAttribute("What");

        final Map<Locale, String> names = new HashMap<>(3);
        names.put(new Locale("en"), "EN locale Name");
        names.put(new Locale("fr"), "FR locale Name");
        names.put(new Locale("de"), "DE locale Name");

        final Map<Locale, String> descriptions = new HashMap<>(3);
        descriptions.put(new Locale("en"), "EN locale Description");
        descriptions.put(new Locale("fr"), "FR locale Description");
        descriptions.put(new Locale("de"), "DE locale Description");
        
        attr.setDisplayNames(names);
        attr.setDisplayDescriptions(descriptions);
        
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
    
    @Test public void testHttpOnly() {
        Function<IdPAttribute, String> func = new AttributeDisplayNameFunction(getMockRequest("fr", "de", "en"), null);
        Assert.assertEquals(func.apply(testAttribute), "FR locale Name");

        func = new AttributeDisplayNameFunction(getMockRequest("pt", "es"), null);
        Assert.assertEquals(func.apply(testAttribute), testAttribute.getId());
    }

    @Test public void testWithDefault() {
        List<String> fallback = Lists.newArrayList("en", "fr", "de");
        
        Function<IdPAttribute, String> func = new AttributeDisplayNameFunction(getMockRequest("fr", "de", "en"), fallback);
        Assert.assertEquals(func.apply(testAttribute), "FR locale Name");

        func = new AttributeDisplayNameFunction(getMockRequest("pt", "es"), fallback);
        Assert.assertEquals(func.apply(testAttribute), "EN locale Name");
    }

}
