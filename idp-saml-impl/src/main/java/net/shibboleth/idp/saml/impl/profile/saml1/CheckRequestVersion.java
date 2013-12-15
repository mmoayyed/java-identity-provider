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

import javax.annotation.Nonnull;

import net.shibboleth.ext.spring.webflow.Event;
import net.shibboleth.ext.spring.webflow.Events;
import net.shibboleth.idp.profile.AbstractProfileAction;
import net.shibboleth.idp.profile.ActionSupport;
import org.opensaml.profile.ProfileException;
import org.opensaml.profile.action.EventIds;
import org.opensaml.profile.context.ProfileRequestContext;
import net.shibboleth.idp.saml.profile.SAMLEventIds;

import org.opensaml.saml.common.SAMLVersion;
import org.opensaml.saml.saml1.core.RequestAbstractType;
import org.springframework.webflow.execution.RequestContext;

import com.google.common.base.Objects;

/** Checks whether the inbound SAML request has the appropriate version. */
@Events({
        @Event(id = EventIds.PROCEED_EVENT_ID),
        @Event(id = SAMLEventIds.INVALID_MESSAGE_VERSION,
                description = "A message with a version other than 1.1 was received")})
public class CheckRequestVersion extends AbstractProfileAction<RequestAbstractType, Object> {

    /** {@inheritDoc} */
    protected org.springframework.webflow.execution.Event doExecute(
            @Nonnull final RequestContext springRequestContext,
                @Nonnull final ProfileRequestContext<RequestAbstractType, Object> profileRequestContext)
                        throws ProfileException {

        final RequestAbstractType request = profileRequestContext.getInboundMessageContext().getMessage();

        if (Objects.equal(SAMLVersion.VERSION_10, request.getVersion())
                || Objects.equal(SAMLVersion.VERSION_11, request.getVersion())) {
            return ActionSupport.buildProceedEvent(this);
        } else {
            return ActionSupport.buildEvent(this, SAMLEventIds.INVALID_MESSAGE_VERSION);
        }
    }
}