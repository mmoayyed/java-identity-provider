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

import net.shibboleth.idp.attribute.Attribute;
import net.shibboleth.idp.profile.AbstractIdentityProviderAction;
import net.shibboleth.idp.profile.ProfileRequestContext;

import org.opensaml.saml1.core.AttributeStatement;
import org.opensaml.saml1.core.Response;
import org.springframework.webflow.execution.Event;
import org.springframework.webflow.execution.RequestContext;

//TODO need attributes to be encoded

/**
 * Builds an {@link AttributeStatement} and adds it to the {@link Response} set as the message of the
 * {@link ProfileRequestContext#getOutboundMessageContext()}. The {@link Attribute} set to be encoded is drawn from the
 * ...
 */
public class AddAttributeStatementToAssertion extends AbstractIdentityProviderAction<Object, Response> {

    /** {@inheritDoc} */
    public Event doExecute(RequestContext springRequestContext,
            ProfileRequestContext<Object, Response> profileRequestContext) throws Throwable {
        // TODO Auto-generated method stub
        // TODO need to add an assertion if it's not there already
        return null;
    }

}