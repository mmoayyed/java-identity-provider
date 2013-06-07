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

import java.util.UUID;

//TODO need to deal with relay state

import net.shibboleth.idp.saml.impl.profile.BaseIdpInitiatedSsoRequestMessageDecoder;

import org.opensaml.messaging.context.BasicMessageMetadataContext;
import org.opensaml.messaging.context.MessageContext;
import org.opensaml.messaging.decoder.MessageDecodingException;

/** Decodes an incoming Shibboleth Authentication Request message. */
class IdpInitiatedSsoRequestMessageDecoder extends BaseIdpInitiatedSsoRequestMessageDecoder<IdpInitatedSsoRequest> {

    /** {@inheritDoc} */
    protected void doDecode() throws MessageDecodingException {
        IdpInitatedSsoRequest authnRequest =
                new IdpInitatedSsoRequest(getEntityId(getHttpServletRequest()), getAcsUrl(getHttpServletRequest()),
                        getTarget(getHttpServletRequest()), getTime(getHttpServletRequest()));

        MessageContext<IdpInitatedSsoRequest> messageContext = new MessageContext<IdpInitatedSsoRequest>();
        messageContext.setMessage(authnRequest);

        //TODO fix
        BasicMessageMetadataContext msgMetadata = new BasicMessageMetadataContext();
        msgMetadata.setMessageId(UUID.randomUUID().toString());
        msgMetadata.setMessageIssueInstant(authnRequest.getTime());
        msgMetadata.setMessageIssuer(authnRequest.getEntityId());

        messageContext.addSubcontext(msgMetadata);
        
        //TODO binding context

        setMessageContext(messageContext);
    }
}