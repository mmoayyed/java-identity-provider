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

package net.shibboleth.idp.profile.impl;

import org.opensaml.profile.action.ProfileAction;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.testng.AbstractTestNGSpringContextTests;
import org.testng.Assert;
import org.testng.annotations.Test;

/** {@link ProfileActionBeanPostProcessor} unit test. */
@ContextConfiguration({"ProfileActionBeanPostProcessorTest.xml"})
public class ProfileActionBeanPostProcessorTest extends AbstractTestNGSpringContextTests {

    @Test public void testPostProcessBeforeInitialization() {
        Object bean = null;

        bean = applicationContext.getBean("IdPActionWithDefaultID");
        Assert.assertTrue(bean instanceof ProfileAction);
        Assert.assertEquals(((ProfileAction) bean).getId(), "IdPActionWithDefaultID");

        bean = applicationContext.getBean("IdPActionWithCustomID");
        Assert.assertTrue(bean instanceof ProfileAction);
        Assert.assertEquals(((ProfileAction) bean).getId(), "CustomID");

        bean = applicationContext.getBean("OpenSAMLActionWithDefaultID");
        Assert.assertTrue(bean instanceof ProfileAction);
        Assert.assertEquals(((ProfileAction) bean).getId(), "OpenSAMLActionWithDefaultID");

        bean = applicationContext.getBean("OpenSAMLActionWithCustomID");
        Assert.assertTrue(bean instanceof ProfileAction);
        Assert.assertEquals(((ProfileAction) bean).getId(), "CustomID");
    }

    @Test public void testPostProcessAfterInitialization() {
        Object bean = null;

        bean = applicationContext.getBean("IdPActionWithDefaultID");
        Assert.assertFalse(bean instanceof WebFlowProfileActionAdaptor);

        bean = applicationContext.getBean("IdPActionWithCustomID");
        Assert.assertFalse(bean instanceof WebFlowProfileActionAdaptor);

        bean = applicationContext.getBean("OpenSAMLActionWithDefaultID");
        Assert.assertTrue(bean instanceof WebFlowProfileActionAdaptor);
        Assert.assertTrue(((WebFlowProfileActionAdaptor) bean).isInitialized());

        bean = applicationContext.getBean("OpenSAMLActionWithCustomID");
        Assert.assertTrue(bean instanceof WebFlowProfileActionAdaptor);
        Assert.assertTrue(((WebFlowProfileActionAdaptor) bean).isInitialized());
        
        // TODO Test throwing ComponentInitializationException ?
    }

}
