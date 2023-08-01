/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
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

import java.time.Instant;

import javax.annotation.Nonnull;
import javax.xml.namespace.QName;

import net.shibboleth.idp.cas.protocol.TicketValidationRequest;
import net.shibboleth.idp.cas.protocol.TicketValidationResponse;

import org.opensaml.profile.action.EventException;
import org.opensaml.profile.context.ProfileRequestContext;
import org.opensaml.saml.saml1.core.Response;
import org.opensaml.saml.saml1.core.Status;
import org.opensaml.saml.saml1.core.StatusCode;
import org.opensaml.saml.saml1.core.StatusMessage;

/**
 * Creates the SAML response message for failed ticket validation at the <code>/samlValidate</code> URI.
 *
 * @author Marvin S. Addison
 */
public class BuildSamlValidationFailureMessageAction extends AbstractOutgoingSamlMessageAction {

    /** {@inheritDoc} */
    @Override
    @Nonnull protected Response buildSamlResponse(@Nonnull final ProfileRequestContext profileRequestContext)
            throws EventException {

        final TicketValidationRequest request = getCASRequest(profileRequestContext);
        final TicketValidationResponse validationResponse = getCASResponse(profileRequestContext);
        final Response response = newSAMLObject(Response.class, Response.DEFAULT_ELEMENT_NAME);
        response.setID(request.getTicket());
        response.setIssueInstant(Instant.now());
        final Status status = newSAMLObject(Status.class, Status.DEFAULT_ELEMENT_NAME);
        final StatusCode statusCode = newSAMLObject(StatusCode.class, StatusCode.DEFAULT_ELEMENT_NAME);
        statusCode.setValue(new QName(NAMESPACE, validationResponse.getErrorCode()));
        status.setStatusCode(statusCode);
        final StatusMessage message = newSAMLObject(StatusMessage.class, StatusMessage.DEFAULT_ELEMENT_NAME);
        message.setValue(validationResponse.getErrorDetail());
        status.setStatusMessage(message);
        response.setStatus(status);

        return response;
    }

}