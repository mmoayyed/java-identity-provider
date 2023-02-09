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

package net.shibboleth.idp.saml.saml2.profile.config.logic;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.opensaml.profile.context.ProfileRequestContext;

import net.shibboleth.profile.config.ProfileConfiguration;
import net.shibboleth.idp.profile.context.RelyingPartyContext;
import net.shibboleth.idp.profile.logic.AbstractRelyingPartyPredicate;
import net.shibboleth.idp.saml.saml2.profile.config.SAML2ProfileConfiguration;
import net.shibboleth.shared.primitive.LoggerFactory;

import org.slf4j.Logger;

/** Predicate that decides whether to ignore a request signature. */
public class IgnoreRequestSignaturesPredicate extends AbstractRelyingPartyPredicate {

    /** Class logger. */
    @Nonnull private final Logger log = LoggerFactory.getLogger(IgnoreRequestSignaturesPredicate.class);
    
    /** {@inheritDoc} */
    public boolean test(@Nullable final ProfileRequestContext input) {
        
        final RelyingPartyContext rpCtx = getRelyingPartyContextLookupStrategy().apply(input);
        if (input == null || rpCtx == null) {
            log.debug("No RelyingPartyContext found, assuming signatures should be checked");
            return false;
        }
        
        final ProfileConfiguration pc = rpCtx.getProfileConfig();
        if (!(pc instanceof SAML2ProfileConfiguration)) {
            log.debug("No SAML 2 profile configuration found, assuming signatures should be checked");
            return false;
        }
        
        return ((SAML2ProfileConfiguration) pc).isIgnoreRequestSignatures(input);
    }

}