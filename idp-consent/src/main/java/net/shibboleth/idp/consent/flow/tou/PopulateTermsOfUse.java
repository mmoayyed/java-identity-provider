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

import java.util.Locale;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import net.shibboleth.idp.profile.context.ProfileInterceptorContext;
import net.shibboleth.idp.profile.context.SpringRequestContext;
import net.shibboleth.utilities.java.support.primitive.StringSupport;

import org.opensaml.profile.context.ProfileRequestContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Action to create a terms of use from a {@link org.springframework.context.MessageSource} and attach it to the terms
 * of use context.
 * 
 * TODO details
 */
public class PopulateTermsOfUse extends AbstractTermsOfUseAction {

    /** Class logger. */
    @Nonnull private final Logger log = LoggerFactory.getLogger(PopulateTermsOfUse.class);

    /** Spring request context. */
    @Nullable private SpringRequestContext springContext;

    /** Locale from Web Flow external context. */
    @Nullable private Locale locale;

    /** Terms of use identifier. */
    @Nullable private String termsOfUseId;

    /** Terms of use text. */
    @Nullable private String termsOfUseText;

    /** {@inheritDoc} */
    @Override protected boolean doPreExecute(@Nonnull final ProfileRequestContext profileRequestContext,
            @Nonnull final ProfileInterceptorContext interceptorContext) {

        if (!super.doPreExecute(profileRequestContext, interceptorContext)) {
            return false;
        }
        
        springContext = profileRequestContext.getSubcontext(SpringRequestContext.class);
        if (springContext == null) {
            log.debug("{} Spring request context cannot be null");
            return false;
        }

        locale = springContext.getRequestContext().getExternalContext().getLocale();
        log.debug("{} Locale '{}'", getLogPrefix(), locale);
        if (locale == null) {
            // TODO suitable default if null ?
            log.debug("{} Locale cannot be null");
            return false;
        }

        final String idMessageCode = getTermsOfUseFlowDescriptor().getIdMessageCode();
        termsOfUseId = StringSupport.trimOrNull(getMessage(idMessageCode, null, locale));
        if (termsOfUseId == null) {
            log.debug("{} No terms of use id resolved with message code '{}'", getLogPrefix(), idMessageCode);
            return false;
        }

        final String textMessageCode = getTermsOfUseFlowDescriptor().getTextMessageCode();
        termsOfUseText = getMessage(textMessageCode, null, locale);
        if (termsOfUseText == null) {
            log.debug("{} No terms of use text resolved with message code '{}'", getLogPrefix(), textMessageCode);
            return false;
        }

        return true;
    }

    /** {@inheritDoc} */
    @Override protected void doExecute(@Nonnull final ProfileRequestContext profileRequestContext,
            @Nonnull final ProfileInterceptorContext interceptorContext) {

        // TODO support TOU replacement args ?
        
        // TODO TOU per relying party ?

        final TermsOfUse termsOfUse = new TermsOfUse();
        termsOfUse.setId(termsOfUseId);
        termsOfUse.setText(termsOfUseText);

        getTermsOfUseContext().setTermsOfUse(termsOfUse);
    }
}
