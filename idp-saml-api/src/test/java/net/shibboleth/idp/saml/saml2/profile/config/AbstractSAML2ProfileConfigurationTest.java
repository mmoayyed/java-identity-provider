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

package net.shibboleth.idp.saml.saml2.profile.config;

import java.util.HashSet;
import java.util.Set;

import net.shibboleth.shared.logic.FunctionSupport;
import net.shibboleth.shared.logic.PredicateSupport;

import org.testng.Assert;
import org.testng.annotations.Test;

/** Unit test for {@link AbstractSAML2ProfileConfiguration}. */
@SuppressWarnings("javadoc")
public class AbstractSAML2ProfileConfigurationTest {

    @Test
    public void testEncryptionOptional(){
        final MockSAML2ProfileConfiguration config = new MockSAML2ProfileConfiguration();
        Assert.assertFalse(config.isEncryptionOptional(null));

        config.setEncryptionOptional(true);
        Assert.assertTrue(config.isEncryptionOptional(null));
    }

    @Test
    public void testIndirectEncryptionOptional(){
        final MockSAML2ProfileConfiguration config = new MockSAML2ProfileConfiguration();

        config.setEncryptionOptionalPredicate(PredicateSupport.alwaysTrue());
        Assert.assertTrue(config.isEncryptionOptional(null));
    }

    @Test public void testEncryptNameIDsPredicate() {
        final MockSAML2ProfileConfiguration config = new MockSAML2ProfileConfiguration();

        config.setEncryptNameIDs(true);
        Assert.assertTrue(config.isEncryptNameIDs(null));
    }

    @Test public void testEncryptAssertionsPredicate() {
        final MockSAML2ProfileConfiguration config = new MockSAML2ProfileConfiguration();

        config.setEncryptAssertions(true);
        Assert.assertTrue(config.isEncryptAssertions(null));
    }

    @Test public void testEncryptAttributesPredicate() {
        final MockSAML2ProfileConfiguration config = new MockSAML2ProfileConfiguration();

        config.setEncryptAttributes(true);
        Assert.assertTrue(config.isEncryptAttributes(null));
    }
    
    @Test public void testProxyCount() {
        final MockSAML2ProfileConfiguration config = new MockSAML2ProfileConfiguration();
        Assert.assertNull(config.getProxyCount(null));

        config.setProxyCount(1);
        Assert.assertEquals(config.getProxyCount(null), Integer.valueOf(1));
    }

    @Test public void testIndirectProxyCount() {
        final MockSAML2ProfileConfiguration config = new MockSAML2ProfileConfiguration();

        config.setProxyCountLookupStrategy(FunctionSupport.constant(1));
        Assert.assertEquals(config.getProxyCount(null), Integer.valueOf(1));
    }

    @Test public void testProxyAudiences() {
        final MockSAML2ProfileConfiguration config = new MockSAML2ProfileConfiguration();
        Assert.assertNotNull(config.getProxyAudiences(null));
        Assert.assertTrue(config.getProxyAudiences(null).isEmpty());

        final Set<String> audiences = new HashSet<>();
        audiences.add("foo");
        audiences.add("bar");

        config.setProxyAudiences(audiences);
        Assert.assertNotSame(config.getProxyAudiences(null), audiences);
        Assert.assertEquals(config.getProxyAudiences(null), audiences);

        try {
            config.getProxyAudiences(null).add("baz");
            Assert.fail();
        } catch (UnsupportedOperationException e) {
            // expected this
        }
    }

    @Test public void testIndirectProxyAudiences() {
        final MockSAML2ProfileConfiguration config = new MockSAML2ProfileConfiguration();

        final Set<String> audiences = new HashSet<>();
        audiences.add("foo");
        audiences.add("bar");

        config.setProxyAudiencesLookupStrategy(FunctionSupport.constant(audiences));
        Assert.assertNotSame(config.getProxyAudiences(null), audiences);
        Assert.assertEquals(config.getProxyAudiences(null), audiences);

        try {
            config.getProxyAudiences(null).add("baz");
            Assert.fail();
        } catch (UnsupportedOperationException e) {
            // expected this
        }
    }

    /** Mock class for testing {@link AbstractSAML2ProfileConfiguration}. */
    private static class MockSAML2ProfileConfiguration extends AbstractSAML2ProfileConfiguration {

        /** Constructor. */
        public MockSAML2ProfileConfiguration() {
            super("mock");
        }
    }
}