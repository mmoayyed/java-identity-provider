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

package net.shibboleth.idp.attribute.resolver.spring.ad;

import net.shibboleth.idp.attribute.resolver.impl.ad.TransientIdAttributeDefinition;
import net.shibboleth.utilities.java.support.component.ComponentInitializationException;

import org.springframework.beans.factory.BeanCreationException;
import org.testng.Assert;
import org.testng.annotations.Test;

/**
 * test for {@link TransientIdAttributeDefinitionBeanDefinitionParser}
 */
public class TestTransientIdAttributeDefinitionBeanDefinitionParser extends BaseTestAttributeDefinitionBeanParser {

    private TransientIdAttributeDefinition getDefinition(String fileName) {

        return getAttributeDefn(fileName, "idStore.xml", TransientIdAttributeDefinition.class);
    }

    @Test public void testWithTime() throws ComponentInitializationException {

        try {
            getDefinition("transientWithTime.xml");
            Assert.fail();
        } catch (BeanCreationException e) {
            // OK
        }
        TransientIdAttributeDefinition defn =
                getAttributeDefn("transientWithTime.xml", "idStore2.xml", TransientIdAttributeDefinition.class);

        Assert.assertEquals(defn.getId(), "transientIdWithTime");
        Assert.assertEquals(defn.getIdLifetime(), 1000 * 60 * 3);
        Assert.assertEquals(defn.getIdSize(), 16);
    }

    @Test public void testNoTime() throws ComponentInitializationException {

        TransientIdAttributeDefinition defn = getDefinition("transientNoTime.xml");
        defn.initialize();

        Assert.assertEquals(defn.getId(), "transientId");
        Assert.assertEquals(defn.getIdLifetime(), 1000 * 60 * 60 * 4);
        Assert.assertEquals(defn.getIdSize(), 16);
    }

}
