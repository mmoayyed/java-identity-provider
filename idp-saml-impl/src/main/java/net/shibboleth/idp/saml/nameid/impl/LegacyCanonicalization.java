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

package net.shibboleth.idp.saml.nameid.impl;

import javax.annotation.Nonnull;

import org.opensaml.profile.action.ActionSupport;
import org.opensaml.profile.context.ProfileRequestContext;
import org.slf4j.Logger;

import com.google.common.base.Predicates;

import net.shibboleth.idp.authn.AbstractSubjectCanonicalizationAction;
import net.shibboleth.idp.authn.AuthnEventIds;
import net.shibboleth.idp.authn.context.SubjectCanonicalizationContext;
import net.shibboleth.idp.saml.nameid.NameIDCanonicalizationFlowDescriptor;
import net.shibboleth.shared.primitive.DeprecationSupport;
import net.shibboleth.shared.primitive.DeprecationSupport.ObjectType;
import net.shibboleth.shared.primitive.LoggerFactory;

/**
 * Action to fail if asked to perform C14N ..
 * 
 * @deprecated
 */
@Deprecated
public class LegacyCanonicalization extends AbstractSubjectCanonicalizationAction {

    /** Class logger. */
    @Nonnull private final Logger log = LoggerFactory.getLogger(LegacyCanonicalization.class);
    
    /** {@inheritDoc} */
    @Override protected boolean doPreExecute(@Nonnull final ProfileRequestContext profileRequestContext,
            @Nonnull final SubjectCanonicalizationContext c14nContext) {

        log.error("Legacy PrincipalConnectors no longer supported");
        ActionSupport.buildEvent(profileRequestContext, AuthnEventIds.INVALID_SUBJECT_C14N_CTX);
        
        return false;
    }
    
    /** Factory used to generate a disabled flow descriptor for backward compatibility.
     * 
     * @return an appropriate FlowDescriptor 
     */
    public static NameIDCanonicalizationFlowDescriptor c14LegacyPrincipalConnectorFactory() {
        // V4 deprecation, remove this class in V5.
        DeprecationSupport.warn(ObjectType.BEAN, "c14n/LegacyPrincipalConnector", "c14n/subject-c14n.xml", "<remove>");
        final NameIDCanonicalizationFlowDescriptor result = new NameIDCanonicalizationFlowDescriptor();
        result.setActivationCondition(Predicates.alwaysFalse());
        return result;
    }
    
}