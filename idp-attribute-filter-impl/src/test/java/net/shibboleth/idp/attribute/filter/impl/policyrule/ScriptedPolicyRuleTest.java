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

package net.shibboleth.idp.attribute.filter.impl.policyrule;

import javax.annotation.concurrent.ThreadSafe;

import net.shibboleth.idp.attribute.filter.AttributeFilterContext;
import net.shibboleth.idp.attribute.filter.AttributeFilterException;
import net.shibboleth.idp.attribute.filter.PolicyRequirementRule.Tristate;
import net.shibboleth.idp.attribute.filter.impl.matcher.AbstractMatcherPolicyRuleTest;
import net.shibboleth.utilities.java.support.component.ComponentInitializationException;
import net.shibboleth.utilities.java.support.component.DestroyedComponentException;
import net.shibboleth.utilities.java.support.component.UninitializedComponentException;
import net.shibboleth.utilities.java.support.component.UnmodifiableComponentException;
import net.shibboleth.utilities.java.support.logic.ConstraintViolationException;
import net.shibboleth.utilities.java.support.scripting.EvaluableScript;

import org.testng.Assert;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.Test;

/** {@link ScriptedPolicyRule} unit test. */
@ThreadSafe
public class ScriptedPolicyRuleTest extends AbstractMatcherPolicyRuleTest {

    /** A script that returns null. */
    private EvaluableScript nullReturnScript;

    /** A script that returns Boolean.True. */
    private EvaluableScript trueReturnScript;

    /** A script that returns Boolean.false . */
    private EvaluableScript falseReturnScript;

    /** A script that returns an object other than a set. */
    private EvaluableScript invalidReturnObjectScript;

    @BeforeTest public void setup() throws Exception {
        super.setUp();

        filterContext = new AttributeFilterContext();

        nullReturnScript = new EvaluableScript("JavaScript", "null;");

        invalidReturnObjectScript = new EvaluableScript("JavaScript", "new java.lang.String();");

        trueReturnScript = new EvaluableScript("JavaScript", "new java.lang.Boolean(true);");

        falseReturnScript = new EvaluableScript("JavaScript", "new java.lang.Boolean(false);");
    }

    @Test public void testNullArguments() throws Exception {
        ScriptedPolicyRule matcher = new ScriptedPolicyRule(trueReturnScript);
        matcher.setId("Test");
        matcher.initialize();

        try {
            matcher.matches(null);
            Assert.fail();
        } catch (ConstraintViolationException e) {
            // expected this
        }

        try {
            new ScriptedPolicyRule(null);
            Assert.fail();
        } catch (ConstraintViolationException e) {
            // expected this
        }
    }

    @Test public void testNullReturnScript() throws Exception {
        ScriptedPolicyRule matcher = new ScriptedPolicyRule(nullReturnScript);
        matcher.setId("Test");
        matcher.initialize();

        Assert.assertEquals(matcher.matches(filterContext), Tristate.FAIL);
    }

    @Test public void testInvalidReturnObjectValue() throws Exception {
        ScriptedPolicyRule matcher = new ScriptedPolicyRule(invalidReturnObjectScript);
        matcher.setId("Test");
        matcher.initialize();

        Assert.assertEquals(matcher.matches(filterContext), Tristate.FAIL);
    }

    @Test public void testInitTeardown() throws AttributeFilterException, ComponentInitializationException {
        ScriptedPolicyRule matcher = new ScriptedPolicyRule(trueReturnScript);

        boolean thrown = false;
        try {
            matcher.matches(filterContext);
        } catch (UninitializedComponentException e) {
            thrown = true;
        }
        Assert.assertTrue(thrown, "matches before init");

        matcher.setId("Test");
        matcher.initialize();
        matcher.matches(filterContext);

        thrown = false;
        try {
            matcher.setScript(trueReturnScript);
        } catch (UnmodifiableComponentException e) {
            thrown = true;
        }

        matcher.destroy();

        thrown = false;
        try {
            matcher.initialize();
        } catch (DestroyedComponentException e) {
            thrown = true;
        }
        Assert.assertTrue(thrown, "init after destroy");

        thrown = false;
        try {
            matcher.matches(filterContext);
        } catch (DestroyedComponentException e) {
            thrown = true;
        }
        Assert.assertTrue(thrown, "matches after destroy");

    }

    @Test public void testEqualsHashToString() {
        ScriptedPolicyRule matcher = new ScriptedPolicyRule(trueReturnScript);

        matcher.toString();

        Assert.assertFalse(matcher.equals(null));
        Assert.assertTrue(matcher.equals(matcher));
        Assert.assertFalse(matcher.equals(this));

        ScriptedPolicyRule other = new ScriptedPolicyRule(trueReturnScript);

        Assert.assertTrue(matcher.equals(other));
        Assert.assertEquals(matcher.hashCode(), other.hashCode());

        other = new ScriptedPolicyRule(nullReturnScript);

        Assert.assertFalse(matcher.equals(other));
        Assert.assertNotSame(matcher.hashCode(), other.hashCode());

    }
    
    @Test public void testPredicate() throws ComponentInitializationException, AttributeFilterException {
        ScriptedPolicyRule matcher = new ScriptedPolicyRule(nullReturnScript);
        matcher.setId("test");
        matcher.initialize();

        matcher = new ScriptedPolicyRule(trueReturnScript);
        matcher.setId("test");
        matcher.initialize();
        Assert.assertEquals(matcher.matches(filterContext), Tristate.TRUE);
        
        matcher = new ScriptedPolicyRule(falseReturnScript);
        matcher.setId("test");
        matcher.initialize();
        Assert.assertEquals(matcher.matches(filterContext), Tristate.FALSE);

    }

}