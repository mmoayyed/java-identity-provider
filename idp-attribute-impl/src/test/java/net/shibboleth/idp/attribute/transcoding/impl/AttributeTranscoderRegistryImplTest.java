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

package net.shibboleth.idp.attribute.transcoding.impl;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Function;

import static org.testng.Assert.*;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.google.common.base.Predicates;

import net.shibboleth.ext.spring.testing.MockApplicationContext;
import net.shibboleth.idp.attribute.AttributeDecodingException;
import net.shibboleth.idp.attribute.AttributeEncodingException;
import net.shibboleth.idp.attribute.EmptyAttributeValue;
import net.shibboleth.idp.attribute.IdPAttribute;
import net.shibboleth.idp.attribute.StringAttributeValue;
import net.shibboleth.idp.attribute.transcoding.AttributeTranscoder;
import net.shibboleth.idp.attribute.transcoding.AttributeTranscoderRegistry;
import net.shibboleth.idp.attribute.transcoding.AttributeTranscoderRegistry.NamingFunction;
import net.shibboleth.idp.attribute.transcoding.BasicNamingFunction;
import net.shibboleth.idp.attribute.transcoding.TranscoderSupport;
import net.shibboleth.idp.attribute.transcoding.TranscodingRule;
import net.shibboleth.utilities.java.support.collection.Pair;
import net.shibboleth.utilities.java.support.component.ComponentInitializationException;

/**
 * Test for {@link AttributeTranscoderRegistryImpl}.
 */
@SuppressWarnings({"javadoc", "rawtypes"})
public class AttributeTranscoderRegistryImplTest {
    
    private AttributeTranscoderRegistryImpl registry;
    
    @BeforeClass public void setUp() throws ComponentInitializationException {
        registry = new AttributeTranscoderRegistryImpl();
        registry.setId("test");
        
        final NamingFunction nf = new BasicNamingFunction<>(Pair.class,
                new Function<Pair,String>() {
                    public String apply(final Pair p) {
                        return "{Pair}" + p.getFirst().toString();
                    }
            });
        registry.setNamingRegistry(Collections.singletonList(nf));
        
        final PairTranscoder transcoder = new PairTranscoder();
        transcoder.initialize();
        
        final Map<String,Object> ruleset1 = new HashMap<>();
        ruleset1.put(AttributeTranscoderRegistry.PROP_ID, "foo");
        ruleset1.put(AttributeTranscoderRegistry.PROP_TRANSCODER, transcoder);
        ruleset1.put(AttributeTranscoderRegistry.PROP_DISPLAY_NAME, "Default name");
        ruleset1.put(AttributeTranscoderRegistry.PROP_DESCRIPTION + '.' + Locale.ENGLISH.toLanguageTag(), "English desc");
        ruleset1.put(AttributeTranscoderRegistry.PROP_DISPLAY_NAME + '.' +  Locale.CANADA_FRENCH.toLanguageTag(), "Canadian French name");
        ruleset1.put(AttributeTranscoderRegistry.PROP_DESCRIPTION + '.' + Locale.CANADA_FRENCH.toLanguageTag(), "Canadian French desc");
        ruleset1.put("name", "bar");
        
        final Map<String,Object> ruleset2 = new HashMap<>();
        ruleset2.put(AttributeTranscoderRegistry.PROP_ID, "foo");
        ruleset2.put(AttributeTranscoderRegistry.PROP_TRANSCODER, transcoder);
        ruleset2.put("name", "baz");
        
        final Map<String,Object> ruleset3 = new HashMap<>();
        ruleset3.put(AttributeTranscoderRegistry.PROP_ID, "foo");
        ruleset3.put(AttributeTranscoderRegistry.PROP_TRANSCODER, transcoder);
        ruleset3.put(AttributeTranscoderRegistry.PROP_CONDITION, Predicates.alwaysFalse());
        ruleset3.put(AttributeTranscoderRegistry.PROP_DISPLAY_NAME + '.' +  Locale.CANADA_FRENCH.toLanguageTag(), "Canadian French name2");
        ruleset3.put(AttributeTranscoderRegistry.PROP_DESCRIPTION + '.' +  Locale.FRENCH.toLanguageTag(), "French desc");
        ruleset3.put("name", "ban");

        final Map<String,Object> ruleset4 = new HashMap<>();
        ruleset4.put(AttributeTranscoderRegistry.PROP_ID, "foo2");
        ruleset4.put(AttributeTranscoderRegistry.PROP_TRANSCODER, transcoder);
        ruleset4.put("name", "baz");
        
        final Map<String,Object> ruleset5 = new HashMap<>();
        ruleset5.put(AttributeTranscoderRegistry.PROP_ID, "foo bar");
        ruleset5.put(AttributeTranscoderRegistry.PROP_TRANSCODER, transcoder);
        ruleset5.put("name", "baz");

        final Map<String,Object> ruleset6 = new HashMap<>();
        ruleset6.put(AttributeTranscoderRegistry.PROP_ID, "frobnitz");
        ruleset6.put(AttributeTranscoderRegistry.PROP_TRANSCODER, transcoder);
        ruleset6.put(AttributeTranscoderRegistry.PROP_ENCODER, "false");
        ruleset6.put(AttributeTranscoderRegistry.PROP_DECODER, "false");
        ruleset6.put("name", "foo");

        registry.setTranscoderRegistry(List.of(
                new TranscodingRule(ruleset1),
                new TranscodingRule(ruleset2),
                new TranscodingRule(ruleset3),
                new TranscodingRule(ruleset4),
                new TranscodingRule(ruleset5),
                new TranscodingRule(ruleset6)));
        registry.setApplicationContext(new MockApplicationContext());
        registry.initialize();
        
        assertEquals(registry.getDisplayNames(new IdPAttribute("foo")).size(), 2);
        assertEquals(registry.getDescriptions(new IdPAttribute("foo")).size(), 3);
    }
    
    @AfterClass public void tearDown() {
        registry.destroy();
        registry = null;
    }


    @Test public void testEncodeNoMappings() throws AttributeEncodingException {
        
        assertTrue(registry.getTranscodingRules(new IdPAttribute("frobnitz"), Pair.class).isEmpty());
        assertTrue(registry.getTranscodingRules(new IdPAttribute("frobnitz"), MyPair.class).isEmpty());
        assertTrue(registry.getTranscodingRules(new IdPAttribute("foo"), String.class).isEmpty());
}

    @Test public void testDecodeNoMappings() throws AttributeDecodingException {
        
        assertTrue(registry.getTranscodingRules(new Pair<>("foo", "value")).isEmpty());
        assertTrue(registry.getTranscodingRules(new MyPair<>("foo", "value")).isEmpty());
        assertTrue(registry.getTranscodingRules(new String("bar")).isEmpty());
    }

    @Test public void testDecodeInactive() throws AttributeDecodingException {

        final Pair<?,?> p = new Pair<>("ban", "value");
        final Collection<TranscodingRule> rulesets = registry.getTranscodingRules(p);
        assertEquals(rulesets.size(), 1);
        
        final TranscodingRule ruleset = rulesets.iterator().next();
        
        final AttributeTranscoder<Pair<?,?>> t = TranscoderSupport.getTranscoder(ruleset);
        assertNull(t.decode(null, p, ruleset));
    }

    @Test public void testEncodeNoValues() throws AttributeEncodingException {
        final IdPAttribute foo = new IdPAttribute("foo");
        
        final List<Pair<?,?>> pairs = new ArrayList<>();
        
        for (final TranscodingRule ruleset : registry.getTranscodingRules(foo, Pair.class)) {
            final AttributeTranscoder<Pair> t = TranscoderSupport.getTranscoder(ruleset);
            final Pair p = t.encode(null, foo, Pair.class, ruleset);
            if (p != null) {
                pairs.add(p);
            }
        }
        
        assertEquals(pairs.size(), 2);
        
        assertEquals(pairs.get(0).getFirst(), "bar");
        assertNull(pairs.get(0).getSecond());
        
        assertEquals(pairs.get(1).getFirst(), "baz");
        assertNull(pairs.get(1).getSecond());
    }

    @Test public void testDecodeOneNoValues() throws AttributeDecodingException {
        
        final Pair<?,?> bar = new Pair<>("bar", null);
        
        final List<IdPAttribute> attributes = new ArrayList<>();
        
        for (final TranscodingRule ruleset : registry.getTranscodingRules(bar)) {
            final AttributeTranscoder<Pair<?,?>> t = TranscoderSupport.getTranscoder(ruleset);
            attributes.add(t.decode(null, bar, ruleset));
        }
        
        assertEquals(attributes.size(), 1);
        
        assertEquals(attributes.get(0).getId(), "foo");
        assertTrue(attributes.get(0).getValues().isEmpty());
        
        assertEquals(attributes.get(0).getDisplayNames().get(Locale.getDefault()), "Default name");
        assertEquals(attributes.get(0).getDisplayDescriptions().get(Locale.forLanguageTag("en")), "English desc");
        assertEquals(attributes.get(0).getDisplayNames().get(Locale.forLanguageTag("fr-ca")), "Canadian French name");
        assertEquals(attributes.get(0).getDisplayDescriptions().get(Locale.forLanguageTag("fr-ca")), "Canadian French desc");
    }

    @Test public void testDecodeTwoNoValues() throws AttributeDecodingException {
        
        final Pair<?,?> baz = new Pair<>("baz", null);
        
        final List<IdPAttribute> attributes = new ArrayList<>();
        
        for (final TranscodingRule ruleset : registry.getTranscodingRules(baz)) {
            final AttributeTranscoder<Pair<?,?>> t = TranscoderSupport.getTranscoder(ruleset);
            attributes.add(t.decode(null, baz, ruleset));
        }
        
        assertEquals(attributes.size(), 2);
        
        assertEquals(attributes.get(0).getId(), "foo");
        assertTrue(attributes.get(0).getValues().isEmpty());
        assertTrue(attributes.get(0).getDisplayNames().isEmpty());
        assertTrue(attributes.get(0).getDisplayDescriptions().isEmpty());

        assertEquals(attributes.get(1).getId(), "foo2");
        assertTrue(attributes.get(1).getValues().isEmpty());
        assertTrue(attributes.get(1).getDisplayNames().isEmpty());
        assertTrue(attributes.get(1).getDisplayDescriptions().isEmpty());
    }

    @Test public void testEncodeStringValues() throws AttributeEncodingException {
        final IdPAttribute foo = new IdPAttribute("foo");
        foo.setValues(Collections.singletonList(StringAttributeValue.valueOf("value")));
        
        final List<Pair<?,?>> pairs = new ArrayList<>();
        
        for (final TranscodingRule ruleset : registry.getTranscodingRules(foo, Pair.class)) {
            final AttributeTranscoder<Pair> t = TranscoderSupport.getTranscoder(ruleset);
            final Pair p = t.encode(null, foo, Pair.class, ruleset);
            if (p != null) {
                pairs.add(p);
            }
        }
        
        assertEquals(pairs.size(), 2);
        
        assertEquals(pairs.get(0).getFirst(), "bar");
        assertEquals(pairs.get(0).getSecond(), "value");
        
        assertEquals(pairs.get(1).getFirst(), "baz");
        assertEquals(pairs.get(1).getSecond(), "value");
    }

    @Test public void testEncodeSubtypeStringValues() throws AttributeEncodingException {
        final IdPAttribute foo = new IdPAttribute("foo");
        foo.setValues(Collections.singletonList(StringAttributeValue.valueOf("value")));
        
        final List<MyPair> pairs = new ArrayList<>();
        
        for (final TranscodingRule ruleset : registry.getTranscodingRules(foo, MyPair.class)) {
            final AttributeTranscoder<MyPair> t = TranscoderSupport.getTranscoder(ruleset);
            final MyPair p = t.encode(null, foo, MyPair.class, ruleset);
            if (p != null) {
                pairs.add(p);
            }
        }
        
        assertEquals(pairs.size(), 2);
        
        assertEquals(pairs.get(0).getFirst(), "bar");
        assertEquals(pairs.get(0).getSecond(), "value");
        
        assertEquals(pairs.get(1).getFirst(), "baz");
        assertEquals(pairs.get(1).getSecond(), "value");
    }
    
    @Test public void testDecodeOneStringValues() throws AttributeDecodingException {
        
        final Pair<?,?> bar = new Pair<>("bar", "value");
        
        final List<IdPAttribute> attributes = new ArrayList<>();
        
        for (final TranscodingRule ruleset : registry.getTranscodingRules(bar)) {
            final AttributeTranscoder<Pair<?,?>> t = TranscoderSupport.getTranscoder(ruleset);
            attributes.add(t.decode(null, bar, ruleset));
        }
        
        assertEquals(attributes.size(), 1);
        
        assertEquals(attributes.get(0).getId(), "foo");
        assertEquals(((StringAttributeValue)attributes.get(0).getValues().get(0)).getValue(), "value");
    }
    
    @Test public void testDecodeTwoStringValues() throws AttributeDecodingException {
        
        final Pair<?,?> baz = new Pair<>("baz", "value");
        
        final List<IdPAttribute> attributes = new ArrayList<>();
        
        for (final TranscodingRule ruleset : registry.getTranscodingRules(baz)) {
            final AttributeTranscoder<Pair<?,?>> t = TranscoderSupport.getTranscoder(ruleset);
            attributes.add(t.decode(null, baz, ruleset));
        }
        
        assertEquals(attributes.size(), 2);
        
        assertEquals(attributes.get(0).getId(), "foo");
        assertEquals(((StringAttributeValue)attributes.get(0).getValues().get(0)).getValue(), "value");

        assertEquals(attributes.get(1).getId(), "foo2");
        assertEquals(((StringAttributeValue)attributes.get(1).getValues().get(0)).getValue(), "value");
    }

    @Test public void testEncodeUnsupportedValues() throws AttributeEncodingException {
        final IdPAttribute foo = new IdPAttribute("foo");
        foo.setValues(Collections.singletonList(EmptyAttributeValue.ZERO_LENGTH));
        
        final List<Pair<?,?>> pairs = new ArrayList<>();
        
        for (final TranscodingRule ruleset : registry.getTranscodingRules(foo, Pair.class)) {
            final AttributeTranscoder<Pair> t = TranscoderSupport.getTranscoder(ruleset);
            final Pair p = t.encode(null, foo, Pair.class, ruleset);
            if (p != null) {
                pairs.add(p);
            }
        }
        
        assertEquals(pairs.size(), 2);
        
        assertEquals(pairs.get(0).getFirst(), "bar");
        assertNull(pairs.get(0).getSecond());
        
        assertEquals(pairs.get(1).getFirst(), "baz");
        assertNull(pairs.get(0).getSecond());
    }
    
    @Test public void testDecodeOneUnsupportedValues() throws AttributeDecodingException {
        
        final Pair<?,?> bar = new Pair<>("bar", 0L);
        
        final List<IdPAttribute> attributes = new ArrayList<>();
        
        for (final TranscodingRule ruleset : registry.getTranscodingRules(bar)) {
            final AttributeTranscoder<Pair<?,?>> t = TranscoderSupport.getTranscoder(ruleset);
            attributes.add(t.decode(null, bar, ruleset));
        }
        
        assertEquals(attributes.size(), 1);
        
        assertEquals(attributes.get(0).getId(), "foo");
        assertTrue(attributes.get(0).getValues().isEmpty());
    }
    
    @Test public void testDecodeTwoUnsupportedValues() throws AttributeDecodingException {
        
        final Pair<?,?> baz = new Pair<>("baz", 0L);
        
        final List<IdPAttribute> attributes = new ArrayList<>();
        
        for (final TranscodingRule ruleset : registry.getTranscodingRules(baz)) {
            final AttributeTranscoder<Pair<?,?>> t = TranscoderSupport.getTranscoder(ruleset);
            attributes.add(t.decode(null, baz, ruleset));
        }
        
        assertEquals(attributes.size(), 2);
        
        assertEquals(attributes.get(0).getId(), "foo");
        assertTrue(attributes.get(0).getValues().isEmpty());

        assertEquals(attributes.get(1).getId(), "foo2");
        assertTrue(attributes.get(1).getValues().isEmpty());
    }
    
    /** Marker class to exercise subtype support. */
    
    public static class MyPair<T,U> extends Pair<T,U> {
        public MyPair(T one, U two) {
            super(one, two);
        }
    }

}