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

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNull;
import static org.testng.Assert.fail;

import org.springframework.beans.factory.BeanCreationException;
import org.springframework.beans.factory.BeanDefinitionStoreException;
import org.testng.annotations.Test;

import net.shibboleth.idp.attribute.resolver.ResolutionException;
import net.shibboleth.idp.attribute.resolver.ad.impl.ScriptedAttributeDefinition;
import net.shibboleth.idp.attribute.resolver.context.AttributeResolutionContext;
import net.shibboleth.idp.attribute.resolver.context.AttributeResolverWorkContext;
import net.shibboleth.idp.attribute.resolver.spring.BaseAttributeDefinitionParserTest;
import net.shibboleth.idp.attribute.resolver.spring.ad.impl.ScriptedAttributeDefinitionParser;
import net.shibboleth.utilities.java.support.primitive.StringSupport;

/**
 * Test for {@link ScriptedAttributeDefinitionParser}.
 */
@SuppressWarnings("javadoc")
public class ScriptedAttributeParserTest extends BaseAttributeDefinitionParserTest {

    @Test public void inline() {
        final ScriptedAttributeDefinition attrDef =
                getAttributeDefn("resolver/scriptedAttributeInline.xml", ScriptedAttributeDefinition.class);

        assertEquals(attrDef.getId(), "scriptedInline");
        assertEquals(attrDef.getScript().getScriptLanguage(), "javascript");
        assertEquals(attrDef.getScript().getScript(), "foo=\"bar\";");
    }

    @Test public void file() {
        final ScriptedAttributeDefinition attrDef =
                getAttributeDefn("resolver/scriptedAttributeFile.xml", ScriptedAttributeDefinition.class);

        assertEquals(attrDef.getId(), "scriptedFile");
        assertEquals(attrDef.getScript().getScriptLanguage(), "javascript");
        assertEquals(StringSupport.trim(attrDef.getScript().getScript()), "foo=bar();");

        assertNull(attrDef.getCustomObject());

    }

    @Test public void dupl() {
        ScriptedAttributeDefinition attrDef =
                getAttributeDefn("resolver/scriptedAttributeDupl.xml", ScriptedAttributeDefinition.class, true);

        assertEquals(attrDef.getId(), "scriptedDupl");
        assertEquals(attrDef.getScript().getScriptLanguage(), "javascript");
        assertEquals(StringSupport.trim(attrDef.getScript().getScript()), "stuff=\"stuff\";");

        attrDef = getAttributeDefn("resolver/scriptedAttributeDuplFile.xml", ScriptedAttributeDefinition.class, true);

        assertEquals(attrDef.getId(), "scriptedDuplFile");
        assertEquals(attrDef.getScript().getScriptLanguage(), "javascript");
        assertEquals(StringSupport.trim(attrDef.getScript().getScript()), "foo=bar();");
    }

    @Test public void bad() throws ResolutionException {
        try {
            final ScriptedAttributeDefinition  attrdef =  getAttributeDefn("resolver/scriptedAttributeBad.xml", ScriptedAttributeDefinition.class);
            AttributeResolutionContext arc = new AttributeResolutionContext();
            arc.getSubcontext(AttributeResolverWorkContext.class, true);
            attrdef.resolve(arc);
            fail("Bad script worked?");
        } catch (BeanDefinitionStoreException | BeanCreationException | ResolutionException e) {
            // OK
        }
    }

    @Test public void absent() {
        try {
            getAttributeDefn("resolverscriptedAttributeAbsent.xml", ScriptedAttributeDefinition.class);
            fail("Missing script worked?");
        } catch (final BeanDefinitionStoreException e) {
            // OK
        }
    }

    @Test(expectedExceptions = {BeanCreationException.class,}) public void missingFile() {
        getAttributeDefn("resolver/scriptedAttributeFileMissing.xml", ScriptedAttributeDefinition.class);
    }

}
