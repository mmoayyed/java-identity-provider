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

import javax.annotation.Nullable;

import net.shibboleth.idp.attribute.IdPAttribute;
import net.shibboleth.idp.attribute.StringAttributeValue;
import net.shibboleth.idp.attribute.context.AttributeContext;
import net.shibboleth.idp.attribute.resolver.AttributeDefinition;
import net.shibboleth.idp.attribute.resolver.AttributeResolver;
import net.shibboleth.idp.attribute.resolver.MockAttributeDefinition;
import net.shibboleth.idp.attribute.resolver.ResolutionException;
import net.shibboleth.idp.attribute.resolver.context.AttributeResolutionContext;
import net.shibboleth.idp.attribute.resolver.impl.AttributeResolverImpl;
import net.shibboleth.idp.authn.context.SubjectContext;
import net.shibboleth.idp.profile.IdPEventIds;
import net.shibboleth.idp.profile.RequestContextBuilder;
import net.shibboleth.idp.relyingparty.RelyingPartyContext;
import net.shibboleth.idp.service.AbstractReloadableService;
import net.shibboleth.idp.service.ServiceableComponent;
import net.shibboleth.utilities.java.support.collection.LazySet;

import org.opensaml.profile.action.ActionTestingSupport;
import org.opensaml.profile.context.ProfileRequestContext;
import org.testng.Assert;
import org.testng.annotations.Test;

/** {@link ResolveAttributes} unit test. */
public class ResolveAttributesTest {

    /** Test that the action errors out properly if there is no relying party context. */
    @Test public void testNoRelyingPartyContext() throws Exception {
        final ProfileRequestContext profileCtx = new ProfileRequestContext();

        final IdPAttribute attribute = new IdPAttribute("ad1");
        attribute.getValues().add(new StringAttributeValue("value1"));

        final LazySet<AttributeDefinition> definitions = new LazySet<>();
        definitions.add(new MockAttributeDefinition("ad1", attribute));

        final AttributeResolverImpl resolver = new AttributeResolverImpl("resolver", definitions, null, null);
        resolver.initialize();

        final ResolveAttributes action = new ResolveAttributes(new AttributeService(resolver));
        action.setId("test");
        action.initialize();

        action.execute(profileCtx);
        ActionTestingSupport.assertEvent(profileCtx, IdPEventIds.INVALID_RELYING_PARTY_CTX);
    }

    /** Test that the action resolves attributes and proceeds properly. */
    @Test public void testResolveAttributes() throws Exception {
        final ProfileRequestContext profileCtx = new RequestContextBuilder().buildProfileRequestContext();
        profileCtx.getSubcontext(SubjectContext.class, true);

        final IdPAttribute attribute = new IdPAttribute("ad1");
        attribute.getValues().add(new StringAttributeValue("value1"));

        final LazySet<AttributeDefinition> definitions = new LazySet<>();
        definitions.add(new MockAttributeDefinition("ad1", attribute));

        final AttributeResolverImpl resolver = new AttributeResolverImpl("resolver", definitions, null, null);
        resolver.initialize();

        final ResolveAttributes action = new ResolveAttributes(new AttributeService(resolver));
        action.setId("test");
        action.initialize();

        action.execute(profileCtx);
        ActionTestingSupport.assertProceedEvent(profileCtx);

        // The attribute resolution context should be removed by the resolve attributes action.
        Assert.assertNull(profileCtx.getSubcontext(AttributeResolutionContext.class));

        AttributeContext resolvedAttributeCtx =
                profileCtx.getSubcontext(RelyingPartyContext.class).getSubcontext(AttributeContext.class);
        Assert.assertNotNull(resolvedAttributeCtx);

        final Map<String, IdPAttribute> resolvedAttributes = resolvedAttributeCtx.getIdPAttributes();
        Assert.assertFalse(resolvedAttributes.isEmpty());
        Assert.assertEquals(resolvedAttributes.size(), 1);
        Assert.assertNotNull(resolvedAttributes.get("ad1"));
        Assert.assertEquals(resolvedAttributes.get("ad1"), attribute);
    }

    @Test public void testResolveSpecificAttributes() throws Exception {
        ProfileRequestContext profileCtx = new RequestContextBuilder().buildProfileRequestContext();
        profileCtx.getSubcontext(SubjectContext.class, true);

        final IdPAttribute attribute = new IdPAttribute("ad1");
        attribute.getValues().add(new StringAttributeValue("value1"));

        final LazySet<AttributeDefinition> definitions = new LazySet<>();
        definitions.add(new MockAttributeDefinition("ad1", attribute));

        final AttributeResolverImpl resolver = new AttributeResolverImpl("resolver", definitions, null, null);
        resolver.initialize();

        AttributeResolutionContext attributeResolutionCtx = new AttributeResolutionContext();
        attributeResolutionCtx.setRequestedIdPAttributeNames(Collections.singleton("ad1"));
        profileCtx.addSubcontext(attributeResolutionCtx);

        final ResolveAttributes action = new ResolveAttributes(new AttributeService(resolver));
        action.setId("test");
        action.initialize();

        action.execute(profileCtx);
        ActionTestingSupport.assertProceedEvent(profileCtx);

        // The attribute resolution context should be removed by the resolve attributes action.
        Assert.assertNull(profileCtx.getSubcontext(AttributeResolutionContext.class));

        AttributeContext resolvedAttributeCtx =
                profileCtx.getSubcontext(RelyingPartyContext.class).getSubcontext(AttributeContext.class);
        Assert.assertNotNull(resolvedAttributeCtx);

        final Map<String, IdPAttribute> resolvedAttributes = resolvedAttributeCtx.getIdPAttributes();
        Assert.assertFalse(resolvedAttributes.isEmpty());
        Assert.assertEquals(resolvedAttributes.size(), 1);
        Assert.assertNotNull(resolvedAttributes.get("ad1"));
        Assert.assertEquals(resolvedAttributes.get("ad1"), attribute);

        // now test requesting an attribute that does not exist
        profileCtx = new RequestContextBuilder().buildProfileRequestContext();
        profileCtx.getSubcontext(SubjectContext.class, true);

        attributeResolutionCtx = new AttributeResolutionContext();
        attributeResolutionCtx.setRequestedIdPAttributeNames(Collections.singleton("dne"));
        profileCtx.addSubcontext(attributeResolutionCtx, true);

        action.execute(profileCtx);
        ActionTestingSupport.assertProceedEvent(profileCtx);

        // The attribute resolution context should be removed by the resolve attributes action.
        Assert.assertNull(profileCtx.getSubcontext(AttributeResolutionContext.class));

        resolvedAttributeCtx =
                profileCtx.getSubcontext(RelyingPartyContext.class).getSubcontext(AttributeContext.class);
        Assert.assertNotNull(resolvedAttributeCtx);
        Assert.assertTrue(resolvedAttributeCtx.getIdPAttributes().isEmpty());
    }

    /** Test that action returns the proper event if the attributes are not able to be resolved. */
    @Test public void testUnableToResolveAttributes() throws Exception {
        final ProfileRequestContext profileCtx = new RequestContextBuilder().buildProfileRequestContext();
        profileCtx.getSubcontext(SubjectContext.class, true);

        final IdPAttribute attribute = new IdPAttribute("ad1");
        attribute.getValues().add(new StringAttributeValue("value1"));

        final LazySet<AttributeDefinition> definitions = new LazySet<>();
        definitions.add(new MockAttributeDefinition("ad1", new ResolutionException()));

        final AttributeResolverImpl resolver = new AttributeResolverImpl("resolver", definitions, null, null);
        resolver.initialize();

        final ResolveAttributes action = new ResolveAttributes(new AttributeService(resolver));
        action.setId("test");
        action.initialize();

        action.execute(profileCtx);
        ActionTestingSupport.assertEvent(profileCtx, IdPEventIds.UNABLE_RESOLVE_ATTRIBS);
    }
    
    /** Test that action returns the proper event if the attribute configuration is broken */
    @Test public void testUnableToFindResolver() throws Exception {
        final ProfileRequestContext profileCtx = new RequestContextBuilder().buildProfileRequestContext();
        profileCtx.getSubcontext(SubjectContext.class, true);

        final IdPAttribute attribute = new IdPAttribute("ad1");
        attribute.getValues().add(new StringAttributeValue("value1"));

        final LazySet<AttributeDefinition> definitions = new LazySet<>();
        definitions.add(new MockAttributeDefinition("ad1", new ResolutionException()));

        final ResolveAttributes action = new ResolveAttributes(new AttributeService(null));
        action.setId("test");
        action.initialize();

        action.execute(profileCtx);
        ActionTestingSupport.assertEvent(profileCtx, IdPEventIds.UNABLE_RESOLVE_ATTRIBS);
    }

    
    private static class AttributeService extends AbstractReloadableService<AttributeResolver> {
        
        private ServiceableComponent<AttributeResolver> component;
        
        protected AttributeService(ServiceableComponent<AttributeResolver> what) {
            component = what;
        }

        /** {@inheritDoc} */
        @Override
        @Nullable public ServiceableComponent<AttributeResolver> getServiceableComponent() {
            if (null == component) {
                return null;
            }
            component.pinComponent();
            return component;
        }

        /** {@inheritDoc} */
        @Override
        protected boolean shouldReload() {
            return false;
        }
        
    }
    
}