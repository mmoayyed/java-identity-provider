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

package net.shibboleth.idp.consent.logic.impl;

import static org.testng.Assert.assertTrue;

import java.util.HashMap;
import java.util.Map;

import net.shibboleth.idp.attribute.IdPAttribute;
import net.shibboleth.idp.consent.Consent;
import net.shibboleth.idp.consent.context.AttributeReleaseContext;
import net.shibboleth.idp.consent.context.ConsentContext;
import net.shibboleth.idp.consent.flow.ar.impl.AttributeReleaseFlowDescriptor;
import net.shibboleth.idp.consent.flow.impl.ConsentFlowDescriptor;
import net.shibboleth.idp.consent.impl.ConsentTestingSupport;
import net.shibboleth.idp.consent.impl.ConsentTestingSupport.MapType;
import net.shibboleth.idp.profile.context.ProfileInterceptorContext;
import net.shibboleth.idp.profile.context.navigate.WebflowRequestContextProfileRequestContextLookup;
import net.shibboleth.idp.profile.interceptor.ProfileInterceptorFlowDescriptor;
import net.shibboleth.idp.profile.testing.RequestContextBuilder;

import org.opensaml.profile.context.ProfileRequestContext;
import org.springframework.webflow.execution.RequestContext;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

/** {@link AttributeReleaseConsentFunction} unit test. */
@SuppressWarnings("javadoc")
public class AttributeReleaseConsentFunctionTest {

    private RequestContext src;

    private ProfileRequestContext prc;

    private AttributeReleaseFlowDescriptor flowDescriptor;
    
    private AttributeReleaseConsentFunction function;

    @BeforeMethod public void setUp() throws Exception {
        src = new RequestContextBuilder().buildRequestContext();
        prc = new WebflowRequestContextProfileRequestContextLookup().apply(src);

        function = new AttributeReleaseConsentFunction();
    }

    /**
     * Add a {@link ConsentFlowDescriptor} to the {@link ProfileRequestContext}.
     * 
     * @param compareValues whether consent equality includes comparing consent values
     */
    private void setUpDescriptor(final boolean compareValues) {
        flowDescriptor = new AttributeReleaseFlowDescriptor();
        flowDescriptor.setId("test");
        flowDescriptor.setCompareValues(compareValues);

        final ProfileInterceptorContext pic = new ProfileInterceptorContext();
        pic.setAttemptedFlow(flowDescriptor);
        prc.addSubcontext(pic);
        final ProfileInterceptorContext pic2 = prc.getSubcontext(ProfileInterceptorContext.class);
        assert pic2!= null;
        final ProfileInterceptorFlowDescriptor flow = pic2.getAttemptedFlow();
        assert flow != null;
        Assert.assertTrue(flow  instanceof ConsentFlowDescriptor);

        Assert.assertEquals(((ConsentFlowDescriptor) flow).compareValues(), compareValues);
    }

    @Test public void testNullInput() {
        Assert.assertNull(function.apply(null));
    }

    @Test public void testNullConsentContext() {
        Assert.assertNull(prc.getSubcontext(ConsentContext.class));
        setUpDescriptor(false);

        Assert.assertNull(function.apply(prc));
    }

    @Test public void testNullConsentFlowDescriptor() {
        prc.addSubcontext(new ConsentContext());
        prc.addSubcontext(new ProfileInterceptorContext());
        final ProfileInterceptorContext pic2 = prc.getSubcontext(ProfileInterceptorContext.class);
        assert pic2!= null;
        final ProfileInterceptorFlowDescriptor flow = pic2.getAttemptedFlow();
        assert flow == null;

        Assert.assertNull(function.apply(prc));
    }

    @Test public void testNullAttributeReleaseContext() {
        prc.addSubcontext(new ConsentContext());
        setUpDescriptor(false);
        Assert.assertNull(prc.getSubcontext(AttributeReleaseContext.class));

        Assert.assertNull(function.apply(prc));
    }

    @Test public void testNoConsentableAttributes() {
        prc.addSubcontext(new ConsentContext());
        prc.addSubcontext(new AttributeReleaseContext(), true);
        setUpDescriptor(false);
        final AttributeReleaseContext arc =prc.getSubcontext(AttributeReleaseContext.class); 
        assert arc != null;
        Assert.assertTrue(arc.getConsentableAttributes().isEmpty());
        final Map<String, Consent> res = function.apply(prc);
        assert res != null;
        Assert.assertTrue(res.isEmpty());
    }

    @Test public void testNoPreviousConsents() {
        prc.addSubcontext(new ConsentContext());
        final AttributeReleaseContext arc = new AttributeReleaseContext();
        arc.getConsentableAttributes().putAll(ConsentTestingSupport.newAttributeMap());
        prc.addSubcontext(arc);
        setUpDescriptor(false);
        final AttributeReleaseContext arc2 =prc.getSubcontext(AttributeReleaseContext.class); 
        assert arc2 != null;
        Assert.assertFalse(arc2.getConsentableAttributes().isEmpty());
        ConsentContext consentContext = prc.getSubcontext(ConsentContext.class);
        assert consentContext != null;
        Assert.assertTrue(consentContext.getPreviousConsents().isEmpty());

        final Map<String, Consent> expected = new HashMap<>();
        for (final IdPAttribute attr : ConsentTestingSupport.newAttributeMap().values()) {
            final Consent consent = new Consent();
            consent.setId(attr.getId());
            expected.put(consent.getId(), consent);
        }

        Assert.assertEquals(function.apply(prc), expected);
    }

    @Test public void testNoPreviousConsentsCompareValues() {
        prc.addSubcontext(new ConsentContext());
        final AttributeReleaseContext arc = new AttributeReleaseContext();
        arc.getConsentableAttributes().putAll(ConsentTestingSupport.newAttributeMap());
        prc.addSubcontext(arc);
        setUpDescriptor(true);
        final AttributeReleaseContext arc2 =prc.getSubcontext(AttributeReleaseContext.class); 
        assert arc2 != null;
        Assert.assertFalse(arc2.getConsentableAttributes().isEmpty());
        ConsentContext consentContext = prc.getSubcontext(ConsentContext.class);
        assert consentContext != null;
        Assert.assertTrue(consentContext.getPreviousConsents().isEmpty());

        final Map<String, Consent> expected = new HashMap<>();
        for (final IdPAttribute attr : ConsentTestingSupport.newAttributeMap().values()) {
            final Consent consent = new Consent();
            consent.setId(attr.getId());
            consent.setValue(flowDescriptor.getAttributeValuesHashFunction().apply(attr.getValues()));
            expected.put(consent.getId(), consent);
        }

        Assert.assertEquals(function.apply(prc), expected);
    }

    @Test public void testRememberPreviousConsents() {
        final Consent previousConsent = new Consent();
        previousConsent.setId("attribute1");
        previousConsent.setApproved(true);
        final ConsentContext consentCtx = new ConsentContext();
        consentCtx.getPreviousConsents().put(previousConsent.getId(), previousConsent);
        prc.addSubcontext(consentCtx);

        final AttributeReleaseContext arc = new AttributeReleaseContext();
        arc.getConsentableAttributes().putAll(ConsentTestingSupport.newAttributeMap());
        prc.addSubcontext(arc);
        setUpDescriptor(false);
        final AttributeReleaseContext arc2 =prc.getSubcontext(AttributeReleaseContext.class); 
        assert arc2 != null;
        Assert.assertFalse(arc2.getConsentableAttributes().isEmpty());
        ConsentContext consentContext = prc.getSubcontext(ConsentContext.class);
        assert consentContext != null;
        Assert.assertFalse(consentContext.getPreviousConsents().isEmpty());

        final Map<String, Consent> expected = new HashMap<>();
        for (final IdPAttribute attr : ConsentTestingSupport.newAttributeMap().values()) {
            final Consent consent = new Consent();
            consent.setId(attr.getId());
            if (attr.getId().equals("attribute1")) {
                consent.setApproved(true);
            }
            expected.put(consent.getId(), consent);
        }

        Assert.assertEquals(function.apply(prc), expected);
    }

    @Test public void testRememberPreviousConsentsCompareValues() {
        final Consent previousConsent = new Consent();
        previousConsent.setId("attribute1");
        previousConsent.setValue(flowDescriptor.getAttributeValuesHashFunction().apply(ConsentTestingSupport.newAttributeMap()
                .get("attribute1").getValues()));
        previousConsent.setApproved(true);
        final ConsentContext consentCtx = new ConsentContext();
        consentCtx.getPreviousConsents().put(previousConsent.getId(), previousConsent);
        prc.addSubcontext(consentCtx);

        final AttributeReleaseContext arc = new AttributeReleaseContext();
        arc.getConsentableAttributes().putAll(ConsentTestingSupport.newAttributeMap());
        prc.addSubcontext(arc);
        setUpDescriptor(true);
        final AttributeReleaseContext arc2 =prc.getSubcontext(AttributeReleaseContext.class); 
        assert arc2 != null;
        Assert.assertFalse(arc2.getConsentableAttributes().isEmpty());
        ConsentContext consentContext = prc.getSubcontext(ConsentContext.class);
        assert consentContext != null;
        Assert.assertFalse(consentContext.getPreviousConsents().isEmpty());

        final Map<String, Consent> expected = new HashMap<>();
        for (final IdPAttribute attr : ConsentTestingSupport.newAttributeMap().values()) {
            final Consent consent = new Consent();
            consent.setId(attr.getId());
            consent.setValue(flowDescriptor.getAttributeValuesHashFunction().apply(attr.getValues()));
            if (attr.getId().equals("attribute1")) {
                consent.setApproved(true);
            }
            expected.put(consent.getId(), consent);
        }

        Assert.assertEquals(function.apply(prc), expected);
    }
    
    @Test public void testSorting () throws Exception {
        // Setup unsorted previous
        final Consent previousConsent = new Consent();
        previousConsent.setId("attribute1");
        previousConsent.setValue(flowDescriptor.getAttributeValuesHashFunction().apply(ConsentTestingSupport.newAttributeMap(MapType.ORDER1)
                .get("attribute1").getValues()));
        previousConsent.setApproved(true);
        ConsentContext consentCtx = new ConsentContext();
        consentCtx.getPreviousConsents().put(previousConsent.getId(), previousConsent);
        prc.addSubcontext(consentCtx);
        
        // Test that we accept it
        AttributeReleaseContext arc = new AttributeReleaseContext();
        arc.getConsentableAttributes().putAll(ConsentTestingSupport.newAttributeMap(MapType.ORDER1));
        prc.addSubcontext(arc);
        setUpDescriptor(true);

        final Map<String, Consent> firstResult = function.apply(prc);
        assert firstResult != null;
        final Consent firstConsent = firstResult.get("attribute1");
        assertTrue(firstConsent.isApproved());

        // Now test  in a different order
        setUp();
        consentCtx = new ConsentContext();
        // first consent is now the previous consent
        consentCtx.getPreviousConsents().put(firstConsent.getId(), firstConsent);
        prc.addSubcontext(consentCtx);
        arc = new AttributeReleaseContext();
        arc.getConsentableAttributes().putAll(ConsentTestingSupport.newAttributeMap(MapType.ORDER2));
        prc.addSubcontext(arc);
        setUpDescriptor(true);

        final Map<String, Consent> secondResult = function.apply(prc);
        assert secondResult != null;
        final Consent secondConsent = secondResult.get("attribute1");
        assertTrue(secondConsent.isApproved());
    }
    

    @Test public void testRememberPreviousConsentsDifferentValueCompareValues() {
        final Consent previousConsent = new Consent();
        previousConsent.setId("attribute1");
        previousConsent.setValue("differentValue");
        previousConsent.setApproved(true);
        final ConsentContext consentCtx = new ConsentContext();
        consentCtx.getPreviousConsents().put(previousConsent.getId(), previousConsent);
        prc.addSubcontext(consentCtx);

        final AttributeReleaseContext arc = new AttributeReleaseContext();
        arc.getConsentableAttributes().putAll(ConsentTestingSupport.newAttributeMap());
        prc.addSubcontext(arc);
        setUpDescriptor(true);
        final AttributeReleaseContext arc2 =prc.getSubcontext(AttributeReleaseContext.class); 
        assert arc2 != null;
        Assert.assertFalse(arc2.getConsentableAttributes().isEmpty());
        ConsentContext consentContext = prc.getSubcontext(ConsentContext.class);
        assert consentContext != null;
        Assert.assertFalse(consentContext.getPreviousConsents().isEmpty());

        final Map<String, Consent> expected = new HashMap<>();
        for (final IdPAttribute attr : ConsentTestingSupport.newAttributeMap().values()) {
            final Consent consent = new Consent();
            consent.setId(attr.getId());
            consent.setValue(flowDescriptor.getAttributeValuesHashFunction().apply(attr.getValues()));
            expected.put(consent.getId(), consent);
        }

        Assert.assertEquals(function.apply(prc), expected);
    }

}
