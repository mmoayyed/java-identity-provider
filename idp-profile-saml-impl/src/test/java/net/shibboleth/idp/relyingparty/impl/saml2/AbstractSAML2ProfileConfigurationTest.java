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

package net.shibboleth.idp.relyingparty.impl.saml2;

import java.util.ArrayList;

import org.opensaml.xml.security.StaticResponseEvaluableCritieria;
import org.testng.Assert;
import org.testng.annotations.Test;

/** Unit test for {@link AbstractSAML2ProfileConfiguration}. */
public class AbstractSAML2ProfileConfigurationTest {

    @Test
    public void testEncryptNameIDsCriteria() {
        MockSAML2ProfileConfiguration config = new MockSAML2ProfileConfiguration();
        Assert.assertNotNull(config.getEncryptNameIDsCriteria());

        config.setEncryptNameIDsCriteria(StaticResponseEvaluableCritieria.FALSE_RESPONSE);
        Assert.assertSame(config.getEncryptNameIDsCriteria(), StaticResponseEvaluableCritieria.FALSE_RESPONSE);

        try {
            config.setEncryptNameIDsCriteria(null);
            Assert.fail();
        } catch (IllegalArgumentException e) {
            // excepted this
        }
    }

    @Test
    public void testEncryptAssertionsCriteria() {
        MockSAML2ProfileConfiguration config = new MockSAML2ProfileConfiguration();
        Assert.assertNotNull(config.getEncryptAssertionsCriteria());

        config.setEncryptAssertionsCriteria(StaticResponseEvaluableCritieria.FALSE_RESPONSE);
        Assert.assertSame(config.getEncryptAssertionsCriteria(), StaticResponseEvaluableCritieria.FALSE_RESPONSE);

        try {
            config.setEncryptAssertionsCriteria(null);
            Assert.fail();
        } catch (IllegalArgumentException e) {
            // excepted this
        }
    }

    @Test
    public void testProxyCount() {
        MockSAML2ProfileConfiguration config = new MockSAML2ProfileConfiguration();
        Assert.assertEquals(config.getProxyCount(), 0);

        config.setProxyCount(1);
        Assert.assertEquals(config.getProxyCount(), 1);
    }

    @Test
    public void testProxyAudiences() {
        MockSAML2ProfileConfiguration config = new MockSAML2ProfileConfiguration();
        Assert.assertNotNull(config.getProxyAudiences());
        Assert.assertTrue(config.getProxyAudiences().isEmpty());

        config.setProxyAudiences(null);
        Assert.assertNotNull(config.getProxyAudiences());
        Assert.assertTrue(config.getProxyAudiences().isEmpty());

        ArrayList<String> audiences = new ArrayList<String>();
        audiences.add("foo");
        audiences.add(null);
        audiences.add("");
        audiences.add("foo");
        audiences.add("bar");

        config.setProxyAudiences(audiences);
        Assert.assertNotSame(config.getProxyAudiences(), audiences);
        Assert.assertNotNull(config.getProxyAudiences());
        Assert.assertEquals(config.getProxyAudiences().size(), 2);
        Assert.assertTrue(config.getProxyAudiences().contains("foo"));
        Assert.assertTrue(config.getProxyAudiences().contains("bar"));

        try {
            config.getProxyAudiences().add("baz");
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