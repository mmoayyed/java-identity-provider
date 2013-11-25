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

import java.util.Collections;
import java.util.Map;

import net.shibboleth.idp.attribute.IdPAttribute;
import net.shibboleth.idp.attribute.AttributeContext;
import net.shibboleth.idp.attribute.StringAttributeValue;
import net.shibboleth.idp.attribute.resolver.AttributeResolutionContext;
import net.shibboleth.idp.attribute.resolver.AttributeResolverImpl;
import net.shibboleth.idp.attribute.resolver.ResolutionException;
import net.shibboleth.idp.attribute.resolver.AttributeResolver;
import net.shibboleth.idp.attribute.resolver.AttributeDefinition;
import net.shibboleth.idp.attribute.resolver.MockAttributeDefinition;
import net.shibboleth.idp.profile.ActionTestingSupport;

import org.opensaml.profile.action.EventIds;
import org.opensaml.profile.context.ProfileRequestContext;
import net.shibboleth.idp.profile.RequestContextBuilder;
import net.shibboleth.idp.relyingparty.RelyingPartyContext;
import net.shibboleth.utilities.java.support.collection.LazySet;

import org.springframework.webflow.execution.Event;
import org.springframework.webflow.test.MockRequestContext;
import org.testng.Assert;
import org.testng.annotations.Test;

/** {@link ResolveAttributes} unit test. */
public class ResolveAttributesTest {

    /** Test that the action errors out properly if there is no relying party context. */
    @Test public void testNoRelyingPartyContext() throws Exception {
        ProfileRequestContext profileCtx = new ProfileRequestContext();

        IdPAttribute attribute = new IdPAttribute("ad1");
        attribute.getValues().add(new StringAttributeValue("value1"));

        LazySet<AttributeDefinition> definitions = new LazySet<AttributeDefinition>();
        definitions.add(new MockAttributeDefinition("ad1", attribute));

        AttributeResolver resolver = new AttributeResolverImpl("resolver", definitions, null);
        resolver.initialize();

        ResolveAttributes action = new ResolveAttributes(resolver);
        action.setId("test");
        action.initialize();

        Event result = action.doExecute(new MockRequestContext(), profileCtx);

        ActionTestingSupport.assertEvent(result, EventIds.INVALID_RELYING_PARTY_CTX);
    }

    /** Test that the action resolves attributes and proceeds properly. */
    @Test public void testResolveAttributes() throws Exception {
        ProfileRequestContext profileCtx = new RequestContextBuilder().buildProfileRequestContext();

        IdPAttribute attribute = new IdPAttribute("ad1");
        attribute.getValues().add(new StringAttributeValue("value1"));

        LazySet<AttributeDefinition> definitions = new LazySet<AttributeDefinition>();
        definitions.add(new MockAttributeDefinition("ad1", attribute));

        AttributeResolver resolver = new AttributeResolverImpl("resolver", definitions, null);
        resolver.initialize();

        ResolveAttributes action = new ResolveAttributes(resolver);
        action.setId("test");
        action.initialize();

        Event result = action.doExecute(new MockRequestContext(), profileCtx);

        ActionTestingSupport.assertProceedEvent(result);

        // The attribute resolution context should be removed by the resolve attributes action.
        Assert.assertNull(profileCtx.getSubcontext(RelyingPartyContext.class).getSubcontext(
                AttributeResolutionContext.class));

        AttributeContext resolvedAttributeCtx =
                profileCtx.getSubcontext(RelyingPartyContext.class).getSubcontext(AttributeContext.class);
        Assert.assertNotNull(resolvedAttributeCtx);

        Map<String, IdPAttribute> resolvedAttributes = resolvedAttributeCtx.getIdPAttributes();
        Assert.assertFalse(resolvedAttributes.isEmpty());
        Assert.assertEquals(resolvedAttributes.size(), 1);
        Assert.assertNotNull(resolvedAttributes.get("ad1"));
        Assert.assertEquals(resolvedAttributes.get("ad1"), attribute);
    }

    @Test public void testResolveSpecificAttributes() throws Exception {
        ProfileRequestContext profileCtx = new RequestContextBuilder().buildProfileRequestContext();

        IdPAttribute attribute = new IdPAttribute("ad1");
        attribute.getValues().add(new StringAttributeValue("value1"));

        LazySet<AttributeDefinition> definitions = new LazySet<AttributeDefinition>();
        definitions.add(new MockAttributeDefinition("ad1", attribute));

        AttributeResolver resolver = new AttributeResolverImpl("resolver", definitions, null);
        resolver.initialize();

        AttributeResolutionContext attributeResolutionCtx = new AttributeResolutionContext();
        attributeResolutionCtx.setRequestedIdPAttributes(Collections.singleton(new IdPAttribute("ad1")));
        profileCtx.addSubcontext(attributeResolutionCtx);

        ResolveAttributes action = new ResolveAttributes(resolver);
        action.setId("test");
        action.initialize();

        Event result = action.doExecute(new MockRequestContext(), profileCtx);

        ActionTestingSupport.assertProceedEvent(result);

        // The attribute resolution context should be removed by the resolve attributes action.
        Assert.assertNull(profileCtx.getSubcontext(RelyingPartyContext.class).getSubcontext(
                AttributeResolutionContext.class));

        AttributeContext resolvedAttributeCtx =
                profileCtx.getSubcontext(RelyingPartyContext.class).getSubcontext(AttributeContext.class);
        Assert.assertNotNull(resolvedAttributeCtx);

        Map<String, IdPAttribute> resolvedAttributes = resolvedAttributeCtx.getIdPAttributes();
        Assert.assertFalse(resolvedAttributes.isEmpty());
        Assert.assertEquals(resolvedAttributes.size(), 1);
        Assert.assertNotNull(resolvedAttributes.get("ad1"));
        Assert.assertEquals(resolvedAttributes.get("ad1"), attribute);

        // now test requesting an attribute that does not exist
        profileCtx = new RequestContextBuilder().buildProfileRequestContext();

        attributeResolutionCtx = new AttributeResolutionContext();
        attributeResolutionCtx.setRequestedIdPAttributes(Collections.singleton(new IdPAttribute("dne")));
        profileCtx.addSubcontext(attributeResolutionCtx, true);

        result = action.doExecute(new MockRequestContext(), profileCtx);

        ActionTestingSupport.assertProceedEvent(result);

        // The attribute resolution context should be removed by the resolve attributes action.
        Assert.assertNull(profileCtx.getSubcontext(RelyingPartyContext.class).getSubcontext(
                AttributeResolutionContext.class));

        resolvedAttributeCtx =
                profileCtx.getSubcontext(RelyingPartyContext.class).getSubcontext(AttributeContext.class);
        Assert.assertNotNull(resolvedAttributeCtx);
        Assert.assertTrue(resolvedAttributeCtx.getIdPAttributes().isEmpty());
    }

    /** Test that action returns the proper event if the attributes are not able to be resolved. */
    @Test public void testUnableToResolveAttributes() throws Exception {
        ProfileRequestContext profileCtx = new RequestContextBuilder().buildProfileRequestContext();

        IdPAttribute attribute = new IdPAttribute("ad1");
        attribute.getValues().add(new StringAttributeValue("value1"));

        LazySet<AttributeDefinition> definitions = new LazySet<AttributeDefinition>();
        definitions.add(new MockAttributeDefinition("ad1", new ResolutionException()));

        AttributeResolver resolver = new AttributeResolverImpl("resolver", definitions, null);
        resolver.initialize();

        ResolveAttributes action = new ResolveAttributes(resolver);
        action.setId("test");
        action.initialize();

        Event result = action.doExecute(new MockRequestContext(), profileCtx);

        ActionTestingSupport.assertEvent(result, ResolveAttributes.UNABLE_RESOLVE_ATTRIBS);
    }
}
