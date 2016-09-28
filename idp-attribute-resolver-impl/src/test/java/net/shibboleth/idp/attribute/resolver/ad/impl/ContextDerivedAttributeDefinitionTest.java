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

package net.shibboleth.idp.attribute.resolver.ad.impl;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.security.auth.Subject;

import net.shibboleth.idp.attribute.IdPAttribute;
import net.shibboleth.idp.attribute.IdPAttributeValue;
import net.shibboleth.idp.attribute.StringAttributeValue;
import net.shibboleth.idp.attribute.resolver.ResolutionException;
import net.shibboleth.idp.attribute.resolver.context.AttributeResolutionContext;
import net.shibboleth.idp.authn.AuthenticationResult;
import net.shibboleth.idp.authn.context.SubjectContext;
import net.shibboleth.idp.authn.principal.IdPAttributePrincipal;
import net.shibboleth.idp.saml.authn.principal.AuthenticationMethodPrincipal;
import net.shibboleth.idp.saml.impl.TestSources;
import net.shibboleth.utilities.java.support.component.ComponentInitializationException;

import org.testng.Assert;
import org.testng.annotations.Test;

/** Test for {@link SubjectDerivedAttributeValuesFunction}. */
public class ContextDerivedAttributeDefinitionTest {

    /** Simple result. */
    private static final String SIMPLE_VALUE = "simple";
    

    @Test public void simpleValue() throws ComponentInitializationException, ResolutionException {
        final List<IdPAttributeValue<String>> list = new ArrayList<>(2);
        list.add(new StringAttributeValue(SIMPLE_VALUE));
        list.add(new StringAttributeValue(SIMPLE_VALUE + "2"));
        
        final IdPAttribute attr = new IdPAttribute("wibble");
        attr.setValues(list);

        final SubjectDerivedAttributeValuesFunction ctxValueFunction = new SubjectDerivedAttributeValuesFunction();
        ctxValueFunction.setId("pDaD");
        final IdPAttributePrincipalValuesFunction fn = new IdPAttributePrincipalValuesFunction();
        fn.setAttributeName("wibble");
        fn.doInitialize();
        ctxValueFunction.setAttributeValuesFunction(fn);
        
        final ContextDerivedAttributeDefinition defn = new ContextDerivedAttributeDefinition();
        defn.setAttributeValuesFunction(ctxValueFunction);
        defn.setId("pDAD");
        defn.initialize();

        final AttributeResolutionContext ctx =
                TestSources.createResolutionContext(TestSources.PRINCIPAL_ID, TestSources.IDP_ENTITY_ID,
                        TestSources.SP_ENTITY_ID);
        final SubjectContext sc = ctx.getParent().getSubcontext(SubjectContext.class, true);
        final Map<String, AuthenticationResult> authnResults = sc.getAuthenticationResults();
        final Subject subject = new Subject();
        subject.getPrincipals().add(new IdPAttributePrincipal(attr));
        subject.getPrincipals().add(new AuthenticationMethodPrincipal(SIMPLE_VALUE + "2"));
        authnResults.put("one", new AuthenticationResult("1", subject));
        
        
      final List<IdPAttributeValue<?>> foo = defn.resolve(ctx).getValues();
        
        Assert.assertEquals(2, foo.size());
        Assert.assertTrue(foo.contains(new StringAttributeValue(SIMPLE_VALUE)));
        Assert.assertTrue(foo.contains(new StringAttributeValue(SIMPLE_VALUE + "2")));
    }
}
