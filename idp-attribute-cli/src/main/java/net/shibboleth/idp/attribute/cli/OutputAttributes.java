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

import net.shibboleth.idp.attribute.IdPAttribute;
import net.shibboleth.idp.attribute.IdPAttributeValue;
import net.shibboleth.idp.attribute.context.AttributeContext;
import net.shibboleth.idp.profile.AbstractProfileAction;
import net.shibboleth.idp.profile.ActionSupport;
import net.shibboleth.idp.profile.IdPEventIds;

import org.opensaml.profile.ProfileException;
import org.opensaml.profile.context.ProfileRequestContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.webflow.execution.Event;
import org.springframework.webflow.execution.RequestContext;


/**
 * A Spring-aware action to write the attribute context information to the external output sink.
 * This is for use in command line scenarios where there is no servlet environment for output.
 * 
 * @event {@link org.opensaml.profile.action.EventIds#PROCEED_EVENT_ID}
 * @event {@link IdPEventIds#INVALID_ATTRIBUTE_CTX}
 */
public final class OutputAttributes extends AbstractProfileAction {

    /** Class logger. */
    @Nonnull private final Logger log = LoggerFactory.getLogger(OutputAttributes.class);

    /** {@inheritDoc} */
    @Override
    protected Event doExecute(@Nonnull final RequestContext springRequestContext,
            @Nonnull final ProfileRequestContext profileRequestContext) throws ProfileException {
        
        AttributeContext attributeContext = profileRequestContext.getSubcontext(AttributeContext.class, false);
        if (attributeContext == null) {
            log.debug("{} No attribute context, no attributes to filter", getLogPrefix());
            return ActionSupport.buildEvent(this, IdPEventIds.INVALID_ATTRIBUTE_CTX);
        }
        
        try {
            Writer writer = springRequestContext.getExternalContext().getResponseWriter();
            for (IdPAttribute a : attributeContext.getIdPAttributes().values()) {
                writer.write("Attribute: " + a.getId() + "\n");
                if (a.getDisplayNames().containsKey(Locale.getDefault())) {
                    writer.write("\tDisplay name: " + a.getDisplayNames().get(Locale.getDefault()) + "\n");
                }
                
                if (!a.getValues().isEmpty()) {
                    writer.write("\tValues:\n");
                    for (IdPAttributeValue av : a.getValues()) {
                        writer.write("\t\t" + av.getValue().toString() + "\n");
                    }
                } else {
                    writer.write("\t(no values)\n");
                }
            }
        } catch (IOException e) {
            log.error(getLogPrefix() + " I/O error writing attributes to output context", e);
            return ActionSupport.buildEvent(this, org.opensaml.profile.action.EventIds.IO_ERROR);
        }
        
        return ActionSupport.buildProceedEvent(this);
    }
}