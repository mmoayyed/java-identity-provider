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

package net.shibboleth.idp.profile;

import javax.servlet.http.HttpServletRequest;

import org.opensaml.messaging.decoder.MessageDecoder;
import org.opensaml.messaging.decoder.MessageDecodingException;

/**
 * A factory that produces a {@link MessageDecoder} appropriate for a given {@link HttpServletRequest}.
 * 
 * @param <MessageType> the type of message produced by the decoder
 */
public interface HttpServletRequestMessageDecoderFactory<MessageType> {

    /**
     * Generates a new message decoder based on the given HTTP request.
     * 
     * @param httpRequest the HTTP request, never null
     * 
     * @return the generated decoder, never null
     * 
     * @throws MessageDecodingException thrown if there is a problem generating a new message decoder for the given HTTP
     *             request
     */
    public MessageDecoder<MessageType> newDecoder(HttpServletRequest httpRequest) throws MessageDecodingException;
}