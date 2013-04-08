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

package net.shibboleth.idp.attribute.resolver.spring;

import net.shibboleth.idp.attribute.resolver.impl.ad.ScriptedAttributeDefinition;
import net.shibboleth.idp.attribute.resolver.spring.ad.BaseAttributeDefinitionBeanDefinitionParser;
import net.shibboleth.idp.attribute.resolver.spring.ad.SimpleAttributeDefinitionBeanDefinitionParser;
import net.shibboleth.utilities.java.support.primitive.StringSupport;

import org.springframework.beans.factory.BeanDefinitionStoreException;
import org.testng.Assert;
import org.testng.annotations.Test;

/**
 * Test for {@link SimpleAttributeDefinitionBeanDefinitionParser} and by extension
 * {@link BaseAttributeDefinitionBeanDefinitionParser}.
 */
public class TestScriptedAttributeDefinitionBeanParser extends BaseTestAttributeDefinitionBeanParser {

    @Test public void testInline() {
        ScriptedAttributeDefinition attrDef = getAttributeDefn("scriptedAttributeInline.xml", ScriptedAttributeDefinition.class);

        Assert.assertEquals(attrDef.getId(), "scriptedInline");
        Assert.assertEquals(attrDef.getScript().getScriptLanguage(), "javascript");
        Assert.assertEquals(attrDef.getScript().getScript(), "foo=\"bar\";");
    }

    @Test public void testFile() {
        ScriptedAttributeDefinition attrDef = getAttributeDefn("scriptedAttributeFile.xml", ScriptedAttributeDefinition.class);

        Assert.assertEquals(attrDef.getId(), "scriptedFile");
        Assert.assertEquals(attrDef.getScript().getScriptLanguage(), "javascript");
        Assert.assertEquals(StringSupport.trim(attrDef.getScript().getScript()), "foo=bar();");
    }
    
    @Test public void testDupl() {
        ScriptedAttributeDefinition attrDef = getAttributeDefn("scriptedAttributeDupl.xml", ScriptedAttributeDefinition.class);

        Assert.assertEquals(attrDef.getId(), "scriptedDupl");
        Assert.assertEquals(attrDef.getScript().getScriptLanguage(), "javascript");
        Assert.assertEquals(StringSupport.trim(attrDef.getScript().getScript()), "stuff=\"stuff\";");
    }

    @Test public void testBad() {
        try {
            getAttributeDefn("scriptedAttributeBad.xml", ScriptedAttributeDefinition.class);     
            Assert.fail("Bad script worked?");
        } catch (BeanDefinitionStoreException e) {
            // OK
        }
    }

    @Test public void testAbsent() {
        try {
            getAttributeDefn("scriptedAttributeAbsent.xml", ScriptedAttributeDefinition.class);
            Assert.fail("Missing script worked?");
        } catch (BeanDefinitionStoreException e) {
            // OK
        }
    }
    
    @Test public void testMissing() {
        try {
            getAttributeDefn("scriptedAttributeFileMissing.xml", ScriptedAttributeDefinition.class);
            Assert.fail("Missing script worked?");
        } catch (BeanDefinitionStoreException e) {
            // OK
        }
    }

}
