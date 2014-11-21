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

package net.shibboleth.idp.consent.logic;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import net.shibboleth.idp.consent.Consent;
import net.shibboleth.idp.consent.flow.ConsentFlowDescriptor;
import net.shibboleth.idp.profile.RequestContextBuilder;
import net.shibboleth.idp.profile.context.ProfileInterceptorContext;
import net.shibboleth.idp.profile.context.navigate.WebflowRequestContextProfileRequestContextLookup;
import net.shibboleth.utilities.java.support.component.ComponentInitializationException;
import net.shibboleth.utilities.java.support.component.UnmodifiableComponentException;
import net.shibboleth.utilities.java.support.logic.ConstraintViolationException;

import org.opensaml.profile.context.ProfileRequestContext;
import org.springframework.context.MessageSource;
import org.springframework.context.MessageSourceResolvable;
import org.springframework.context.NoSuchMessageException;
import org.springframework.webflow.execution.RequestContext;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

/** {@link MessageSourceConsentFunction} unit test. */
public class MessageSourceConsentFunctionTest {

    private RequestContext src;

    private ProfileRequestContext prc;

    private MessageSource messageSource;

    private HashFunction hashFunction;

    private MessageSourceConsentFunction function;

    @BeforeMethod public void setUp() throws Exception {
        src = new RequestContextBuilder().buildRequestContext();
        prc = new WebflowRequestContextProfileRequestContextLookup().apply(src);

        messageSource = new MockMessageSource();

        hashFunction = new HashFunction();

        function = new MessageSourceConsentFunction();
        function.setMessageSource(messageSource);
    }

    /**
     * Add a {@link ConsentFlowDescriptor} to the {@link ProfileRequestContext}.
     * 
     * @param compareValues whether consent equality includes comparing consent values
     */
    private void setUpDescriptor(final boolean compareValues) {
        final ConsentFlowDescriptor descriptor = new ConsentFlowDescriptor();
        descriptor.setId("test");
        descriptor.setCompareValues(compareValues);

        final ProfileInterceptorContext pic = new ProfileInterceptorContext();
        pic.setAttemptedFlow(descriptor);
        prc.addSubcontext(pic);

        Assert.assertNotNull(prc.getSubcontext(ProfileInterceptorContext.class));
        Assert.assertNotNull(prc.getSubcontext(ProfileInterceptorContext.class).getAttemptedFlow());
        Assert.assertTrue(prc.getSubcontext(ProfileInterceptorContext.class).getAttemptedFlow() instanceof ConsentFlowDescriptor);

        Assert.assertEquals(((ConsentFlowDescriptor) prc.getSubcontext(ProfileInterceptorContext.class)
                .getAttemptedFlow()).compareValues(), compareValues);
    }

    @Test public void testNullInput() {
        Assert.assertNull(function.apply(null));
    }

    @Test(expectedExceptions = ComponentInitializationException.class) public void testMissingIdMessageCode()
            throws Exception {
        function.setConsentValueMessageCode("consentValueMessageCode");
        function.initialize();
    }

    @Test(expectedExceptions = ComponentInitializationException.class) public void testMissingValueMessageCode()
            throws Exception {
        function.setConsentIdMessageCode("consentIdMessageCode");
        function.initialize();
    }

    @Test(expectedExceptions = ConstraintViolationException.class) public void testNullIdMessageCode() throws Exception {
        function.setConsentIdMessageCode(null);
        function.initialize();
    }

    @Test(expectedExceptions = ConstraintViolationException.class) public void testNullValueMessageCode()
            throws Exception {
        function.setConsentValueMessageCode(null);
        function.initialize();
    }

    @Test(expectedExceptions = ConstraintViolationException.class) public void testEmptyIdMessageCode()
            throws Exception {
        function.setConsentIdMessageCode("");
        function.initialize();
    }

    @Test(expectedExceptions = ConstraintViolationException.class) public void testEmptyValueMessageCode()
            throws Exception {
        function.setConsentValueMessageCode("");
        function.initialize();
    }

    @Test(expectedExceptions = UnmodifiableComponentException.class) public void testInstantiationIdMessageCode()
            throws Exception {
        function.setConsentIdMessageCode("consentIdMessageCode");
        function.setConsentValueMessageCode("consentValueMessageCode");
        function.initialize();

        function.setConsentIdMessageCode("consentIdMessageCode");
    }

    @Test(expectedExceptions = UnmodifiableComponentException.class) public void testInstantiationValueMessageCode()
            throws Exception {
        function.setConsentIdMessageCode("consentIdMessageCode");
        function.setConsentValueMessageCode("consentValueMessageCode");
        function.initialize();

        function.setConsentValueMessageCode("consentValueMessageCode");
    }

    @Test public void testMessageSourceConsent() throws Exception {

        setUpDescriptor(false);

        final Consent consent = new Consent();
        consent.setId("consentIdMessage");

        final Map<String, Consent> expected = new HashMap<>();
        expected.put(consent.getId(), consent);

        function.setConsentIdMessageCode("consentIdMessageCode");
        function.setConsentValueMessageCode("consentValueMessageCode");
        function.initialize();

        Assert.assertEquals(function.apply(prc), expected);
    }

    @Test public void testMessageSourceConsentCompareValues() throws Exception {

        setUpDescriptor(true);

        final Consent consent = new Consent();
        consent.setId("consentIdMessage");
        consent.setValue(hashFunction.apply("consentValueMessage"));

        final Map<String, Consent> expected = new HashMap<>();
        expected.put(consent.getId(), consent);

        function.setConsentIdMessageCode("consentIdMessageCode");
        function.setConsentValueMessageCode("consentValueMessageCode");
        function.initialize();

        Assert.assertEquals(function.apply(prc), expected);
    }

    private class MockMessageSource implements MessageSource {

        /** {@inheritDoc} */
        public String getMessage(String code, Object[] args, String defaultMessage, Locale locale) {
            if (code.equals("consentIdMessageCode")) {
                return "consentIdMessage";
            } else if (code.equals("consentValueMessageCode")) {
                return "consentValueMessage";
            } else {
                return defaultMessage;
            }
        }

        /** {@inheritDoc} */
        public String getMessage(String code, Object[] args, Locale locale) throws NoSuchMessageException {
            if (code.equals("consentIdMessageCode")) {
                return "consentIdMessage";
            } else if (code.equals("consentValueMessageCode")) {
                return "consentValueMessage";
            }
            throw new NoSuchMessageException("No such message");
        }

        /** {@inheritDoc} */
        public String getMessage(MessageSourceResolvable resolvable, Locale locale) throws NoSuchMessageException {
            if (resolvable.getCodes()[0].equals("consentIdMessageCode")) {
                return "consentIdMessage";
            } else if (resolvable.getCodes()[0].equals("consentValueMessageCode")) {
                return "consentValueMessage";
            }
            throw new NoSuchMessageException("No such message");
        }

    }
}
