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

package net.shibboleth.idp.attribute.filter.matcher.impl;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.assertNotSame;
import static org.testng.Assert.assertNull;
import static org.testng.Assert.assertTrue;
import static org.testng.Assert.fail;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;

import javax.annotation.concurrent.ThreadSafe;
import javax.security.auth.Subject;

import org.opensaml.profile.context.ProfileRequestContext;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.Test;

import net.shibboleth.idp.attribute.IdPAttribute;
import net.shibboleth.idp.attribute.IdPAttributeValue;
import net.shibboleth.idp.attribute.StringAttributeValue;
import net.shibboleth.idp.attribute.filter.context.AttributeFilterContext;
import net.shibboleth.idp.authn.AuthenticationResult;
import net.shibboleth.idp.authn.context.SubjectContext;
import net.shibboleth.idp.profile.context.RelyingPartyContext;
import net.shibboleth.idp.saml.authn.principal.AuthenticationMethodPrincipal;
import net.shibboleth.utilities.java.support.component.ComponentInitializationException;
import net.shibboleth.utilities.java.support.component.DestroyedComponentException;
import net.shibboleth.utilities.java.support.component.UninitializedComponentException;
import net.shibboleth.utilities.java.support.component.UnmodifiableComponentException;
import net.shibboleth.utilities.java.support.logic.ConstraintViolationException;
import net.shibboleth.utilities.java.support.scripting.EvaluableScript;
import net.shibboleth.utilities.java.support.testing.TestSupport;

/** {@link ScriptedMatcher} unit test. */
@ThreadSafe
@SuppressWarnings("javadoc")
public class ScriptedMatcherTest extends AbstractMatcherPolicyRuleTest {

    /** A script that returns a set that contains the one of values the attribute. */
    private EvaluableScript returnOneValueScript;

    /** A script that returns null. */
    private EvaluableScript nullReturnScript;

    /** A script that returns an object other than a set. */
    private EvaluableScript invalidReturnObjectScript;

    /** A script that returns a set contain values that were not attribute values. */
    private EvaluableScript addedValuesScript;

    /** A script that returns a set containing the prc name. */
    private EvaluableScript prcscScript;

    @BeforeTest public void setup() throws Exception {
        super.setUp();

        filterContext = new AttributeFilterContext();

        nullReturnScript = new EvaluableScript("JavaScript", "null;");

        if (!TestSupport.isJavaV8OrLater()) {
            returnOneValueScript =
                    new EvaluableScript("JavaScript", new StringBuilder().append("importPackage(Packages.java.util);")
                            .append("filterContext.getPrefilteredIdPAttributes();").append("x = new HashSet();")
                            .append("x.add(attribute.getValues().iterator().next());").append("x;").toString());

            invalidReturnObjectScript = new EvaluableScript("JavaScript", "new java.lang.String();");

            addedValuesScript =
                    new EvaluableScript("JavaScript", new StringBuilder().append("importPackage(Packages.java.util);")
                            .append("x = new HashSet();").append("x.add(attribute.getValues().iterator().next());")
                            .append("x.add(new net.shibboleth.idp.attribute.StringAttributeValue(\"a\"));")
                            .append("x;").toString());
            prcscScript =
                    new EvaluableScript(
                            "JavaScript",
                            new StringBuilder("importPackage(Packages.net.shibboleth.idp.attribute);")
                                    .append("x = new java.util.HashSet(1);\n")
                                    .append("x.add(new StringAttributeValue(profileContext.getClass().getName()));\n")
                                    .append("x.add(new StringAttributeValue(subjects[0].getPrincipals().iterator().next().getName()));\n")
                                    .append("x;").toString());
        } else {

            returnOneValueScript =
                    new EvaluableScript("JavaScript", new StringBuilder()
                            .append("load('nashorn:mozilla_compat.js');importPackage(Packages.java.util);")
                            .append("filterContext.getPrefilteredIdPAttributes();").append("x = new HashSet();")
                            .append("x.add(attribute.getValues().iterator().next());").append("x;").toString());

            invalidReturnObjectScript =
                    new EvaluableScript("JavaScript", "load('nashorn:mozilla_compat.js');new java.lang.String();");

            addedValuesScript =
                    new EvaluableScript("JavaScript", new StringBuilder()
                            .append("load('nashorn:mozilla_compat.js');importPackage(Packages.java.util);")
                            .append("importPackage(Packages.net.shibboleth.idp.attribute);")
                            .append("x = new HashSet();").append("x.add(attribute.getValues().iterator().next());")
                            .append("x.add(new StringAttributeValue(\"a\"));").append("x;").toString());
            prcscScript =
                    new EvaluableScript(
                            "JavaScript",
                            new StringBuilder("HashSet = Java.type(\"java.util.HashSet\");\n")
                                    .append("StringAttributeValue = Java.type(\"net.shibboleth.idp.attribute.StringAttributeValue\");\n")
                                    .append("x = new HashSet(1);\n")
                                    .append("x.add(new StringAttributeValue(profileContext.getClass().getName()));\n")
                                    .append("x.add(new StringAttributeValue(subjects[0].getPrincipals().iterator().next().getName()));\n")
                                    .append("x;").toString());

        }
    }

    @Test public void testGetMatcher() throws Exception {

        final ScriptedMatcher matcher = newScriptedMatcher(returnOneValueScript);
        matcher.setId("Test");
        matcher.initialize();

        assertNotNull(matcher.getScript());
    }

    @Test public void testNullArguments() throws Exception {

        ScriptedMatcher matcher = newScriptedMatcher(returnOneValueScript);
        matcher.setId("Test");
        matcher.initialize();

        try {
            matcher.getMatchingValues(null, filterContext);
            fail();
        } catch (final ConstraintViolationException e) {
            // expected this
        }

        try {
            matcher.getMatchingValues(attribute, null);
            fail();
        } catch (final ConstraintViolationException e) {
            // expected this
        }

        try {
            matcher.getMatchingValues(null, null);
            fail();
        } catch (final ConstraintViolationException e) {
            // expected this
        }

        matcher = newScriptedMatcher(returnOneValueScript);
        try {
            matcher.setScript(null);
        } catch (final ConstraintViolationException e) {
            // expected this
        }

        try {
            newScriptedMatcher(null);
            fail();
        } catch (final ConstraintViolationException e) {
            // expected this
        }
    }

    @Test public void testValidScript() throws Exception {
        final ScriptedMatcher matcher = newScriptedMatcher(returnOneValueScript);
        matcher.setId("Test");
        matcher.initialize();

        final Set<IdPAttributeValue> result = matcher.getMatchingValues(attribute, filterContext);
        assertNotNull(result);
        assertEquals(result.size(), 1);
        assertTrue(result.contains(value1) || result.contains(value2) || result.contains(value3));
    }
    
    @Test public void custom() throws Exception {
        
        final ScriptedMatcher matcher = newScriptedMatcher(new EvaluableScript("custom;"));
        final Set<IdPAttributeValue> custom = Collections.singleton(attribute.getValues().get(0));
        matcher.setId("Test");
        matcher.setCustomObject(custom);
        matcher.initialize();

        final Set<IdPAttributeValue> result = matcher.getMatchingValues(attribute, filterContext);
        assertNotNull(result);
        assertEquals(result.size(), 1);
        assertTrue(result.contains(value1) || result.contains(value2) || result.contains(value3));
    }


    @Test public void testNullReturnScript() throws Exception {

        final ScriptedMatcher matcher = newScriptedMatcher(nullReturnScript);
        matcher.setId("Test");
        matcher.initialize();

        assertNull(matcher.getMatchingValues(attribute, filterContext));
    }
    
    @Test public void testInvalidReturnObjectValue() throws Exception {

        final ScriptedMatcher matcher = newScriptedMatcher(invalidReturnObjectScript);
        matcher.setId("Test");
        matcher.initialize();

        assertNull(matcher.getMatchingValues(attribute, filterContext));
    }

    @Test public void testAddedValuesScript() throws Exception {

        final ScriptedMatcher matcher = newScriptedMatcher(addedValuesScript);
        matcher.setId("Test");
        matcher.initialize();

        final Set<IdPAttributeValue> result = matcher.getMatchingValues(attribute, filterContext);
        assertNotNull(result);
        assertEquals(result.size(), 1);
        assertTrue(result.contains(value1) || result.contains(value2) || result.contains(value3));
    }

    @Test public void testInitTeardown() throws ComponentInitializationException {

        final ScriptedMatcher matcher = newScriptedMatcher(returnOneValueScript);

        boolean thrown = false;
        try {
            matcher.getMatchingValues(attribute, filterContext);
        } catch (final UninitializedComponentException e) {
            thrown = true;
        }
        assertTrue(thrown, "getMatchingValues before init");

        matcher.setId("Test");
        matcher.initialize();
        matcher.getMatchingValues(attribute, filterContext);

        thrown = false;
        try {
            matcher.setScript(returnOneValueScript);
        } catch (final UnmodifiableComponentException e) {
            thrown = true;
        }

        matcher.destroy();

        thrown = false;
        try {
            matcher.initialize();
        } catch (final DestroyedComponentException e) {
            thrown = true;
        }
        assertTrue(thrown, "getMatchingValues after destroy");

        thrown = false;
        try {
            matcher.getMatchingValues(attribute, filterContext);
        } catch (final DestroyedComponentException e) {
            thrown = true;
        }
        assertTrue(thrown, "getMatchingValues after destroy");
    }

    @SuppressWarnings("unlikely-arg-type")
    @Test public void testEqualsHashToString() {

        final ScriptedMatcher matcher = newScriptedMatcher(addedValuesScript);

        matcher.toString();

        assertFalse(matcher.equals(null));
        assertTrue(matcher.equals(matcher));
        assertFalse(matcher.equals(this));

        ScriptedMatcher other = newScriptedMatcher(addedValuesScript);

        assertTrue(matcher.equals(other));
        assertEquals(matcher.hashCode(), other.hashCode());

        other = newScriptedMatcher(nullReturnScript);

        assertFalse(matcher.equals(other));
        assertNotSame(matcher.hashCode(), other.hashCode());

    }

    @Test public void testPrc() throws ComponentInitializationException, CloneNotSupportedException {
        final ScriptedMatcher matcher = newScriptedMatcher(prcscScript);

        matcher.setId("prc");
        matcher.initialize();
        final  ProfileRequestContext prc = new ProfileRequestContext();
        prc.getSubcontext(RelyingPartyContext.class, true).addSubcontext(filterContext);
        final SubjectContext sc = prc.getSubcontext(SubjectContext.class, true);
        
        final Subject subject = new Subject();
        subject.getPrincipals().add(new AuthenticationMethodPrincipal("FOO"));
        sc.getAuthenticationResults().put("one", new AuthenticationResult("1", subject));        
        
        Set<IdPAttributeValue> result = matcher.getMatchingValues(attribute, filterContext);
        assertEquals(result.size(), 0);

        final IdPAttribute newAttr = attribute.clone();

        final Set<IdPAttributeValue> s = new LinkedHashSet<>(2);
        s.add(new StringAttributeValue(ProfileRequestContext.class.getName()));
        s.add(new StringAttributeValue("BAR"));
        s.add(new StringAttributeValue("FOO"));
        newAttr.setValues(s);
        result = matcher.getMatchingValues(newAttr, filterContext);
        assertEquals(result.size(), 2);
        assertTrue(result.contains(new StringAttributeValue(ProfileRequestContext.class.getName())));
        assertTrue(result.contains(new StringAttributeValue("FOO")));
    }

    static public  ScriptedMatcher newScriptedMatcher(final EvaluableScript script) {
        final ScriptedMatcher what = new ScriptedMatcher();
        what.setScript(script);
        return what;
    }
}
