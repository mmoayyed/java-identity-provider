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
import static org.testng.Assert.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.annotation.Nonnull;
import javax.security.auth.Subject;

import org.opensaml.messaging.context.BaseContext;
import org.springframework.beans.factory.BeanDefinitionStoreException;
import org.testng.annotations.Test;

import net.shibboleth.idp.attribute.IdPAttribute;
import net.shibboleth.idp.attribute.IdPAttributeValue;
import net.shibboleth.idp.attribute.StringAttributeValue;
import net.shibboleth.idp.attribute.resolver.AttributeDefinition;
import net.shibboleth.idp.attribute.resolver.ResolutionException;
import net.shibboleth.idp.attribute.resolver.ad.impl.ContextDerivedAttributeDefinition;
import net.shibboleth.idp.attribute.resolver.context.AttributeResolutionContext;
import net.shibboleth.idp.attribute.resolver.spring.ad.impl.ContextDerivedAttributeDefinitionParser;
import net.shibboleth.idp.attribute.resolver.spring.ad.impl.SubjectDerivedAttributeDefinitionParser;
import net.shibboleth.idp.attribute.resolver.testing.TestSources;
import net.shibboleth.idp.authn.AuthenticationResult;
import net.shibboleth.idp.authn.context.SubjectCanonicalizationContext;
import net.shibboleth.idp.authn.context.SubjectContext;
import net.shibboleth.idp.authn.principal.IdPAttributePrincipal;
import net.shibboleth.idp.saml.authn.principal.AuthenticationMethodPrincipal;

/**
 * Test for {@link SubjectDerivedAttributeDefinitionParser} and {@link ContextDerivedAttributeDefinitionParser}.
 */
@SuppressWarnings("javadoc")
public class ContextDerivedAttributeDefinitionParserTest extends BaseAttributeDefinitionParserTest {

    /** Simple result. */
    @Nonnull private static final String SIMPLE_VALUE = "simple";

    @Nonnull private AttributeResolutionContext getCtx(@Nonnull final String attributeName, boolean c14n) {
        final List<IdPAttributeValue> list = new ArrayList<>(2);
        list.add(new StringAttributeValue(SIMPLE_VALUE));
        list.add(new StringAttributeValue(SIMPLE_VALUE + "2"));

        final IdPAttribute attr = new IdPAttribute(attributeName);
        attr.setValues(list);

        final AttributeResolutionContext ctx = TestSources.createResolutionContext(TestSources.PRINCIPAL_ID,
                TestSources.IDP_ENTITY_ID, TestSources.SP_ENTITY_ID);
        final Subject subject = new Subject();
        subject.getPrincipals().add(new IdPAttributePrincipal(attr));
        subject.getPrincipals().add(new AuthenticationMethodPrincipal(SIMPLE_VALUE + "2"));

        final BaseContext parent = ctx.getParent();
        assert parent != null;
        if (c14n) {
            parent.ensureSubcontext(SubjectCanonicalizationContext.class).setSubject(subject);
        } else {
            final SubjectContext sc = parent.ensureSubcontext(SubjectContext.class);
            final Map<String, AuthenticationResult> authnResults = sc.getAuthenticationResults();
            authnResults.put("one", new AuthenticationResult("1", subject));
        }

        return ctx;
    }

    @Test public void resolverSubject() throws ResolutionException {
        final AttributeDefinition attrDef =
                getAttributeDefn("subjectDerived.xml", ContextDerivedAttributeDefinition.class);

        final IdPAttribute attr = attrDef.resolve(getCtx("Whatever", false));
        assert attr != null;
        final List<IdPAttributeValue> foo = attr.getValues();

        assertEquals(2, foo.size());
        assertTrue(foo.contains(new StringAttributeValue(SIMPLE_VALUE)));
        assertTrue(foo.contains(new StringAttributeValue(SIMPLE_VALUE + "2")));

    }

    @Test public void resolverSubjectViaC14N() throws ResolutionException {
        final AttributeDefinition attrDef =
                getAttributeDefn("subjectDerivedViaC14N.xml", ContextDerivedAttributeDefinition.class);

        final IdPAttribute attr = attrDef.resolve(getCtx("Whatever", true));
        assert attr != null;
        final List<IdPAttributeValue> foo = attr.getValues();

        assertEquals(2, foo.size());
        assertTrue(foo.contains(new StringAttributeValue(SIMPLE_VALUE)));
        assertTrue(foo.contains(new StringAttributeValue(SIMPLE_VALUE + "2")));

    }

    @Test public void complex() throws ResolutionException {
        final AttributeDefinition attrDef = getAttributeDefn("subjectDerivedComplex.xml", "contextDerivedBeans.xml",
                ContextDerivedAttributeDefinition.class);

        assert attrDef != null;
        final IdPAttribute attr = attrDef.resolve(getCtx("BeanWhatever", false));
        assert attr != null;
        final List<IdPAttributeValue> foo = attr.getValues();

        assertEquals(2, foo.size());
        assertTrue(foo.contains(new StringAttributeValue(SIMPLE_VALUE)));
        assertTrue(foo.contains(new StringAttributeValue(SIMPLE_VALUE + "2")));
    }

    @Test public void context() throws ResolutionException {
        final AttributeDefinition attrDef = getAttributeDefn("contextDerived.xml", "contextDerivedBeans.xml",
                ContextDerivedAttributeDefinition.class);

        assert attrDef != null;
        final IdPAttribute attr = attrDef.resolve(getCtx("BeanWhatever", false));
        assert attr != null;
        final List<IdPAttributeValue> foo = attr.getValues();

        assertEquals(2, foo.size());
        assertTrue(foo.contains(new StringAttributeValue(SIMPLE_VALUE)));
        assertTrue(foo.contains(new StringAttributeValue(SIMPLE_VALUE + "2")));
    }

    @Test public void warn() throws ResolutionException {
        final AttributeDefinition attrDef =
                getAttributeDefn("subjectDerivedWarn.xml", ContextDerivedAttributeDefinition.class);
        final IdPAttribute attr = attrDef.resolve(getCtx("Whatever", false));
        assert attr != null;
        final List<IdPAttributeValue> foo = attr.getValues();

        assertEquals(2, foo.size());
        assertTrue(foo.contains(new StringAttributeValue(SIMPLE_VALUE)));
        assertTrue(foo.contains(new StringAttributeValue(SIMPLE_VALUE + "2")));
    }

    @Test(expectedExceptions = {BeanDefinitionStoreException.class}) public void fail() throws ResolutionException {
        getAttributeDefn("subjectDerivedFail.xml", ContextDerivedAttributeDefinition.class);
    }

    @Test(expectedExceptions = {BeanDefinitionStoreException.class}) public void dependency()
            throws ResolutionException {
        getAttributeDefn("subjectDerivedDependency.xml", ContextDerivedAttributeDefinition.class);
    }
}
