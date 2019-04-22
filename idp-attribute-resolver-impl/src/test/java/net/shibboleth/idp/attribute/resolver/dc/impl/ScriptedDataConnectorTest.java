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

package net.shibboleth.idp.attribute.resolver.dc.impl;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import javax.script.ScriptException;
import javax.security.auth.Subject;

import org.opensaml.profile.context.ProfileRequestContext;
import org.testng.annotations.Test;

import net.shibboleth.idp.attribute.IdPAttribute;
import net.shibboleth.idp.attribute.IdPAttributeValue;
import net.shibboleth.idp.attribute.StringAttributeValue;
import net.shibboleth.idp.attribute.resolver.ResolutionException;
import net.shibboleth.idp.attribute.resolver.context.AttributeResolutionContext;
import net.shibboleth.idp.attribute.resolver.context.AttributeResolverWorkContext;
import net.shibboleth.idp.authn.AuthenticationResult;
import net.shibboleth.idp.authn.context.SubjectContext;
import net.shibboleth.idp.saml.authn.principal.AuthenticationMethodPrincipal;
import net.shibboleth.utilities.java.support.component.ComponentInitializationException;
import net.shibboleth.utilities.java.support.primitive.StringSupport;
import net.shibboleth.utilities.java.support.scripting.EvaluableScript;
import net.shibboleth.utilities.java.support.testing.TestSupport;

/**
 * Tests for {@link ScriptedDataConnector}
 * 
 */
public class ScriptedDataConnectorTest {

    private String getScript(String fileName) throws IOException {
        final String name;
        if (TestSupport.isJavaV8OrLater()) {
            name = "/net/shibboleth/idp/attribute/resolver/impl/dc/v8/" + fileName;
        } else {
            name = "/net/shibboleth/idp/attribute/resolver/impl/dc/" + fileName;
        }
        return StringSupport.inputStreamToString(getClass().getResourceAsStream(name), null);
    }

    @Test(expectedExceptions=ResolutionException.class)
    public void error() throws ComponentInitializationException, ScriptException, IOException, ResolutionException {
        final ScriptedDataConnector connector = new ScriptedDataConnector();
        connector.setId("Scripted");
        final EvaluableScript definitionScript = new EvaluableScript("javascript", getScript("error.js"));
        connector.setScript(definitionScript);

        connector.initialize();

        final AttributeResolutionContext context = new ProfileRequestContext<>().getSubcontext(AttributeResolutionContext.class,  true);
        context.getSubcontext(AttributeResolverWorkContext.class, true);
        
        connector.resolve(context);
    }
    
    @Test public void simple() throws ComponentInitializationException, ResolutionException, ScriptException, IOException {

        final ScriptedDataConnector connector = new ScriptedDataConnector();
        connector.setId("Scripted");
        final EvaluableScript definitionScript = new EvaluableScript("javascript", getScript("scriptedConnector.js"));
        connector.setScript(definitionScript);

        connector.initialize();

        final AttributeResolutionContext context = new ProfileRequestContext<>().getSubcontext(AttributeResolutionContext.class,  true);
        
        final SubjectContext sc = context.getParent().getSubcontext(SubjectContext.class, true);
        
        final Map<String, AuthenticationResult> authnResults = sc.getAuthenticationResults();
        Subject subject = new Subject();
        subject.getPrincipals().add(new AuthenticationMethodPrincipal("Foo"));
        subject.getPrincipals().add(new AuthenticationMethodPrincipal("Bar"));
        authnResults.put("one", new AuthenticationResult("1", subject));
        subject = new Subject();
        subject.getPrincipals().add(new AuthenticationMethodPrincipal("Toto"));
        authnResults.put("two", new AuthenticationResult("2", subject));

        
        context.getSubcontext(AttributeResolverWorkContext.class, true);
        final Map<String, IdPAttribute> result = connector.resolve(context);

        assertEquals(result.size(), 4);
        
        List<IdPAttributeValue<?>> values = result.get("ScriptedOne").getValues();
        assertEquals(values.size(), 2);
        assertTrue(values.contains(new StringAttributeValue("Value 1")));
        assertTrue(values.contains(new StringAttributeValue("Value 2")));

        values = result.get("TwoScripted").getValues();
        assertEquals(values.size(), 3);
        assertTrue(values.contains(new StringAttributeValue("1Value")));
        assertTrue(values.contains(new StringAttributeValue("2Value")));
        assertTrue(values.contains(new StringAttributeValue("3Value")));

        values = result.get("ThreeScripted").getValues();
        assertEquals(values.size(), 1);
        assertTrue(values.contains(new StringAttributeValue(AttributeResolutionContext.class.getSimpleName())));
        
        values = result.get("Subjects").getValues();
        assertEquals(values.size(), 3);
        assertTrue(values.contains(new StringAttributeValue("Foo")));
        assertTrue(values.contains(new StringAttributeValue("Bar")));
        assertTrue(values.contains(new StringAttributeValue("Toto")));

    }
    
    @Test public void custom() throws ComponentInitializationException, ResolutionException, ScriptException, IOException {

        final ScriptedDataConnector connector = new ScriptedDataConnector();
        connector.setId("Scripted");
        
        final IdPAttribute attribute = new IdPAttribute("attr");
        attribute.setValues(Collections.singletonList((IdPAttributeValue<?>)new StringAttributeValue("bar")));
        connector.setCustomObject(attribute);
        
        final EvaluableScript definitionScript = new EvaluableScript("javascript", getScript("custom.js"));
        connector.setScript(definitionScript);

        connector.initialize();

        final AttributeResolutionContext context = new ProfileRequestContext<>().getSubcontext(AttributeResolutionContext.class,  true);
        context.getSubcontext(AttributeResolverWorkContext.class, true);
        final Map<String, IdPAttribute> result = connector.resolve(context);

        assertEquals(result.size(), 1);
        assertEquals(result.get(attribute.getId()),attribute);
    }


}
