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

package net.shibboleth.idp.cas.flow;

import net.shibboleth.idp.authn.context.SubjectContext;
import net.shibboleth.idp.profile.ActionSupport;
import org.opensaml.profile.context.ProfileRequestContext;
import org.springframework.webflow.execution.Event;
import org.springframework.webflow.execution.RequestContext;

import javax.annotation.Nonnull;

/**
 * Creates the following contexts needed for attribute resolution:
 * <ul>
 *     <li>{@link net.shibboleth.idp.attribute.context.AttributeContext} -
 *         Child of {@link net.shibboleth.idp.profile.context.RelyingPartyContext} will hold resolved attributes.</li>
 *     <li>{@link net.shibboleth.idp.authn.context.SubjectContext} -
 *         Contains IdP session principal name needed for attribute resolution.</li>
 * </ul>
 *
 * @author Marvin S. Addison
 */
public class BuildAttributeContextAction extends AbstractCASProtocolAction{

    @Override
    protected Event doExecute(
        final @Nonnull RequestContext springRequestContext,
        final @Nonnull ProfileRequestContext profileRequestContext) {
        final SubjectContext sc = new SubjectContext();
        sc.setPrincipalName(getIdPSession(profileRequestContext).getPrincipalName());
        profileRequestContext.addSubcontext(sc);
        return ActionSupport.buildProceedEvent(this);
    }
}
