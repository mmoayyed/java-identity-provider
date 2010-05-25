/*
 * Copyright 2010 University Corporation for Advanced Internet Development, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package edu.internet2.middleware.shibboleth.idp.consent.entities;

import java.util.Collection;
import java.util.HashSet;

import static org.testng.AssertJUnit.assertTrue;
import static org.testng.AssertJUnit.assertFalse;

import org.joda.time.DateTime;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;
import static org.testng.AssertJUnit.assertEquals;
import edu.internet2.middleware.shibboleth.idp.consent.BaseTest;
import edu.internet2.middleware.shibboleth.idp.consent.components.TermsOfUse;

/**
 *
 */

@Test
public class PrincipalTest extends BaseTest {
    
    Principal principal;
    
    @BeforeMethod
    public void beforeTest() {
        principal = new Principal("id", new DateTime(), new DateTime(), false);
    }
    
    public void equals() {
        Principal principalT1 = new Principal("id", new DateTime(), new DateTime(), false);
        Principal principalT2 = new Principal("id", new DateTime().minusDays(1), new DateTime().plusDays(1), true);
        Principal principalT3 = new Principal("other-id", new DateTime(), new DateTime(), false);
        
        assertEquals(principal, principalT1);
        assertEquals(principal, principalT2);
        assertFalse(principal.equals(principalT3));
    }
    
    public void acceptedTermsOfUse() {
        TermsOfUse termsOfUse = new TermsOfUse("version", "fingerprint");
        TermsOfUse otherTermsOfUse = new TermsOfUse("other-version", "other-fingerprint");
        
        AgreedTermsOfUse agreedTermsOfUse = new AgreedTermsOfUse(termsOfUse, new DateTime());
        AgreedTermsOfUse otherAgreedTermsOfUse = new AgreedTermsOfUse(otherTermsOfUse, new DateTime());

        Collection<AgreedTermsOfUse> agreedTermsOfUses = new HashSet<AgreedTermsOfUse>();
        agreedTermsOfUses.add(agreedTermsOfUse);
        agreedTermsOfUses.add(otherAgreedTermsOfUse);
        
        principal.setAgreedTermsOfUses(agreedTermsOfUses);
        
        assertTrue(principal.hasAcceptedTermsOfUse(termsOfUse));
        assertTrue(principal.hasAcceptedTermsOfUse(otherTermsOfUse));
        
    }
    
    public void notAcceptedTermsOfUse() {
        TermsOfUse termsOfUse = new TermsOfUse("version", "fingerprint");
        TermsOfUse otherTermsOfUse = new TermsOfUse("other-version", "other-fingerprint");
        
        TermsOfUse notTermsOfUse = new TermsOfUse("not-version", "not-fingerprint");
        TermsOfUse otherNotTermsOfUse = new TermsOfUse("version", "not-fingerprint");
        
        AgreedTermsOfUse agreedTermsOfUse = new AgreedTermsOfUse(termsOfUse, new DateTime());
        AgreedTermsOfUse otherAgreedTermsOfUse = new AgreedTermsOfUse(otherTermsOfUse, new DateTime());

        Collection<AgreedTermsOfUse> agreedTermsOfUses = new HashSet<AgreedTermsOfUse>();
        agreedTermsOfUses.add(agreedTermsOfUse);
        agreedTermsOfUses.add(otherAgreedTermsOfUse);
        
        assertFalse(principal.hasAcceptedTermsOfUse(termsOfUse));
        assertFalse(principal.hasAcceptedTermsOfUse(notTermsOfUse));
        
        principal.setAgreedTermsOfUses(agreedTermsOfUses);
        
        assertFalse(principal.hasAcceptedTermsOfUse(notTermsOfUse));
        assertFalse(principal.hasAcceptedTermsOfUse(otherNotTermsOfUse));    
    }
    
    @Test(dataProvider="relyingParty")
    public void approvedAttributes(RelyingParty relyingParty) {
        Attribute attribute = new Attribute("id", "valueHash");
        Attribute otherAttribute = new Attribute("other-id", "other-valueHash");
        Attribute approvedAttribute = new Attribute("approved-id", "approved-valueHash");

        AttributeReleaseConsent attributeReleaseConsent = new AttributeReleaseConsent(attribute, new DateTime());
        AttributeReleaseConsent otherAttributeReleaseConsent = new AttributeReleaseConsent(otherAttribute, new DateTime());
        AttributeReleaseConsent approvedAttributeReleaseConsent = new AttributeReleaseConsent(approvedAttribute, new DateTime());

        
        Collection<AttributeReleaseConsent> attributeReleaseConsents = new HashSet<AttributeReleaseConsent>();
        attributeReleaseConsents.add(attributeReleaseConsent);
        attributeReleaseConsents.add(otherAttributeReleaseConsent);
        
        principal.setAttributeReleaseConsents(relyingParty, attributeReleaseConsents);
        
        Collection<Attribute> attributes = new HashSet<Attribute>();
        attributes.add(attribute);
        attributes.add(otherAttribute);
        assertTrue(principal.hasApproved(attributes, relyingParty));
        
        attributeReleaseConsents.add(approvedAttributeReleaseConsent);
        assertTrue(principal.hasApproved(attributes, relyingParty));
        
    }
    
    @Test(dataProvider="relyingParty")
    public void notApprovedAttributes(RelyingParty relyingParty) {
        Attribute attribute = new Attribute("id", "valueHash");
        Attribute otherAttribute = new Attribute("other-id", "other-valueHash");
        
        Attribute notApprovedAttribute = new Attribute("not-approved-id", "not-approved-valueHash");
        Attribute changedApprovedAttribute = new Attribute("approved-id", "not-approved-valueHash");

        AttributeReleaseConsent attributeReleaseConsent = new AttributeReleaseConsent(attribute, new DateTime());
        AttributeReleaseConsent otherAttributeReleaseConsent = new AttributeReleaseConsent(otherAttribute, new DateTime());
        
        Collection<AttributeReleaseConsent> attributeReleaseConsents = new HashSet<AttributeReleaseConsent>();
        attributeReleaseConsents.add(attributeReleaseConsent);
        attributeReleaseConsents.add(otherAttributeReleaseConsent);
        
        Collection<Attribute> attributes = new HashSet<Attribute>();
        attributes.add(attribute);
        attributes.add(otherAttribute);
        
        assertFalse(principal.hasApproved(attributes, relyingParty));
        
        principal.setAttributeReleaseConsents(relyingParty, attributeReleaseConsents);
        
        attributes.add(notApprovedAttribute);
        assertFalse(principal.hasApproved(attributes, relyingParty));
        
        attributes.remove(notApprovedAttribute);  
        assertTrue(principal.hasApproved(attributes, relyingParty));
        
        attributes.add(changedApprovedAttribute);
        assertFalse(principal.hasApproved(attributes, relyingParty));
    }
}