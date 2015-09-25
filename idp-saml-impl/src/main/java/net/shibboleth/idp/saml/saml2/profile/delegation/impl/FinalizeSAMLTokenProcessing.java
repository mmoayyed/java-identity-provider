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

package net.shibboleth.idp.saml.saml2.profile.delegation.impl;

import net.shibboleth.idp.authn.context.SubjectCanonicalizationContext;
import net.shibboleth.idp.authn.context.SubjectContext;
import net.shibboleth.idp.profile.AbstractProfileAction;

import org.opensaml.profile.context.ProfileRequestContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

//TODO need a lot more Javadoc detail here, and event ID's supported.

/**
 * Post-process the results of token and subject canonicalization.
 */
public class FinalizeSAMLTokenProcessing extends AbstractProfileAction {
    
    /** Logger. */
    private Logger log = LoggerFactory.getLogger(FinalizeSAMLTokenProcessing.class);

    /** {@inheritDoc} */
    protected void doExecute(ProfileRequestContext profileRequestContext) {
        SubjectCanonicalizationContext c14nContext = 
                profileRequestContext.getSubcontext(SubjectCanonicalizationContext.class);
        profileRequestContext.removeSubcontext(SubjectCanonicalizationContext.class);
        
        //TODO seems that without populating SubjectContext authenticationResults, there's nothing
        // much useful to be done with the Subject here. Confirm.
        
        /**
        Subject subject = c14nContext.getSubject();
        
        log.debug("Transferring principal name canonicalized from SAML NameID to a UsernamePrincipal context: {}", 
                c14nContext.getPrincipalName());
        
        subject.getPrincipals().add(new UsernamePrincipal(c14nContext.getPrincipalName()));
        */
        
        SubjectContext subjectContext = profileRequestContext.getSubcontext(SubjectContext.class, true);
        subjectContext.setPrincipalName(c14nContext.getPrincipalName());
    }

}
