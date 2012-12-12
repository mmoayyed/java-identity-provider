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

import javax.servlet.http.HttpServletRequest;

import net.shibboleth.idp.profile.ActionTestingSupport;
import net.shibboleth.idp.profile.HttpServletRequestMessageDecoderFactory;
import net.shibboleth.idp.profile.ProfileRequestContext;
import net.shibboleth.utilities.java.support.component.ComponentInitializationException;

import org.opensaml.core.xml.XMLObjectBaseTestCase;
import org.opensaml.messaging.context.MessageContext;
import org.opensaml.messaging.decoder.MessageDecoder;
import org.opensaml.messaging.decoder.MessageDecodingException;
import org.opensaml.messaging.decoder.servlet.BaseHttpServletRequestXmlMessageDecoder;
import org.opensaml.messaging.decoder.servlet.HttpServletRequestMessageDecoder;
import org.opensaml.saml.common.SAMLObject;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.webflow.execution.Event;
import org.testng.Assert;
import org.testng.annotations.Test;

/** Unit test for {@link DecodeMessage}. */
public class DecodeMessageTest extends XMLObjectBaseTestCase {

    /** Test that the action proceeds properly if the message can be decoded. */
    @Test public void testDecodeMessage() throws Exception {

        ProfileRequestContext profileCtx = new ProfileRequestContext();

        MockHttpServletRequestMessageDecoderFactory decoderFactory = new MockHttpServletRequestMessageDecoderFactory();

        DecodeMessage action = new DecodeMessage(decoderFactory);
        action.setId("test");
        action.initialize();

        Event result = action.doExecute(new MockHttpServletRequest(), null, profileCtx);

        ActionTestingSupport.assertProceedEvent(result);

        Assert.assertNotNull(profileCtx.getInboundMessageContext());
        Assert.assertEquals(profileCtx.getInboundMessageContext().getClass(), MessageContext.class);
    }

    /** Test that the action errors out properly if the message can not be decoded. */
    @Test public void testThrowException() throws Exception {

        ProfileRequestContext profileCtx = new ProfileRequestContext();

        MockHttpServletRequestMessageDecoderFactory decoderFactory = new MockHttpServletRequestMessageDecoderFactory();
        decoderFactory.setThrowExecption(true);

        DecodeMessage action = new DecodeMessage(decoderFactory);
        action.setId("test");
        action.initialize();

        Event result = action.doExecute(null, null, profileCtx);

        ActionTestingSupport.assertEvent(result, DecodeMessage.UNABLE_TO_DECODE);
    }

    /**
     * Mock implementation of {@link HttpServletRequestMessageDecoderFactory} producing a {@link MessageDecoder} which
     * either returns an empty {@link MessageContext} or throws a {@link MessageDecodingException}.
     */
    class MockHttpServletRequestMessageDecoderFactory implements HttpServletRequestMessageDecoderFactory {

        /** Whether a {@link MessageDecodingException} should be thrown by the {@link MessageDecoder}. */
        private boolean throwException = false;

        /**
         * Sets whether a {@link MessageDecodingException} should be thrown by the {@link MessageDecoder}.
         * 
         * @param shouldThrowDecodeException true if an exception should be thrown, false if not
         */
        public void setThrowExecption(final boolean shouldThrowDecodeException) {
            throwException = shouldThrowDecodeException;
        }

        /** {@inheritDoc} */
        public MessageDecoder newDecoder(HttpServletRequest httpRequest) throws MessageDecodingException {
            MockHttpServletRequestXmlMessageDecoder decoder = new MockHttpServletRequestXmlMessageDecoder();
            decoder.setThrowExecption(throwException);
            decoder.setHttpServletRequest(httpRequest);
            try {
                decoder.initialize();
            } catch (ComponentInitializationException e) {
                throw new MessageDecodingException(e);
            }
            return decoder;
        }
    }

    /**
     * Mock implementation of {@link HttpServletRequestMessageDecoder} which either returns an empty
     * {@link MessageContext} or throws a {@link MessageDecodingException}.
     */
    class MockHttpServletRequestXmlMessageDecoder extends BaseHttpServletRequestXmlMessageDecoder {

        /** Whether a {@link MessageDecodingException} should be thrown by {@link #doDecode()}. */
        private boolean throwException = false;

        /**
         * Sets whether a {@link MessageDecodingException} should be thrown by {@link #doDecode()}.
         * 
         * @param shouldThrowDecodeException true if an exception should be thrown, false if not
         */
        public void setThrowExecption(final boolean shouldThrowDecodeException) {
            throwException = shouldThrowDecodeException;
        }

        /** {@inheritDoc} */
        protected void doDecode() throws MessageDecodingException {
            if (throwException) {
                throw new MessageDecodingException();
            } else {
                setMessageContext(new MessageContext<SAMLObject>());
            }
        }
    }
}
