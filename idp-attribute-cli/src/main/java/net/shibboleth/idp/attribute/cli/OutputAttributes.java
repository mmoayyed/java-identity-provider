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

package net.shibboleth.idp.attribute.cli;

import java.io.IOException;
import java.io.Writer;
import java.util.Locale;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import net.shibboleth.ext.spring.webflow.Event;
import net.shibboleth.ext.spring.webflow.Events;
import net.shibboleth.idp.attribute.Attribute;
import net.shibboleth.idp.attribute.AttributeContext;
import net.shibboleth.idp.attribute.AttributeValue;
import net.shibboleth.idp.profile.AbstractProfileAction;
import net.shibboleth.idp.profile.ActionSupport;
import net.shibboleth.idp.profile.EventIds;
import net.shibboleth.idp.profile.ProfileException;

import org.opensaml.messaging.profile.ProfileRequestContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.webflow.execution.RequestContext;


/**
 * A Spring-aware action to write the attribute context information to the external output sink.
 * This is for use in command line scenaros where there is no servlet environment for output.
 */
@Events({
    @Event(id = EventIds.PROCEED_EVENT_ID),
    @Event(id = EventIds.INVALID_ATTRIBUTE_CTX, description = "No attributes were available for filtering"),
    })
public final class OutputAttributes extends AbstractProfileAction {

    /** Class logger. */
    private final Logger log = LoggerFactory.getLogger(OutputAttributes.class);
    
    /** Constructor. */
    public OutputAttributes() {
        super();
    }

    /** {@inheritDoc} */
    protected org.springframework.webflow.execution.Event doExecute(@Nullable final HttpServletRequest httpRequest,
            @Nullable final HttpServletResponse httpResponse, @Nonnull final RequestContext springRequestContext,
            @Nonnull final ProfileRequestContext profileRequestContext)
            throws ProfileException {
        
        AttributeContext attributeContext = profileRequestContext.getSubcontext(AttributeContext.class, false);
        if (attributeContext == null) {
            log.debug("Action {}: No attribute context, no attributes to filter", getId());
            return ActionSupport.buildEvent(this, EventIds.INVALID_ATTRIBUTE_CTX);
        }
        
        try {
            Writer writer = springRequestContext.getExternalContext().getResponseWriter();
            for (Attribute a : attributeContext.getAttributes().values()) {
                writer.write("Attribute: " + a.getId() + "\n");
                if (a.getDisplayNames().containsKey(Locale.getDefault())) {
                    writer.write("\tDisplay name: " + a.getDisplayNames().get(Locale.getDefault()) + "\n");
                }
                
                if (!a.getValues().isEmpty()) {
                    writer.write("\tValues:\n");
                    for (AttributeValue av : a.getValues()) {
                        writer.write("\t\t" + av.getValue().toString() + "\n");
                    }
                } else {
                    writer.write("\t(no values)\n");
                }
            }
        } catch (IOException e) {
            throw new ProfileException("I/O error writing attributes to output context", e);
        }
        
        return ActionSupport.buildProceedEvent(this);
    }
}