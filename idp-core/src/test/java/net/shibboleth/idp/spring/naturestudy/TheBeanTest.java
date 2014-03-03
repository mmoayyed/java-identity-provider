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

package net.shibboleth.idp.spring.naturestudy;

import java.util.Collection;

import net.shibboleth.idp.spring.SchemaTypeAwareXMLBeanDefinitionReader;

import org.springframework.beans.factory.xml.XmlBeanDefinitionReader;
import org.springframework.context.support.GenericApplicationContext;
import org.springframework.core.io.ClassPathResource;
import org.testng.Assert;
import org.testng.annotations.Test;

/**
 * How many ways can we do substitution?
 */
public class TheBeanTest {

    private TheBean getBeanNative(String fileName) {
        final GenericApplicationContext context = new GenericApplicationContext();
        SchemaTypeAwareXMLBeanDefinitionReader beanDefinitionReader =
                new SchemaTypeAwareXMLBeanDefinitionReader(context);

        beanDefinitionReader.setValidationMode(XmlBeanDefinitionReader.VALIDATION_XSD);
        beanDefinitionReader.loadBeanDefinitions(fileName);
        context.refresh(); // Gotta have this line or the property replacement won't work.

        Collection<TheBean> beans = context.getBeansOfType(TheBean.class).values();
        Assert.assertEquals(beans.size(), 1);

        return beans.iterator().next();
    }

    @Test public void testRawBean() {
        TheBean bean = new TheBean();
        bean.setMessage("RAW");
        Assert.assertEquals(bean.getMessage(), "RAW");
    }

    @Test public void testRawNativeBean() {
        // Resource re

        TheBean bean = getBeanNative("classpath:net/shibboleth/idp/spring/naturestudy/SimpleBean.xml");

        Assert.assertEquals(bean.getMessage(), "Spring");
    }

    @Test public void testRawReplaceBean() {
        TheBean bean = getBeanNative("classpath:net/shibboleth/idp/spring/naturestudy/SimpleBeanReplace.xml");

        Assert.assertEquals(bean.getMessage(), "REPLACE_SIMPLE_STRING");
    }

    @Test public void testRawCustomBean() {
        // Resource re

        TheBean bean = getBeanNative("classpath:net/shibboleth/idp/spring/naturestudy/CustomBean.xml");

        Assert.assertEquals(bean.getMessage(), "CustomRaw");
    }

    @Test public void testReplaceCustomBean() {
        // Resource re

        TheBean bean = getBeanNative("classpath:net/shibboleth/idp/spring/naturestudy/CustomBeanReplace.xml");

        Assert.assertEquals(bean.getMessage(), "REPLACE_CUSTOM_STRING");
    }

    @Test public void testRawCustomBean2() {
        // Resource re

        TheBean bean = getBeanNative("classpath:net/shibboleth/idp/spring/naturestudy/CustomBean2.xml");

        Assert.assertEquals(bean.getMessage(), "CustomRaw2");
    }

    @Test public void testReplaceCustomBean2() {
        // Resource re

        TheBean bean = getBeanNative("classpath:net/shibboleth/idp/spring/naturestudy/CustomBeanReplace2.xml");

        Assert.assertEquals(bean.getMessage(), "REPLACE_CUSTOM_2");
    }

    @Test public void testReplaceCustomBean3() {
        final GenericApplicationContext context = new GenericApplicationContext();
        SchemaTypeAwareXMLBeanDefinitionReader beanDefinitionReader =
                new SchemaTypeAwareXMLBeanDefinitionReader(context);

        beanDefinitionReader.setValidationMode(XmlBeanDefinitionReader.VALIDATION_XSD);
        beanDefinitionReader.loadBeanDefinitions(new ClassPathResource(
                "net/shibboleth/idp/spring/naturestudy/CustomBeanReplace3.xml"), new ClassPathResource(
                "net/shibboleth/idp/spring/naturestudy/PropertyPlaceholder.xml"));
        context.refresh();

        Collection<TheBean> beans = context.getBeansOfType(TheBean.class).values();
        Assert.assertEquals(beans.size(), 1);

        TheBean bean = beans.iterator().next();

        Assert.assertEquals(bean.getMessage(), "REPLACE_CUSTOM_2");
    }

    /**
     * Tests done as experimentation prior to building the RelyingParty parsers.
     * <p>
     * The RelyingParty.xml file contains two unrelated types - MetadataProviders and RelyingParties. We would like to
     * run these from separate services, but since the cost of initializing the former is significant we do not want to
     * do a full initialize.  So this test and the next is about removing the metadata providers from the RelyingParty parsing 
     * and vice versa.
     */
    @Test public void testRemoveSecondBean() {
        final GenericApplicationContext context = new GenericApplicationContext();
        SchemaTypeAwareXMLBeanDefinitionReader beanDefinitionReader =
                new SchemaTypeAwareXMLBeanDefinitionReader(context);

        beanDefinitionReader.setValidationMode(XmlBeanDefinitionReader.VALIDATION_XSD);
        TheSecondBean.resetInitCount();
        Assert.assertEquals(TheSecondBean.getInitCount(), 0);

        beanDefinitionReader.loadBeanDefinitions(new ClassPathResource(
                "net/shibboleth/idp/spring/naturestudy/MultipleBeans.xml"));
        Assert.assertEquals(TheSecondBean.getInitCount(), 0);

        context.removeBeanDefinition("ADistinguishedBeanName");

        context.refresh();
        Assert.assertEquals(TheSecondBean.getInitCount(), 0);

        Collection<TheBean> beans = context.getBeansOfType(TheBean.class).values();
        Assert.assertEquals(beans.size(), 2);
        Assert.assertEquals(TheSecondBean.getInitCount(), 0);
        Collection<TheSecondBean> secondBeans = context.getBeansOfType(TheSecondBean.class).values();
        Assert.assertEquals(secondBeans.size(), 0);
        Assert.assertEquals(TheSecondBean.getInitCount(), 0);
    }

    @Test public void testRemoveBeans() {
        final GenericApplicationContext context = new GenericApplicationContext();
        SchemaTypeAwareXMLBeanDefinitionReader beanDefinitionReader =
                new SchemaTypeAwareXMLBeanDefinitionReader(context);

        beanDefinitionReader.setValidationMode(XmlBeanDefinitionReader.VALIDATION_XSD);
        TheSecondBean.resetInitCount();
        Assert.assertEquals(TheSecondBean.getInitCount(), 0);

        beanDefinitionReader.loadBeanDefinitions(new ClassPathResource(
                "net/shibboleth/idp/spring/naturestudy/MultipleBeans.xml"));
        Assert.assertEquals(TheSecondBean.getInitCount(), 0);

        String[] names = context.getBeanDefinitionNames();

        for (String name : names) {
            if (!"ADistinguishedBeanName".equals(name)) {
                context.removeBeanDefinition(name);
            }
        }


        context.refresh();
        Assert.assertEquals(TheSecondBean.getInitCount(), 1);

        Collection<TheBean> beans = context.getBeansOfType(TheBean.class).values();
        Assert.assertEquals(beans.size(), 0);
        Assert.assertEquals(TheSecondBean.getInitCount(), 1);
        Collection<TheSecondBean> secondBeans = context.getBeansOfType(TheSecondBean.class).values();
        Assert.assertEquals(secondBeans.size(), 1);
        Assert.assertEquals(TheSecondBean.getInitCount(), 1);
    }

}
