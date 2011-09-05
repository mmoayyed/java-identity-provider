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

package net.shibboleth.idp.attribute.filtering.impl;

import net.jcip.annotations.ThreadSafe;
import net.shibboleth.idp.attribute.filtering.AttributeFilterContext;
import net.shibboleth.idp.session.AuthenticationEvent;
import net.shibboleth.idp.session.IdPSession;
import net.shibboleth.idp.session.IdPSessionSubcontext;
import net.shibboleth.idp.session.ServiceSession;

import org.opensaml.messaging.context.BasicMessageMetadataSubcontext;
import org.opensaml.messaging.context.InOutOperationContext;
import org.opensaml.messaging.context.MessageContext;
import org.opensaml.messaging.context.SubcontextContainer;
import org.opensaml.util.StringSupport;
import org.opensaml.util.criteria.EvaluationException;
import org.slf4j.LoggerFactory;

/**
 * This class supplies some static helper functions to navigate the contexts to get to information required by the
 * Matchers and Criteria. Just as for
 * {@link net.shibboleth.idp.attribute.resolver.impl.AbstractPrincipalAttributeDefinition} we have requirements on the
 * the {@link AttributeFilterContext} that is passed in. Specifically the owner {@link SubcontextContainer}
 * <em>must:</em>
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
 */
@ThreadSafe
public final class ContextNavigationSupport {

    /** Constructor. We never instantiate this */
    private ContextNavigationSupport() {
    }

    /**
     * Navigate through the Contexts as described above and grab the RP from the incoming message.
     * 
     * @param filterContext The filter context to start at.
     * @return the relying party, never null or empty.
     * @throws EvaluationException if we get an error in traversal.
     */
    public static String getIncomingIssuer(final AttributeFilterContext filterContext) throws EvaluationException {
        return getIssuer(filterContext, true);
    }

    /**
     * Navigate through the Contexts as described above and grab the RP from the outgoing message.
     * 
     * @param filterContext The filter context to start at.
     * @return the relying party, never null or empty.
     * @throws EvaluationException if we get an error in traversal.
     */
    public static String getOutgoingIssuer(final AttributeFilterContext filterContext) throws EvaluationException {
        return getIssuer(filterContext, false);
    }

    /**
     * Navigate through the Contexts as described above and grab the RP from the incoming message.
     * 
     * @param filterContext The filter context to start at.
     * @param useIncoming Whether to use the incoming or outgoing context.
     * @return the relying party, never null or empty.
     * @throws EvaluationException if we get an error in traversal.
     */
    private static String getIssuer(final AttributeFilterContext filterContext, final boolean useIncoming)
            throws EvaluationException {
        final SubcontextContainer parent = filterContext.getOwner();
        if (!(parent instanceof InOutOperationContext)) {
            LoggerFactory.getLogger(ContextNavigationSupport.class).error(
                    "Attribute Filter: Container does not implement InOutOperationContext.");
            throw new EvaluationException("Attribute Filter: Container does not implement InOutOperationContext.");
        }

        final InOutOperationContext<?, ?> parentIoOperationContext = (InOutOperationContext) parent;
        final MessageContext<?> context;

        if (useIncoming) {
            context = parentIoOperationContext.getInboundMessageContext();

            if (null == context) {
                LoggerFactory.getLogger(ContextNavigationSupport.class).error(
                        "Attribute Filter: No inbound context present.");
                throw new EvaluationException("Attribute Filter: No inbound context present.");
            }
        } else {
            context = parentIoOperationContext.getOutboundMessageContext();

            if (null == context) {
                LoggerFactory.getLogger(ContextNavigationSupport.class).error(
                        "Attribute Filter: No outbound context present.");
                throw new EvaluationException("Attribute Filter: No inbound context present.");
            }
        }

        final BasicMessageMetadataSubcontext messageMetadata =
                context.getSubcontext(BasicMessageMetadataSubcontext.class, false);

        if (null == messageMetadata) {
            LoggerFactory.getLogger(ContextNavigationSupport.class).error(
                    "Attribute Filter:  No Message Metadata context present.");
            throw new EvaluationException("Attribute Filter: No inbound context present.");
        }
        final String retVal = StringSupport.trimOrNull(messageMetadata.getMessageIssuer());
        if (null == retVal) {
            LoggerFactory.getLogger(ContextNavigationSupport.class).error("Attribute Filter:  entityId was empty.");
            throw new EvaluationException("Attribute Filter: entityId was empty.");
        }
        return retVal;
    }

    /**
     * Navigate through the Contexts as described above and grab the authentication event.
     * 
     * @param filterContext The filter context to start at.
     * @param relyingParty The relying party used to look up the precise event.
     * @return the event, never null.
     * @throws EvaluationException if we get an error in traversal.
     */
    public static AuthenticationEvent getAuthenticationEvent(final AttributeFilterContext filterContext,
            final String relyingParty) throws EvaluationException {

        final SubcontextContainer parent = filterContext.getOwner();

        if (null == relyingParty) {
            LoggerFactory.getLogger(ContextNavigationSupport.class).error("Attribute Filter : RelyingParty is null.");
            throw new EvaluationException("Attribute Filter : RelyingParty is null.");
        }

        if (!parent.containsSubcontext(IdPSessionSubcontext.class)) {
            LoggerFactory.getLogger(ContextNavigationSupport.class).error(
                    "Attribute Filter : Could not locate IdPSessionSubcontext.");
            throw new EvaluationException("Attribute Filter : Could not locate IdPSessionSubcontext.");
        }

        final IdPSession idpSession = parent.getSubcontext(IdPSessionSubcontext.class).getIdPSession();
        final ServiceSession serviceSession = idpSession.getServiceSession(relyingParty);
        if (null == serviceSession) {
            String errMsg = "Attribute Filter : Could not locate service session for " + relyingParty + ".";
            LoggerFactory.getLogger(ContextNavigationSupport.class).error(errMsg);
            throw new EvaluationException(errMsg);
        }

        final AuthenticationEvent event = serviceSession.getAuthenticationEvent();
        if (null == event) {
            String errMsg = "Attribute Filter : No valid authentication even for " + relyingParty + ".";
            LoggerFactory.getLogger(ContextNavigationSupport.class).error(errMsg);
            throw new EvaluationException(errMsg);
        }

        return event;
    }
}
