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

package net.shibboleth.idp.profile.logic;

import javax.script.ScriptException;

import net.shibboleth.profile.context.RelyingPartyContext;
import net.shibboleth.shared.spring.custom.SchemaTypeAwareXMLBeanDefinitionReader;

import org.opensaml.profile.context.ProfileRequestContext;
import org.springframework.context.support.GenericApplicationContext;
import org.springframework.core.io.ClassPathResource;
import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

/**
 * Tests for {@link ScriptedPredicate}.
 */
@SuppressWarnings("javadoc")
public class ScriptedPredicateTest {

    private ProfileRequestContext withChild;

    private ProfileRequestContext noChild;

    @BeforeClass public void setup() {
        withChild = new ProfileRequestContext();
        withChild.getOrCreateSubcontext(RelyingPartyContext.class);
        noChild = new ProfileRequestContext();
    }

    @Test public void simple() throws ScriptException {
        ScriptedPredicate test = ScriptedPredicate.inlineScript("new java.lang.Boolean(true);");
        Assert.assertTrue(test.test(withChild));

        test = ScriptedPredicate.inlineScript("true");
        Assert.assertTrue(test.test(withChild));

        test = ScriptedPredicate.inlineScript("false");
        Assert.assertFalse(test.test(withChild));

        test = ScriptedPredicate.inlineScript("\"thirty\"");
        Assert.assertFalse(test.test(withChild));
    }
    
    @Test public void custom() throws ScriptException {
        ScriptedPredicate test = ScriptedPredicate.inlineScript("custom;");
        test.setCustomObject(Boolean.valueOf(true));
        Assert.assertTrue(test.test(withChild));

        test.setCustomObject(Boolean.valueOf(false));
        Assert.assertFalse(test.test(withChild));

    }
    @Test public void inlineBean() throws ScriptException {

        try (final GenericApplicationContext ctx = new GenericApplicationContext()) {
            final SchemaTypeAwareXMLBeanDefinitionReader beanDefinitionReader =
                    new SchemaTypeAwareXMLBeanDefinitionReader(ctx);

            beanDefinitionReader.loadBeanDefinitions(new ClassPathResource(
                    "/net/shibboleth/idp/profile/logic/inlineBean.xml"));

            ctx.refresh();
            final ScriptedPredicate rule = ctx.getBean(ScriptedPredicate.class);

            Assert.assertTrue(rule.test(withChild));

            Assert.assertFalse(rule.test(noChild));
        }
    }

    @Test public void resourceBean() throws ScriptException {

        try (final GenericApplicationContext ctx = new GenericApplicationContext()) {
            final SchemaTypeAwareXMLBeanDefinitionReader beanDefinitionReader =
                    new SchemaTypeAwareXMLBeanDefinitionReader(ctx);

            beanDefinitionReader.loadBeanDefinitions(new ClassPathResource(
                    "/net/shibboleth/idp/profile/logic/resourceBean.xml"));

            ctx.refresh();
            final ScriptedPredicate rule = ctx.getBean(ScriptedPredicate.class);

            Assert.assertTrue(rule.test(withChild));

            Assert.assertFalse(rule.test(noChild));
        }
    }

}
