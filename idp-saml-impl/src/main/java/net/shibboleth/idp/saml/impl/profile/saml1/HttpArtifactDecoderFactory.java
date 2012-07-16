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

package net.shibboleth.idp.saml.impl.profile.saml1;

import javax.annotation.Nonnull;
import javax.annotation.concurrent.ThreadSafe;
import javax.servlet.http.HttpServletRequest;

import org.opensaml.messaging.decoder.MessageDecoder;
import org.opensaml.messaging.decoder.MessageDecodingException;
import org.opensaml.saml.saml1.binding.decoding.HTTPArtifactDecoder;

import net.shibboleth.idp.profile.HttpServletRequestMessageDecoderFactory;
import net.shibboleth.utilities.java.support.logic.Constraint;
import net.shibboleth.utilities.java.support.xml.ParserPool;

/** Factory used to produce {@link HTTPArtifactDecoder} message decoders. */
@ThreadSafe
public class HttpArtifactDecoderFactory implements HttpServletRequestMessageDecoderFactory {

    /** Pool of XML parsers used to parse incoming messages. */
    private final ParserPool parserPool;

    /**
     * Constructor.
     * 
     * @param pool pool of parsers used to parse incoming XML
     */
    public HttpArtifactDecoderFactory(@Nonnull final ParserPool pool) {
        parserPool = Constraint.isNotNull(pool, "Parser pool can not be null");
    }

    /** {@inheritDoc} */
    public MessageDecoder newDecoder(@Nonnull final HttpServletRequest httpRequest) throws MessageDecodingException {
        HTTPArtifactDecoder decoder = new HTTPArtifactDecoder();
        decoder.setHttpServletRequest(httpRequest);
        decoder.setParserPool(parserPool);

        return decoder;
    }
}