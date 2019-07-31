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

package net.shibboleth.idp.attribute.resolver.impl;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.assertNull;
import static org.testng.Assert.assertSame;
import static org.testng.Assert.assertTrue;

import java.util.HashSet;
import java.util.function.Function;

import org.opensaml.messaging.context.navigate.ParentContextLookup;
import org.opensaml.profile.context.ProfileRequestContext;
import org.testng.annotations.Test;

import net.shibboleth.idp.attribute.IdPAttribute;
import net.shibboleth.idp.attribute.resolver.context.AttributeResolutionContext;
import net.shibboleth.idp.attribute.resolver.context.navigate.AttributeIssuerIdLookupFunction;
import net.shibboleth.idp.attribute.resolver.context.navigate.AttributePrincipalLookupFunction;
import net.shibboleth.idp.attribute.resolver.context.navigate.AttributeRecipientIdLookupFunction;

/** Unit test for {@link AttributeResolutionContext}. */
public class AttributeResolutionContextTest {
    
    static private final String THE_ISSUER = "Issuer"; 
    static private final String THE_RECIPIENT = "Recipient";
    static private final String THE_PRINCIPAL = "Principal"; 


    /** Test instantiation and post-instantiation state. */
    @Test public void instantiation() {

        AttributeResolutionContext context = new AttributeResolutionContext();
        assertNull(context.getParent());
        assertNotNull(context.getRequestedIdPAttributeNames());
        assertTrue(context.getRequestedIdPAttributeNames().isEmpty());
    }
    
    /** Test {@link AttributeResolutionContext#setRequestedIdPAttributeNames(java.util.Collection)}. */
    @Test public void setRequesedAttributes() {
        AttributeResolutionContext context = new AttributeResolutionContext();

        HashSet<String> attributes = new HashSet<>();
        context.setRequestedIdPAttributeNames(attributes);
        assertNotNull(context.getRequestedIdPAttributeNames());
        assertTrue(context.getRequestedIdPAttributeNames().isEmpty());

        attributes.add(null);
        context.setRequestedIdPAttributeNames(attributes);
        assertNotNull(context.getRequestedIdPAttributeNames());
        assertTrue(context.getRequestedIdPAttributeNames().isEmpty());

        attributes.add("foo");
        attributes.add(null);
        attributes.add("bar");
        context.setRequestedIdPAttributeNames(attributes);
        assertNotNull(context.getRequestedIdPAttributeNames());
        assertEquals(context.getRequestedIdPAttributeNames().size(), 2);

        attributes.clear();
        attributes.add("baz");
        context.setRequestedIdPAttributeNames(attributes);
        assertNotNull(context.getRequestedIdPAttributeNames());
        assertEquals(context.getRequestedIdPAttributeNames().size(), 1);
    }

    /** Test {@link AttributeResolutionContext#setRequestedIdPAttributeNames(java.util.Collection)}. */
    @Test public void setResolvedAttributes() {
        AttributeResolutionContext context = new AttributeResolutionContext();

        context.setResolvedIdPAttributes(null);
        assertNotNull(context.getResolvedIdPAttributes());
        assertTrue(context.getResolvedIdPAttributes().isEmpty());

        HashSet<IdPAttribute> attributes = new HashSet<>();
        context.setResolvedIdPAttributes(attributes);
        assertNotNull(context.getResolvedIdPAttributes());
        assertTrue(context.getResolvedIdPAttributes().isEmpty());

        attributes.add(null);
        context.setResolvedIdPAttributes(attributes);
        assertNotNull(context.getResolvedIdPAttributes());
        assertTrue(context.getResolvedIdPAttributes().isEmpty());

        attributes.add(new IdPAttribute("foo"));
        attributes.add(null);
        attributes.add(new IdPAttribute("bar"));
        context.setResolvedIdPAttributes(attributes);
        assertNotNull(context.getResolvedIdPAttributes());
        assertEquals(context.getResolvedIdPAttributes().size(), 2);

        attributes.clear();
        attributes.add(new IdPAttribute("baz"));
        context.setResolvedIdPAttributes(attributes);
        assertNotNull(context.getResolvedIdPAttributes());
        assertEquals(context.getResolvedIdPAttributes().size(), 1);
    }
    
    @Test public void lookupsParent() {
        final ProfileRequestContext profileCtx = new ProfileRequestContext();
        final AttributeResolutionContext context = profileCtx.getSubcontext(AttributeResolutionContext.class, true);
        
        context.setPrincipal(THE_PRINCIPAL);
        context.setAttributeIssuerID(THE_ISSUER);
        context.setAttributeRecipientID(THE_RECIPIENT);
        
        assertSame(context.getPrincipal(), THE_PRINCIPAL);
        assertSame(context.getAttributeIssuerID(), THE_ISSUER);
        assertSame(context.getAttributeRecipientID(), THE_RECIPIENT);
        
        final Function<ProfileRequestContext,String> principalFn = new AttributePrincipalLookupFunction();
        final Function<ProfileRequestContext,String> recipientFn = new AttributeRecipientIdLookupFunction();
        final Function<ProfileRequestContext,String> issuerFn = new AttributeIssuerIdLookupFunction();
        
        assertSame(principalFn.apply(profileCtx), THE_PRINCIPAL);
        assertSame(issuerFn.apply(profileCtx), THE_ISSUER);
        assertSame(recipientFn.apply(profileCtx), THE_RECIPIENT);
    }
        
    @Test public void lookupsChild() {
        final AttributeResolutionContext context = new AttributeResolutionContext();
        final ProfileRequestContext profileCtx = context.getSubcontext(ProfileRequestContext.class, true);
        
        context.setPrincipal(THE_PRINCIPAL);
        context.setAttributeIssuerID(THE_ISSUER);
        context.setAttributeRecipientID(THE_RECIPIENT);
        
        final AttributePrincipalLookupFunction principalFn = new AttributePrincipalLookupFunction();
        final AttributeRecipientIdLookupFunction recipientFn = new AttributeRecipientIdLookupFunction();
        final AttributeIssuerIdLookupFunction issuerFn = new AttributeIssuerIdLookupFunction();

        assertNull(principalFn.apply(profileCtx), THE_PRINCIPAL);
        assertNull(issuerFn.apply(profileCtx), THE_ISSUER);
        assertNull(recipientFn.apply(profileCtx), THE_RECIPIENT);

        principalFn.setAttributeResolutionContextLookupStrategy(new ParentContextLookup<ProfileRequestContext, AttributeResolutionContext>());
        recipientFn.setAttributeResolutionContextLookupStrategy(new ParentContextLookup<ProfileRequestContext, AttributeResolutionContext>());
        issuerFn.setAttributeResolutionContextLookupStrategy(new ParentContextLookup<ProfileRequestContext, AttributeResolutionContext>());
        
        assertSame(principalFn.apply(profileCtx), THE_PRINCIPAL);
        assertSame(issuerFn.apply(profileCtx), THE_ISSUER);
        assertSame(recipientFn.apply(profileCtx), THE_RECIPIENT);
    }
        

}
