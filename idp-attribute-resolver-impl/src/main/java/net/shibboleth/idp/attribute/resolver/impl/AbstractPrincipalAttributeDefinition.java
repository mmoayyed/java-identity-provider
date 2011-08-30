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

import net.jcip.annotations.ThreadSafe;
import net.shibboleth.idp.attribute.resolver.AttributeResolutionContext;
import net.shibboleth.idp.attribute.resolver.AttributeResolutionException;
import net.shibboleth.idp.attribute.resolver.BaseAttributeDefinition;
import net.shibboleth.idp.session.AuthenticationEvent;
import net.shibboleth.idp.session.IdPSession;
import net.shibboleth.idp.session.IdPSessionSubcontext;
import net.shibboleth.idp.session.ServiceSession;

import org.opensaml.messaging.context.BasicMessageMetadataSubcontext;
import org.opensaml.messaging.context.InOutOperationContext;
import org.opensaml.messaging.context.MessageContext;
import org.opensaml.messaging.context.SubcontextContainer;
import org.opensaml.util.StringSupport;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This attribution definition has strong requirements on the {@link AttributeResolutionContext} that is passed in.
 * Specifically the owner {@link SubcontextContainer} <em>must:</em>
 * <ul>
 * <li>Contain a {@link IdPSession}.
 * <li>Implement {@link InOutOperationContext} (usually it will be a BasicInOutOperationContext, but in tests it may
 * not).</li>
 * <li>Have associated with this, a valid {@link MessageContext} at getInboundMessageContext which in
 * turn <em>must</em>:
 * <ul>
 * <li>Contain a {@link BasicMessageMetadataSubcontext} to provide the relying party name.</li>
 * </ul>
 * </ul>
 * 
 * */
@ThreadSafe
public abstract class AbstractPrincipalAttributeDefinition extends BaseAttributeDefinition {

    /** Class logger. */
    private final Logger log = LoggerFactory.getLogger(AbstractPrincipalAttributeDefinition.class);

    /**
     * Traverse the contexts as described above to get hold of the relying party (from the
     * BasicMessageMetadataSubcontext). <bl />Emit errors and exceptions as we go.
     * 
     * @param parent the parent of the attribute request.
     * @return the principal name, or null
     * @throws AttributeResolutionException if we fail to traverse the tree.
     */
    private String getRelyingParty(SubcontextContainer parent) throws AttributeResolutionException {
        if (!(parent instanceof InOutOperationContext)) {
            String errMsg =
                    "Principal Attribute Definition " + getId()
                            + ": Container does not implement InOutOperationContext";
            log.error(errMsg);
            throw new AttributeResolutionException(errMsg);
        }

        final InOutOperationContext<?, ?> parentIoOperationContext = (InOutOperationContext) parent;
        final MessageContext<?> inboundContext = parentIoOperationContext.getInboundMessageContext();

        if (null == inboundContext) {
            String errMsg = "Principal Attribute Definition " + getId() + ": No inbound context present";
            log.error(errMsg);
            throw new AttributeResolutionException(errMsg);
        }

        final BasicMessageMetadataSubcontext messageMetadata =
                inboundContext.getSubcontext(BasicMessageMetadataSubcontext.class, false);
        if (null == messageMetadata) {
            String errMsg = "Principal Attribute Definition " + getId() + ": No Message Metadata context present";
            log.error(errMsg);
            throw new AttributeResolutionException(errMsg);
        }
        final String relyingParty = StringSupport.trimOrNull(messageMetadata.getMessageIssuer());
        if (null == relyingParty) {
            String errMsg = "Principal Attribute Definition " + getId() + ": RelyingParty is null";
            log.error(errMsg);
            throw new AttributeResolutionException(errMsg);
        }
        return relyingParty;
    }

    /**
     * Traverse the context tree as defined in the class description to find the relevant authentication event.
     * 
     * @param resolutionContext the context that we are starting with.
     * @return the authentication event.
     * @throws AttributeResolutionException if any of the invariants described above fail.
     */
    protected AuthenticationEvent getAuthenticationEvent(final AttributeResolutionContext resolutionContext)
            throws AttributeResolutionException {

        final SubcontextContainer parent = resolutionContext.getOwner();
        final String relyingParty = getRelyingParty(parent);

        if (!parent.containsSubcontext(IdPSessionSubcontext.class)) {
            String msg =
                    "Principal Attribute (Name or AuthN method) Definition " + getId()
                            + ": Could not locate IdPSessionSubcontext";
            log.error(msg);
            throw new AttributeResolutionException(msg);
        }

        final IdPSession idpSession = parent.getSubcontext(IdPSessionSubcontext.class).getIdPSession();
        final ServiceSession serviceSession = idpSession.getServiceSession(relyingParty);
        if (null == serviceSession) {
            String msg =
                    "Principal Attribute Definition (Name or AuthN method) " + getId()
                            + ": Could not locate service session for " + relyingParty;
            log.error(msg);
            throw new AttributeResolutionException(msg);
        }

        return serviceSession.getAuthenticationEvent();
    }
}
