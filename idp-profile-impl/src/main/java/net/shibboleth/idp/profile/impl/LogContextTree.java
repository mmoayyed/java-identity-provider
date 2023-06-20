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

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.opensaml.messaging.context.BaseContext;
import org.opensaml.messaging.context.MessageContext;
import org.opensaml.profile.context.ProfileRequestContext;
import org.slf4j.Logger;
import org.springframework.webflow.execution.RequestContext;

import net.shibboleth.idp.profile.AbstractProfileAction;
import net.shibboleth.idp.profile.context.SpringRequestContext;
import net.shibboleth.shared.annotation.constraint.NotEmpty;
import net.shibboleth.shared.primitive.LoggerFactory;
import net.shibboleth.shared.primitive.StringSupport;

//Checkstyle: JavadocStyle OFF -- ignore extra HTML tag error
/**
 * Spring Web Flow utility action for logging on DEBUG a representation of the current
 * {@link ProfileRequestContext}.
 * 
 * <p>
 * You can contextualize the logging of the context tree either by setting {@link #setDescription(String)},
 * or more usefully by using an attribute on the specific action expression as below.  This allows using
 * just one declaration of the action bean, but parameterized differently depending on where it is placed.
 * </p>
 * 
 * <pre>
 * {@code
 * <evaluate expression="LogContextTree">
 *    <attribute name="contextTreeDescription" value="My Description" />
 * </evaluate>
 *  }
 * </pre>
 */
//Checkstyle: JavadocStyle ON
public class LogContextTree extends AbstractProfileAction {
    
    /** Name of Spring web flow attribute holding the description of the tree to log. */
    @Nonnull @NotEmpty public static final String ATTRIB_DESC = "contextTreeDescription";
    
    /** Logger. */
    @Nonnull private Logger log = LoggerFactory.getLogger("CONTEXT_TREE");
    
    /** Contextual description to output at the start of the action. */
    @Nullable private String description;
    
    /**
     * Set the contextual description to output at the start of the action.
     * 
     * @param value the description value
     */
    public void setDescription(@Nullable final String value) {
        description = StringSupport.trimOrNull(value);
    }

    /** {@inheritDoc} */
    @Override
    protected void doExecute(@Nonnull final ProfileRequestContext profileRequestContext) {
        if (!log.isDebugEnabled()) {
            // short-circuit if not logging at debug
            return;
        }
        
        String contextualDescription = null;
        
        final SpringRequestContext springRequestContext =
                profileRequestContext.getSubcontext(SpringRequestContext.class);
        if (springRequestContext != null && springRequestContext.getRequestContext() != null) {
            final RequestContext requestContext = springRequestContext.getRequestContext();
            assert requestContext != null;
            contextualDescription = requestContext.getAttributes().getString(ATTRIB_DESC);
        }
        
        if (contextualDescription == null) {
            contextualDescription = description;
        }
        
        if (contextualDescription != null) {
            log.debug("Context tree contextual description: {}", contextualDescription) ;
        }
        
        logContext(profileRequestContext, 0);
    }

    /**
     * Recursively log the context tree.
     * 
     * @param current the current context to log
     * @param indent the amount of leading indent
     */
    private void logContext(@Nullable final BaseContext current, final int indent) {
        if (current == null) {
            return;
        }
        
        final String indentString = getIndent(indent);
        
        if (current instanceof ProfileRequestContext) {
            final ProfileRequestContext prc = (ProfileRequestContext) current;
            
            log.debug("{} PRC: {}", indentString, prc.getClass().getName());
            for (final BaseContext subcontext : prc) {
                logContext(subcontext, indent+1);
            }
            
            final MessageContext inbound = prc.getInboundMessageContext();
            if (inbound != null) {
                log.debug("{} PRC InboundMessageContext: {}", indentString, inbound.getClass().getName());
                for (final BaseContext subcontext : inbound) {
                    logContext(subcontext, indent+1);
                }
            } else {
                log.debug("{} PRC InboundMessageContext not present", indentString);
            }
            
            final MessageContext outbound = prc.getOutboundMessageContext();
            if (outbound != null) {
                log.debug("{} PRC OutboundMessageContext: {}", indentString, outbound.getClass().getName());
                for (final BaseContext subcontext : outbound) {
                    logContext(subcontext, indent+1);
                }
            } else {
                log.debug("{} PRC OutboundMessageContext not present", indentString);
            }
            
        } else {
            log.debug("{} {}", indentString, current.getClass().getName());
            for (final BaseContext subcontext : current) {
                logContext(subcontext, indent+1);
            }
        }
        
    }
    
    /**
     * Generate the leading indent string to print.
     * 
     * @param indent the amount of the indent
     * 
     * @return the leading indent string to print
     */
    @Nonnull private String getIndent(final int indent) {
        final StringBuffer buffer = new StringBuffer();
        for (int i=0; i<indent; i++) {
            buffer.append("----");
        }
        return buffer.toString();
    }

}