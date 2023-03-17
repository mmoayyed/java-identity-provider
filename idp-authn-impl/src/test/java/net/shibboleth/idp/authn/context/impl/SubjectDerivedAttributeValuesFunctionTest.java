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

package net.shibboleth.idp.authn.context.impl;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNull;
import static org.testng.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import javax.annotation.Nonnull;
import javax.security.auth.Subject;

import org.opensaml.messaging.context.BaseContext;
import org.testng.annotations.Test;

import net.shibboleth.idp.attribute.IdPAttribute;
import net.shibboleth.idp.attribute.IdPAttributeValue;
import net.shibboleth.idp.attribute.StringAttributeValue;
import net.shibboleth.idp.attribute.resolver.ResolutionException;
import net.shibboleth.idp.attribute.resolver.ad.impl.ContextDerivedAttributeDefinition;
import net.shibboleth.idp.attribute.resolver.context.AttributeResolutionContext;
import net.shibboleth.idp.attribute.resolver.testing.TestSources;
import net.shibboleth.idp.authn.AuthenticationResult;
import net.shibboleth.idp.authn.context.SubjectCanonicalizationContext;
import net.shibboleth.idp.authn.context.SubjectContext;
import net.shibboleth.idp.authn.principal.IdPAttributePrincipal;
import net.shibboleth.idp.saml.authn.principal.AuthenticationMethodPrincipal;
import net.shibboleth.shared.component.ComponentInitializationException;
import net.shibboleth.shared.logic.FunctionSupport;

/** Test for {@link SubjectDerivedAttributeValuesFunction}. */
@SuppressWarnings("javadoc")
public class SubjectDerivedAttributeValuesFunctionTest {

    /** Simple result. */
    private static final String SIMPLE_VALUE = "simple";
    
    @Nonnull private List<IdPAttributeValue> doResolve(@Nonnull final ContextDerivedAttributeDefinition defn, @Nonnull final AttributeResolutionContext ctx) throws ResolutionException {
        final IdPAttribute attr = defn.resolve(ctx);
        assert attr!=null;
        return attr.getValues();
    }

    @Test public void noSubjectContext() throws ComponentInitializationException, ResolutionException {

        final SubjectDerivedAttributeValuesFunction ctxValueFunction = new SubjectDerivedAttributeValuesFunction();
        ctxValueFunction.setId("pDaD");
        final IdPAttributePrincipalValuesFunction fn = new IdPAttributePrincipalValuesFunction();
        fn.setAttributeName("wibble");
        fn.doInitialize();
        ctxValueFunction.setAttributeValuesFunction(fn);
        ctxValueFunction.initialize();
        
        final ContextDerivedAttributeDefinition defn = new ContextDerivedAttributeDefinition();
        defn.setAttributeValuesFunction(ctxValueFunction);
        defn.setId("pDAD");
        defn.initialize();

        final AttributeResolutionContext ctx =
                TestSources.createResolutionContext(TestSources.PRINCIPAL_ID, TestSources.IDP_ENTITY_ID,
                        TestSources.SP_ENTITY_ID);        
        
        final IdPAttribute result = defn.resolve(ctx);
        assertNull(result);
    }
    
    @Test public void simpleValue() throws ComponentInitializationException, ResolutionException {
        final List<IdPAttributeValue> list = new ArrayList<>(2);
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
        ctxValueFunction.initialize();
        
        final ContextDerivedAttributeDefinition defn = new ContextDerivedAttributeDefinition();
        defn.setAttributeValuesFunction(ctxValueFunction);
        defn.setId("pDAD");
        defn.initialize();

        final AttributeResolutionContext ctx =
                TestSources.createResolutionContext(TestSources.PRINCIPAL_ID, TestSources.IDP_ENTITY_ID,
                        TestSources.SP_ENTITY_ID);
        final BaseContext parent = ctx.getParent();
        assert parent != null;
        final SubjectContext sc = parent.ensureSubcontext(SubjectContext.class);
        final Map<String, AuthenticationResult> authnResults = sc.getAuthenticationResults();
        final Subject subject = new Subject();
        subject.getPrincipals().add(new IdPAttributePrincipal(attr));
        subject.getPrincipals().add(new AuthenticationMethodPrincipal(SIMPLE_VALUE + "2"));
        authnResults.put("one", new AuthenticationResult("1", subject));
        
        
      final List<IdPAttributeValue> foo = doResolve(defn, ctx);
        
        assertEquals(2, foo.size());
        assertTrue(foo.contains(new StringAttributeValue(SIMPLE_VALUE)));
        assertTrue(foo.contains(new StringAttributeValue(SIMPLE_VALUE + "2")));
    }
    
    @Test public void simpleValueViaSubject() throws ComponentInitializationException, ResolutionException {
        final List<IdPAttributeValue> list = new ArrayList<>(2);
        list.add(new StringAttributeValue(SIMPLE_VALUE));
        list.add(new StringAttributeValue(SIMPLE_VALUE + "2"));
        
        final IdPAttribute attr = new IdPAttribute("wibble");
        attr.setValues(list);

        final Subject subject = new Subject();
        subject.getPrincipals().add(new IdPAttributePrincipal(attr));
        subject.getPrincipals().add(new AuthenticationMethodPrincipal(SIMPLE_VALUE + "2"));
        
        final SubjectDerivedAttributeValuesFunction ctxValueFunction = new SubjectDerivedAttributeValuesFunction();
        ctxValueFunction.setId("pDaD");
        ctxValueFunction.setSubjectLookupStrategy(FunctionSupport.constant(subject));
        final IdPAttributePrincipalValuesFunction fn = new IdPAttributePrincipalValuesFunction();
        fn.setAttributeName("wibble");
        fn.doInitialize();
        ctxValueFunction.setAttributeValuesFunction(fn);
        ctxValueFunction.initialize();
        
        final ContextDerivedAttributeDefinition defn = new ContextDerivedAttributeDefinition();
        defn.setAttributeValuesFunction(ctxValueFunction);
        defn.setId("pDAD");
        defn.initialize();

        final AttributeResolutionContext ctx =
                TestSources.createResolutionContext(TestSources.PRINCIPAL_ID, TestSources.IDP_ENTITY_ID,
                        TestSources.SP_ENTITY_ID);        
        
        final List<IdPAttributeValue> foo =  doResolve(defn, ctx);
        
        assertEquals(2, foo.size());
        assertTrue(foo.contains(new StringAttributeValue(SIMPLE_VALUE)));
        assertTrue(foo.contains(new StringAttributeValue(SIMPLE_VALUE + "2")));
    }
    
    @Test public void simpleValueViaSubjectC14N() throws ComponentInitializationException, ResolutionException {
        final List<IdPAttributeValue> list = new ArrayList<>(2);
        list.add(new StringAttributeValue(SIMPLE_VALUE));
        list.add(new StringAttributeValue(SIMPLE_VALUE + "2"));
        
        final IdPAttribute attr = new IdPAttribute("wibble");
        attr.setValues(list);

        final Subject subject = new Subject();
        subject.getPrincipals().add(new IdPAttributePrincipal(attr));
        subject.getPrincipals().add(new AuthenticationMethodPrincipal(SIMPLE_VALUE + "2"));
        
        final SubjectDerivedAttributeValuesFunction ctxValueFunction = new SubjectDerivedAttributeValuesFunction();
        ctxValueFunction.setId("pDaD");
        ctxValueFunction.setForCanonicalization(true);
        final IdPAttributePrincipalValuesFunction fn = new IdPAttributePrincipalValuesFunction();
        fn.setAttributeName("wibble");
        fn.doInitialize();
        ctxValueFunction.setAttributeValuesFunction(fn);
        ctxValueFunction.initialize();
        
        final ContextDerivedAttributeDefinition defn = new ContextDerivedAttributeDefinition();
        defn.setAttributeValuesFunction(ctxValueFunction);
        defn.setId("pDAD");
        defn.initialize();

        final AttributeResolutionContext ctx =
                TestSources.createResolutionContext(TestSources.PRINCIPAL_ID, TestSources.IDP_ENTITY_ID,
                        TestSources.SP_ENTITY_ID);
        final BaseContext parent = ctx.getParent();
        assert parent != null;
        final SubjectCanonicalizationContext sc = parent.ensureSubcontext(SubjectCanonicalizationContext.class);
        sc.setSubject(subject);
        
        final List<IdPAttributeValue> foo =  doResolve(defn, ctx);
        
        assertEquals(2, foo.size());
        assertTrue(foo.contains(new StringAttributeValue(SIMPLE_VALUE)));
        assertTrue(foo.contains(new StringAttributeValue(SIMPLE_VALUE + "2")));
    }
        
    @Test public void empty() throws ComponentInitializationException, ResolutionException {
        final List<IdPAttributeValue> list = Collections.emptyList();
        
        final IdPAttribute attr = new IdPAttribute("wibble");
        attr.setValues(list);

        final SubjectDerivedAttributeValuesFunction ctxValueFunction = new SubjectDerivedAttributeValuesFunction();
        ctxValueFunction.setId("pDaD");
        final IdPAttributePrincipalValuesFunction fn = new IdPAttributePrincipalValuesFunction();
        fn.setAttributeName("wibble");
        fn.doInitialize();
        ctxValueFunction.setAttributeValuesFunction(fn);
        ctxValueFunction.initialize();
        
        final ContextDerivedAttributeDefinition defn = new ContextDerivedAttributeDefinition();
        defn.setAttributeValuesFunction(ctxValueFunction);
        defn.setId("pDAD");
        defn.initialize();

        final AttributeResolutionContext ctx =
                TestSources.createResolutionContext(TestSources.PRINCIPAL_ID, TestSources.IDP_ENTITY_ID,
                        TestSources.SP_ENTITY_ID);
        final BaseContext parent = ctx.getParent();
        assert parent != null;
        final SubjectContext sc = parent.ensureSubcontext(SubjectContext.class);
        final Map<String, AuthenticationResult> authnResults = sc.getAuthenticationResults();
        final Subject subject = new Subject();
        subject.getPrincipals().add(new IdPAttributePrincipal(attr));
        subject.getPrincipals().add(new AuthenticationMethodPrincipal(SIMPLE_VALUE + "2"));
        authnResults.put("one", new AuthenticationResult("1", subject));
        
        final IdPAttribute result = defn.resolve(ctx);
        assertNull(result);
    }

}
