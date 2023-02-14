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

package net.shibboleth.idp.test.flows;

import javax.annotation.Nonnull;

import net.shibboleth.idp.authn.context.SubjectContext;
import net.shibboleth.idp.profile.AbstractProfileAction;
import net.shibboleth.idp.profile.IdPEventIds;
import net.shibboleth.idp.profile.relyingparty.RelyingPartyConfiguration;
import net.shibboleth.profile.context.RelyingPartyContext;
import net.shibboleth.shared.component.ComponentInitializationException;

import org.opensaml.profile.action.ActionSupport;
import org.opensaml.profile.context.ProfileRequestContext;

public class SetupForResolver extends AbstractProfileAction {
    
    @Override
    protected void doExecute(@Nonnull final ProfileRequestContext profileRequestContext) {

        final SubjectContext sc = profileRequestContext.getSubcontext(SubjectContext.class, true);
        sc.setPrincipalName("PETER_THE_PRINCIPAL");
        
        final RelyingPartyContext rpContext = profileRequestContext.getOrCreateSubcontext(RelyingPartyContext.class);
        rpContext.setRelyingPartyId(AbstractFlowTest.SP_ENTITY_ID);
        
        final RelyingPartyConfiguration config = new RelyingPartyConfiguration();
        config.setId("test");
        config.setResponderId(AbstractFlowTest.IDP_ENTITY_ID);
        try {
            config.initialize();
        } catch (final ComponentInitializationException e) {
            ActionSupport.buildEvent(profileRequestContext, IdPEventIds.INVALID_RELYING_PARTY_CONFIG);
            return;
        }
        
        rpContext.setConfiguration(config);
    }
    
}