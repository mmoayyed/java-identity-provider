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

package net.shibboleth.idp.profile.impl;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;

import net.shibboleth.idp.attribute.IdPAttribute;
import net.shibboleth.idp.attribute.AttributeContext;
import net.shibboleth.idp.attribute.IdPAttributeValue;
import net.shibboleth.idp.attribute.StringAttributeValue;
import net.shibboleth.idp.attribute.filter.AttributeFilter;
import net.shibboleth.idp.attribute.filter.AttributeFilterContext;
import net.shibboleth.idp.attribute.filter.AttributeFilterPolicy;
import net.shibboleth.idp.attribute.filter.AttributeRule;
import net.shibboleth.idp.attribute.filter.MockMatcher;
import net.shibboleth.idp.attribute.filter.PolicyRequirementRule;
import net.shibboleth.idp.profile.ActionTestingSupport;
import net.shibboleth.idp.profile.RequestContextBuilder;
import net.shibboleth.idp.relyingparty.RelyingPartyContext;

import org.opensaml.profile.action.EventIds;
import org.opensaml.profile.context.ProfileRequestContext;
import org.springframework.webflow.execution.Event;
import org.springframework.webflow.test.MockRequestContext;
import org.testng.Assert;
import org.testng.annotations.Test;

import com.google.common.collect.Lists;

/** {@link FilterAttributes} unit test. */
public class FilterAttributesTest {

    /** Test that the action errors out properly if there is no relying party context. */
    @Test public void testNoRelyingPartyContext() throws Exception {
        ProfileRequestContext profileCtx = new ProfileRequestContext();

        AttributeFilter engine = new AttributeFilter("test", null);

        FilterAttributes action = new FilterAttributes(engine);
        action.setId("test");
        action.initialize();

        Event result = action.doExecute(new MockRequestContext(), profileCtx);

        ActionTestingSupport.assertEvent(result, EventIds.INVALID_RELYING_PARTY_CTX);
    }

    /** Test that the action errors out properly if there is no attribute context. */
    @Test public void testNoAttributeContext() throws Exception {
        ProfileRequestContext profileCtx = new RequestContextBuilder().buildProfileRequestContext();

        AttributeFilter engine = new AttributeFilter("test", null);

        FilterAttributes action = new FilterAttributes(engine);
        action.setId("test");
        action.initialize();

        Event result = action.doExecute(new MockRequestContext(), profileCtx);

        ActionTestingSupport.assertEvent(result, EventIds.INVALID_ATTRIBUTE_CTX);
    }

    /** Test that the action proceeds properly if there are no attributes to filter . */
    @Test public void testNoAttributes() throws Exception {
        ProfileRequestContext profileCtx = new RequestContextBuilder().buildProfileRequestContext();

        AttributeContext attribCtx = new AttributeContext();
        profileCtx.getSubcontext(RelyingPartyContext.class).addSubcontext(attribCtx);

        AttributeFilter engine = new AttributeFilter("test", null);

        FilterAttributes action = new FilterAttributes(engine);
        action.setId("test");
        action.initialize();

        Event result = action.doExecute(new MockRequestContext(), profileCtx);

        ActionTestingSupport.assertProceedEvent(result);
    }

    /** Test that the action filters attributes and proceeds properly while auto-creating a filter context. */
    @Test public void testFilterAttributesAutoCreateFilterContext() throws Exception {
        IdPAttribute attribute1 = new IdPAttribute("attribute1");
        attribute1.setValues(Lists.newArrayList(new StringAttributeValue("one"), new StringAttributeValue("two")));

        IdPAttribute attribute2 = new IdPAttribute("attribute2");
        attribute2.setValues(Lists.newArrayList(new StringAttributeValue("a"), new StringAttributeValue("b")));

        List<IdPAttribute> attributes = Arrays.asList(attribute1, attribute2);

        MockMatcher attribute1Matcher = new MockMatcher();
        attribute1Matcher.setMatchingAttribute("attribute1");
        attribute1Matcher.setMatchingValues(null);

        AttributeRule attribute1Policy = new AttributeRule();
        attribute1Policy.setId("attribute1Policy");
        attribute1Policy.setAttributeId("attribute1");
        attribute1Policy.setMatcher(attribute1Matcher);
        attribute1Policy.setIsDenyRule(false);

        AttributeFilterPolicy policy =
                new AttributeFilterPolicy("attribute1Policy", PolicyRequirementRule.MATCHES_ALL,
                        Lists.newArrayList(attribute1Policy));

        AttributeFilter engine = new AttributeFilter("engine", Lists.newArrayList(policy));
        engine.initialize();

        ProfileRequestContext profileCtx = new RequestContextBuilder().buildProfileRequestContext();

        AttributeContext attributeCtx = new AttributeContext();
        attributeCtx.setIdPAttributes(attributes);
        profileCtx.getSubcontext(RelyingPartyContext.class).addSubcontext(attributeCtx);

        FilterAttributes action = new FilterAttributes(engine);
        action.setId("test");
        action.initialize();

        Event result = action.doExecute(new MockRequestContext(), profileCtx);

        ActionTestingSupport.assertProceedEvent(result);

        // The attribute filter context should be removed by the filter attributes action.
        Assert.assertNull(profileCtx.getSubcontext(RelyingPartyContext.class).getSubcontext(
                AttributeFilterContext.class));

        AttributeContext resultAttributeCtx =
                profileCtx.getSubcontext(RelyingPartyContext.class).getSubcontext(AttributeContext.class);
        Assert.assertNotNull(resultAttributeCtx);

        Map<String, IdPAttribute> resultAttributes = resultAttributeCtx.getIdPAttributes();
        Assert.assertEquals(resultAttributes.size(), 1);

        Set<IdPAttributeValue<?>> resultAttributeValue = resultAttributes.get("attribute1").getValues();
        Assert.assertEquals(resultAttributeValue.size(), 2);
        Assert.assertTrue(resultAttributeValue.contains(new StringAttributeValue("one")));
        Assert.assertTrue(resultAttributeValue.contains(new StringAttributeValue("two")));
    }

    /** Test that the action filters attributes and proceeds properly with an existing filter context. */
    @Test public void testFilterAttributesExistingFilterContext() throws Exception {
        IdPAttribute attribute1 = new IdPAttribute("attribute1");
        attribute1.setValues(Lists.newArrayList(new StringAttributeValue("one"), new StringAttributeValue("two")));

        IdPAttribute attribute2 = new IdPAttribute("attribute2");
        attribute2.setValues(Lists.newArrayList(new StringAttributeValue("a"), new StringAttributeValue("b")));

        List<IdPAttribute> attributes = Arrays.asList(attribute1, attribute2);

        MockMatcher attribute1Matcher = new MockMatcher();
        attribute1Matcher.setMatchingAttribute("attribute1");
        attribute1Matcher.setMatchingValues(null);

        AttributeRule attribute1Policy = new AttributeRule();
        attribute1Policy.setId("attribute1Policy");
        attribute1Policy.setAttributeId("attribute1");
        attribute1Policy.setMatcher(attribute1Matcher);
        attribute1Policy.setIsDenyRule(false);

        AttributeFilterPolicy policy =
                new AttributeFilterPolicy("attribute1Policy", PolicyRequirementRule.MATCHES_ALL,
                        Lists.newArrayList(attribute1Policy));

        AttributeFilter engine = new AttributeFilter("engine", Lists.newArrayList(policy));
        engine.initialize();

        ProfileRequestContext profileCtx = new RequestContextBuilder().buildProfileRequestContext();

        AttributeContext attributeCtx = new AttributeContext();
        attributeCtx.setIdPAttributes(attributes);
        profileCtx.getSubcontext(RelyingPartyContext.class).addSubcontext(attributeCtx);

        AttributeFilterContext attributeFilterCtx = new AttributeFilterContext();
        profileCtx.getSubcontext(RelyingPartyContext.class).addSubcontext(attributeFilterCtx);

        FilterAttributes action = new FilterAttributes(engine);
        action.setId("test");
        action.initialize();

        Event result = action.doExecute(new MockRequestContext(), profileCtx);

        ActionTestingSupport.assertProceedEvent(result);

        // The attribute filter context should be removed by the filter attributes action.
        Assert.assertNull(profileCtx.getSubcontext(RelyingPartyContext.class).getSubcontext(
                AttributeFilterContext.class));

        AttributeContext resultAttributeCtx =
                profileCtx.getSubcontext(RelyingPartyContext.class).getSubcontext(AttributeContext.class);
        Assert.assertNotNull(resultAttributeCtx);

        Map<String, IdPAttribute> resultAttributes = resultAttributeCtx.getIdPAttributes();
        Assert.assertEquals(resultAttributes.size(), 1);

        Set<IdPAttributeValue<?>> resultAttributeValue = resultAttributes.get("attribute1").getValues();
        Assert.assertEquals(resultAttributeValue.size(), 2);
        Assert.assertTrue(resultAttributeValue.contains(new StringAttributeValue("one")));
        Assert.assertTrue(resultAttributeValue.contains(new StringAttributeValue("two")));
    }

    /** Test that action returns the proper event if the attributes are not able to be filtered. */
    @Test public void testUnableToFilterAttributes() throws Exception {
        IdPAttribute attribute1 = new MockUncloneableAttribute("attribute1");
        attribute1.setValues(Lists.newArrayList(new StringAttributeValue("one"), new StringAttributeValue("two")));

        List<IdPAttribute> attributes = Arrays.asList(attribute1);

        MockMatcher attribute1Matcher = new MockMatcher();
        attribute1Matcher.setMatchingAttribute("attribute1");
        attribute1Matcher.setMatchingValues(null);

        AttributeRule attribute1Policy = new AttributeRule();
        attribute1Policy.setId("attribute1Policy");
        attribute1Policy.setAttributeId("attribute1");
        attribute1Policy.setMatcher(attribute1Matcher);
        attribute1Policy.setIsDenyRule(false);

        AttributeFilterPolicy policy =
                new AttributeFilterPolicy("attribute1Policy", PolicyRequirementRule.MATCHES_ALL,
                        Lists.newArrayList(attribute1Policy));

        AttributeFilter engine = new AttributeFilter("engine", Lists.newArrayList(policy));
        engine.initialize();

        ProfileRequestContext profileCtx = new RequestContextBuilder().buildProfileRequestContext();

        AttributeContext attributeCtx = new AttributeContext();
        attributeCtx.setIdPAttributes(attributes);
        profileCtx.getSubcontext(RelyingPartyContext.class).addSubcontext(attributeCtx);

        AttributeFilterContext attributeFilterCtx = new AttributeFilterContext();
        profileCtx.getSubcontext(RelyingPartyContext.class).addSubcontext(attributeFilterCtx);

        FilterAttributes action = new FilterAttributes(engine);
        action.setId("test");
        action.initialize();

        Event result = action.doExecute(new MockRequestContext(), profileCtx);

        ActionTestingSupport.assertEvent(result, FilterAttributes.UNABLE_FILTER_ATTRIBS);
    }

    /** {@link IdPAttribute} which always throws a {@link CloneNotSupportedException}. */
    private class MockUncloneableAttribute extends IdPAttribute {

        /**
         * Constructor.
         * 
         * @param attributeId
         */
        public MockUncloneableAttribute(String attributeId) {
            super(attributeId);
        }

        /** Always throws exception. */
        public IdPAttribute clone() throws CloneNotSupportedException {
            throw new CloneNotSupportedException();
        }
    }
}
