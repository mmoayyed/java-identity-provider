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
import javax.servlet.http.HttpServletResponse;

import net.shibboleth.idp.cas.protocol.TicketValidationRequest;
import net.shibboleth.idp.cas.protocol.TicketValidationResponse;
import org.opensaml.profile.context.ProfileRequestContext;
import org.springframework.webflow.execution.Event;
import org.springframework.webflow.execution.RequestContext;

/**
 * CAS 1.0 protocol response handler.
 *
 * @author Marvin S. Addison
 * @since 3.3.0
 */
public class WriteValidateResponseAction extends
        AbstractCASProtocolAction<TicketValidationRequest, TicketValidationResponse>  {
    /** CAS 1.0 protocol content type is plain text. */
    private static final String CONTENT_TYPE = "text/plain;charset=utf-8";

    /** Protocol success flag indicates what kind of response to provide. */
    private final boolean success;

    /**
     * Constructor.
     *
     * @param successFlag success flag
     */
    public WriteValidateResponseAction(final boolean successFlag) {
        success = successFlag;
    }

    @Override
    protected Event doExecute(
            final @Nonnull RequestContext springRequestContext,
            final @Nonnull ProfileRequestContext profileRequestContext) {

        final TicketValidationResponse response = getCASResponse(profileRequestContext);
        try {
            final HttpServletResponse servletResponse =
                    (HttpServletResponse) springRequestContext.getExternalContext().getNativeResponse();
            servletResponse.setContentType(CONTENT_TYPE);
            final PrintWriter output = servletResponse.getWriter();
            if (success) {
                output.print("yes\n");
                output.print(response.getUserName() + '\n');
            } else {
                output.print("no\n\n");
            }
            output.flush();
        } catch (IOException e) {
            throw new RuntimeException("IO error writing CAS protocol response", e);
        }
        return null;
    }
}
