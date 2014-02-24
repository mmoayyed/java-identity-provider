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

package net.shibboleth.idp.spring;

import javax.annotation.Nonnull;

import junit.framework.Assert;
import net.shibboleth.utilities.java.support.component.AbstractIdentifiableInitializeableComponent;
import net.shibboleth.utilities.java.support.component.AbstractIdentifiedInitializableComponent;
import net.shibboleth.utilities.java.support.component.ComponentInitializationException;
import net.shibboleth.utilities.java.support.component.IdentifiedComponent;

import org.springframework.beans.factory.BeanCreationException;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.testng.AbstractTestNGSpringContextTests;
import org.testng.annotations.Test;

/**
 * A test for {@link IdentifiableBeanPostProcessor}.
 */
@ContextConfiguration({"identifiableBeanPostProcessorTest.xml"})
public class BeanPostProcessorTest extends AbstractTestNGSpringContextTests {

    @Test(expectedExceptions = {ComponentInitializationException.class, BeanCreationException.class,}) public void
            defaultedIdentified() {
        applicationContext.getBean("IdentifiedBean");
    }

    @Test public void defaultedIdentifiable() {
        IdentifiedComponent bean = applicationContext.getBean("IdentifiableBean", Identifiable.class);
        Assert.assertEquals("IdentifiableBean", bean.getId());
    }

    @Test public void nonDefaultedIdentified() {
        IdentifiedComponent bean = applicationContext.getBean("NonDefaultIdentifiedBean", Identified.class);
        Assert.assertEquals("NameForAnIdentifiedBean", bean.getId());

    }

    @Test public void nonDefaultedIdentifiable() {
        IdentifiedComponent bean = applicationContext.getBean("NonDefaultIdentifiableBean", Identifiable.class);
        Assert.assertEquals("NameForAnIdentifiableBean", bean.getId());
    }

    @Test public void TautologousTest() {
        IdentifiedComponent bean = applicationContext.getBean("TautologousName", Identifiable.class);
        Assert.assertEquals("TautologousName", bean.getId());

    }

    public static class Identified extends AbstractIdentifiedInitializableComponent {
        @Override public void setId(@Nonnull String componentId) {
            super.setId(componentId);
        }
    }

    public static class Identifiable extends AbstractIdentifiableInitializeableComponent {
    }
}
