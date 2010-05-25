/*
 * Copyright 2010 University Corporation for Advanced Internet Development, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package edu.internet2.middleware.shibboleth.idp.consent.persistence;

import java.util.Collection;

import org.joda.time.DateTime;

import edu.internet2.middleware.shibboleth.idp.consent.components.TermsOfUse;
import edu.internet2.middleware.shibboleth.idp.consent.entities.AgreedTermsOfUse;
import edu.internet2.middleware.shibboleth.idp.consent.entities.Attribute;
import edu.internet2.middleware.shibboleth.idp.consent.entities.AttributeReleaseConsent;
import edu.internet2.middleware.shibboleth.idp.consent.entities.Principal;
import edu.internet2.middleware.shibboleth.idp.consent.entities.RelyingParty;

/**
 *
 */
public interface Storage {

    public abstract AgreedTermsOfUse createAgreedTermsOfUse(final Principal principal, final TermsOfUse termsOfUse, final DateTime agreeDate);
    
    public abstract AttributeReleaseConsent createAttributeReleaseConsent(final Principal principal, final RelyingParty relyingParty,
            final Attribute attribute, final DateTime releaseDate);
    
    public abstract Principal createPrincipal(final String uniqueId, final DateTime accessDate);
    
    public abstract RelyingParty createRelyingParty(final String entityId);

    public abstract boolean deleteAgreedTermsOfUse(final Principal principal, final TermsOfUse termsOfUse);

    public abstract int deleteAgreedTermsOfUses(final Principal principal);

    public abstract boolean deleteAttributeReleaseConsent(final Principal principal, final RelyingParty relyingParty,
            final Attribute attribute);

    public abstract int deleteAttributeReleaseConsents(final Principal principal);

    public abstract int deleteAttributeReleaseConsents(final Principal principal, final RelyingParty relyingParty);

    public abstract boolean deletePrincipal(final Principal principal);

    public abstract boolean deleteRelyingParty(final RelyingParty relyingParty);

    public abstract boolean containsPrincipal(final String uniqueId);

    public abstract boolean containsRelyingParty(final String entityId);

    public abstract AgreedTermsOfUse readAgreedTermsOfUse(final Principal principal, final TermsOfUse termsOfUse);

    public abstract Collection<AgreedTermsOfUse> readAgreedTermsOfUses(final Principal principal);

    public abstract AttributeReleaseConsent readAttributeReleaseConsent(final Principal principal, final RelyingParty relyingParty,
            Attribute attribute);

    public abstract Collection<AttributeReleaseConsent> readAttributeReleaseConsents(final Principal principal);

    public abstract Collection<AttributeReleaseConsent> readAttributeReleaseConsents(final Principal principal,
            RelyingParty relyingParty);

    public abstract Principal readPrincipal(final String uniqueId);

    public abstract RelyingParty readRelyingParty(final String entityId);

    public abstract boolean updateAgreedTermsOfUse(final Principal principal, final TermsOfUse termsOfUse, final DateTime agreeDate);
    
    public abstract boolean updateAttributeReleaseConsent(final Principal principal, final RelyingParty relyingParty,
            final Attribute attribute, final DateTime relaseDate);

    public abstract boolean updatePrincipal(final Principal principal);

    public abstract boolean updateRelyingParty(final RelyingParty relyingParty);

}