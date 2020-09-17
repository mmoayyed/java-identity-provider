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

package net.shibboleth.idp.attribute.resolver.spring.dc;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;

import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.BeanDefinitionStoreException;
import org.testng.annotations.Test;

import net.shibboleth.idp.attribute.IdPAttribute;
import net.shibboleth.idp.attribute.IdPAttributeValue;
import net.shibboleth.idp.attribute.StringAttributeValue;
import net.shibboleth.idp.attribute.resolver.ResolutionException;
import net.shibboleth.idp.attribute.resolver.context.AttributeResolutionContext;
import net.shibboleth.idp.attribute.resolver.dc.impl.ScriptedDataConnector;
import net.shibboleth.idp.attribute.resolver.spring.BaseAttributeDefinitionParserTest;
import net.shibboleth.idp.attribute.resolver.spring.dc.impl.ScriptDataConnectorParser;
import net.shibboleth.idp.saml.impl.testing.TestSources;
import net.shibboleth.utilities.java.support.component.ComponentInitializationException;

/**
 * test for {@link ScriptDataConnectorParser}
 */
@SuppressWarnings("javadoc")
public class ScriptDataConnectorParserTest extends BaseAttributeDefinitionParserTest {

    @Test public void resolver() throws ComponentInitializationException, ResolutionException {

        testConnector("resolver/scriptedAttributes.xml");
    }


    private void testConnector(final String source) throws ComponentInitializationException, ResolutionException {
        final ScriptedDataConnector dataConnector = getDataConnector(source, ScriptedDataConnector.class);
        dataConnector.initialize();
        
        final Map<?,?> custom = (Map<?,?>) dataConnector.getCustomObject();
        
        assertEquals(custom.size(), 1);
        assertEquals(custom.get("bar"), "foo");
        
        final AttributeResolutionContext context =
                TestSources.createResolutionContext(TestSources.PRINCIPAL_ID, TestSources.IDP_ENTITY_ID,
                        TestSources.SP_ENTITY_ID);
        final Map<String, IdPAttribute> result = dataConnector.resolve(context);
        
        assertEquals(result.size(), 2);
        
        List<IdPAttributeValue> values = result.get("ScriptedOne").getValues();
        assertEquals(values.size(), 2);
        assertTrue(values.contains(new StringAttributeValue("Value 1")));
        assertTrue(values.contains(new StringAttributeValue("Value 2")));

        values = result.get("TwoScripted").getValues();
        assertEquals(values.size(), 3);
        assertTrue(values.contains(new StringAttributeValue("1Value")));
        assertTrue(values.contains(new StringAttributeValue("2Value")));
        assertTrue(values.contains(new StringAttributeValue("3Value")));
        
    }
    
    @Test public void tooManyFiles() throws ComponentInitializationException, ResolutionException {

        getDataConnector("resolver/scriptedAttributeTooManyFiles.xml", ScriptedDataConnector.class);
    }
    
    @Test public void tooManyScript() throws ComponentInitializationException, ResolutionException {

        getDataConnector("resolver/scriptedAttributeTooManyScripts.xml", ScriptedDataConnector.class);
    }

    @Test public void both() throws ComponentInitializationException, ResolutionException {

        getDataConnector("resolver/scriptedAttributeBoth.xml", ScriptedDataConnector.class);
    }
    
    @Test(expectedExceptions={BeanDefinitionStoreException.class,}) public void none() throws ComponentInitializationException, ResolutionException {

        getDataConnector("resolver/scriptedAttributesNone.xml", ScriptedDataConnector.class);
    }

}
