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

package net.shibboleth.idp.consent.flow.tou;

import java.util.Collections;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import net.shibboleth.idp.consent.Consent;
import net.shibboleth.idp.profile.context.ProfileInterceptorContext;

import org.opensaml.profile.context.ProfileRequestContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Terms of use consent action to create the consent to be chosen by user.
 */
public class CreateTermsOfUseChoice extends AbstractTermsOfUseAction {

    /** Class logger. */
    @Nonnull private final Logger log = LoggerFactory.getLogger(CreateTermsOfUseChoice.class);

    /** Terms of use. */
    @Nullable private TermsOfUse termsOfUse;

    /** {@inheritDoc} */
    @Override protected boolean doPreExecute(@Nonnull final ProfileRequestContext profileRequestContext,
            @Nonnull final ProfileInterceptorContext interceptorContext) {

        if (!super.doPreExecute(profileRequestContext, interceptorContext)) {
            return false;
        }
        
        termsOfUse = getTermsOfUseContext().getTermsOfUse();
        if (termsOfUse == null) {
            log.debug("{} Terms of use cannot be null", getLogPrefix());
            // TODO event ?
            return false;
        }

        return true;
    }

    /** {@inheritDoc} */
    @Override protected void doExecute(@Nonnull final ProfileRequestContext profileRequestContext,
            @Nonnull final ProfileInterceptorContext interceptorContext) {

        // Build new consent object.
        final Consent consent = new Consent();
        consent.setId(termsOfUse.getId());

        // Optionally include hash as consent value.
        if (getConsentFlowDescriptor().compareValues()) {
            final String hash = getTermsOfUseFlowDescriptor().getTermsOfUseHashFunction().apply(termsOfUse);
            consent.setValue(hash);
        }

        // TODO maintain previous result ?

        getConsentContext().setCurrentConsents(Collections.singletonMap(consent.getId(), consent));
    }
}
