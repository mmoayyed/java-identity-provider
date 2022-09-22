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

package net.shibboleth.idp.cas.flow.impl;

import java.io.IOException;
import java.io.PrintWriter;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import net.shibboleth.idp.cas.protocol.TicketValidationRequest;
import net.shibboleth.idp.cas.protocol.TicketValidationResponse;
import net.shibboleth.shared.annotation.constraint.NotEmpty;

import org.opensaml.profile.action.ActionSupport;
import org.opensaml.profile.action.EventException;
import org.opensaml.profile.context.ProfileRequestContext;

/**
 * CAS 1.0 protocol response handler.
 *
 * @author Marvin S. Addison
 * @since 3.3.0
 */
public class WriteValidateResponseAction extends
        AbstractCASProtocolAction<TicketValidationRequest, TicketValidationResponse>  {
    
    /** CAS 1.0 protocol content type is plain text. */
    @Nonnull @NotEmpty private static final String CONTENT_TYPE = "text/plain;charset=utf-8";

    /** Protocol success flag indicates what kind of response to provide. */
    private final boolean success;

    /** CAS response. */
    @Nullable private TicketValidationResponse response;
    
    /**
     * Constructor.
     *
     * @param successFlag success flag
     */
    public WriteValidateResponseAction(final boolean successFlag) {
        success = successFlag;
    }

    @Override
    protected boolean doPreExecute(@Nonnull final ProfileRequestContext profileRequestContext) {
        if (!super.doPreExecute(profileRequestContext)) {
            return false;
        }

        try {
            response = getCASResponse(profileRequestContext);
        } catch (final EventException e) {
            ActionSupport.buildEvent(profileRequestContext, e.getEventID());
            return false;
        }

        return true;
    }
    
    @Override
    protected void doExecute(@Nonnull final ProfileRequestContext profileRequestContext) {

        try {
            getHttpServletResponse().setContentType(CONTENT_TYPE);
            final PrintWriter output = getHttpServletResponse().getWriter();
            if (success) {
                output.print("yes\n");
                output.print(response.getUserName() + '\n');
            } else {
                output.print("no\n\n");
            }
            output.flush();
        } catch (final IOException e) {
            throw new RuntimeException("IO error writing CAS protocol response", e);
        }
    }

}