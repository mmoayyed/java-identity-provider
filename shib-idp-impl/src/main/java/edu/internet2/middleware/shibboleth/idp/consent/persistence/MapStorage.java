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

import java.util.ArrayList;
import java.util.Collection;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


import edu.internet2.middleware.shibboleth.idp.consent.components.TermsOfUse;
import edu.internet2.middleware.shibboleth.idp.consent.entities.AgreedTermsOfUse;
import edu.internet2.middleware.shibboleth.idp.consent.entities.Attribute;
import edu.internet2.middleware.shibboleth.idp.consent.entities.AttributeReleaseConsent;
import edu.internet2.middleware.shibboleth.idp.consent.entities.Principal;
import edu.internet2.middleware.shibboleth.idp.consent.entities.RelyingParty;

/**
 *
 */
public class MapStorage implements Storage {
    
    private final Logger logger = LoggerFactory.getLogger(JDBCStorage.class);
    
    private final ConcurrentMap<String, ConcurrentMap> cache;
    
    private final ConcurrentMap<String, Principal> principalPartition;
    private final ConcurrentMap<String, RelyingParty> relyingPartyPartition;
    private final ConcurrentMap<Principal, ConcurrentMap<TermsOfUse, AgreedTermsOfUse>> agreedTermsOfUsePartition;
    private final ConcurrentMap<Principal, ConcurrentMap<RelyingParty, ConcurrentMap<Attribute, AttributeReleaseConsent>>> attributeReleaseConsentPartition;
     
    public MapStorage() {        
        // TODO: check how to (re-)initialize the cache.        
        cache = new ConcurrentHashMap<String, ConcurrentMap>();
        
        principalPartition = cache.putIfAbsent("principalPartition",
                new ConcurrentHashMap<String, Principal>());
        relyingPartyPartition = cache.putIfAbsent("relyingPartyPartition",
                new ConcurrentHashMap<String, RelyingParty>());
        agreedTermsOfUsePartition = cache.putIfAbsent("agreedTermsOfUsePartition",
                new ConcurrentHashMap<Principal, ConcurrentMap<TermsOfUse, AgreedTermsOfUse>>());
        attributeReleaseConsentPartition = cache.putIfAbsent("attributeReleaseConsentPartition",
                new ConcurrentHashMap<Principal, ConcurrentMap<RelyingParty, ConcurrentMap<Attribute, AttributeReleaseConsent>>>());   
    }

    /** {@inheritDoc} */
    public final AgreedTermsOfUse createAgreedTermsOfUse(final Principal principal, final TermsOfUse termsOfUse, final DateTime agreeDate) {
        agreedTermsOfUsePartition.putIfAbsent(principal, new ConcurrentHashMap<TermsOfUse, AgreedTermsOfUse>());
        if (agreedTermsOfUsePartition.get(principal).containsKey(termsOfUse)) {
            logger.warn("AgreedTermsOfUse {} already exists", agreedTermsOfUsePartition.get(principal).get(termsOfUse));
        }
        final AgreedTermsOfUse agreedTermsOfUse = new AgreedTermsOfUse(termsOfUse, agreeDate);
        agreedTermsOfUsePartition.get(principal).put(termsOfUse, agreedTermsOfUse);
        return agreedTermsOfUse;
    }

    /** {@inheritDoc} */
    public final AttributeReleaseConsent createAttributeReleaseConsent(final Principal principal, final RelyingParty relyingParty,
            final Attribute attribute, final DateTime releaseDate) {
        attributeReleaseConsentPartition.putIfAbsent(principal, new ConcurrentHashMap<RelyingParty, ConcurrentMap<Attribute, AttributeReleaseConsent>>());
        attributeReleaseConsentPartition.get(principal).putIfAbsent(relyingParty, new ConcurrentHashMap<Attribute, AttributeReleaseConsent>());
        if (attributeReleaseConsentPartition.get(principal).get(relyingParty).containsKey(attribute)) {
            logger.warn("AttributeReleaseConsent {} already exists", attributeReleaseConsentPartition.get(principal).get(relyingParty).get(attribute));
        }
        final AttributeReleaseConsent attributeReleaseConsent = new AttributeReleaseConsent(attribute, releaseDate);
        attributeReleaseConsentPartition.get(principal).get(relyingParty).put(attribute, attributeReleaseConsent);
        return attributeReleaseConsent;
        
    }

    /** {@inheritDoc} */
    public final Principal createPrincipal(final String uniqueId, final DateTime accessDate) {
        if (principalPartition.containsKey(uniqueId)) {
            logger.warn("Principal {} already exists", uniqueId);
        }    
        final Principal principal = new Principal(uniqueId, accessDate, accessDate, false);
        principalPartition.put(uniqueId, principal);
        return principal;
    }

    /** {@inheritDoc} */
    public final RelyingParty createRelyingParty(final String entityId) {
        if (relyingPartyPartition.containsKey(entityId)) {
            logger.warn("RelyingParty {} already exists", entityId);
        } 
        final RelyingParty relyingParty = new RelyingParty(entityId);
        relyingPartyPartition.put(entityId, relyingParty);
        return relyingParty;
    }

    /** {@inheritDoc} */
    public boolean deleteAgreedTermsOfUse(final Principal principal, final TermsOfUse termsOfUse) {
        throw new UnsupportedOperationException();
    }

    /** {@inheritDoc} */
    public int deleteAgreedTermsOfUses(final Principal principal) {
        throw new UnsupportedOperationException();
    }

    /** {@inheritDoc} */
    public boolean deleteAttributeReleaseConsent(final Principal principal, final RelyingParty relyingParty, final Attribute attribute) {
        throw new UnsupportedOperationException();
    }

    /** {@inheritDoc} */
    public int deleteAttributeReleaseConsents(final Principal principal) {
        throw new UnsupportedOperationException();
    }

    /** {@inheritDoc} */
    public int deleteAttributeReleaseConsents(final Principal principal, final RelyingParty relyingParty) {
        if (!attributeReleaseConsentPartition.containsKey(principal) || !attributeReleaseConsentPartition.get(principal).containsKey(relyingParty)) {
            return 0;
        }       
        return attributeReleaseConsentPartition.get(principal).remove(relyingParty).size();
    }

    /** {@inheritDoc} */
    public boolean deletePrincipal(final Principal principal) {
        throw new UnsupportedOperationException();
    }

    /** {@inheritDoc} */
    public boolean deleteRelyingParty(final RelyingParty relyingParty) {
        throw new UnsupportedOperationException();
    }

    /** {@inheritDoc} */
    public boolean containsPrincipal(final String uniqueId) {
        return principalPartition.containsKey(uniqueId);
    }

    /** {@inheritDoc} */
    public boolean containsRelyingParty(final String entityId) {
        return relyingPartyPartition.containsKey(entityId);
    }

    /** {@inheritDoc} */
    public final AgreedTermsOfUse readAgreedTermsOfUse(final Principal principal, final TermsOfUse termsOfUse) {
        if (!agreedTermsOfUsePartition.containsKey(principal)) {
            return null;
        }
        return agreedTermsOfUsePartition.get(principal).get(termsOfUse);
    }

    /** {@inheritDoc} */
    public final Collection<AgreedTermsOfUse> readAgreedTermsOfUses(final Principal principal) {
        if (!agreedTermsOfUsePartition.containsKey(principal)) {
            return new ArrayList<AgreedTermsOfUse>();
        }
        return agreedTermsOfUsePartition.get(principal).values();
    }

    /** {@inheritDoc} */
    public final AttributeReleaseConsent readAttributeReleaseConsent(final Principal principal, final RelyingParty relyingParty,
            final Attribute attribute) {
        
        if (!attributeReleaseConsentPartition.containsKey(principal) || !attributeReleaseConsentPartition.get(principal).containsKey(relyingParty)) {
            return null;
        }
        
        return attributeReleaseConsentPartition.get(principal).get(relyingParty).get(attribute);
    }

    /** {@inheritDoc} */
    public final Collection<AttributeReleaseConsent> readAttributeReleaseConsents(final Principal principal) {
        throw new UnsupportedOperationException();
    }

    /** {@inheritDoc} */
    public final Collection<AttributeReleaseConsent> readAttributeReleaseConsents(final Principal principal, final RelyingParty relyingParty) {
        if (!attributeReleaseConsentPartition.containsKey(principal) || !attributeReleaseConsentPartition.get(principal).containsKey(relyingParty)) {
            return new ArrayList<AttributeReleaseConsent>();
        }
        
        return attributeReleaseConsentPartition.get(principal).get(relyingParty).values();
    }

    /** {@inheritDoc} */
    public final Principal readPrincipal(final String uniqueId) {
        return principalPartition.get(uniqueId);
    }

    /** {@inheritDoc} */
    public final RelyingParty readRelyingParty(final String entityId) {
        return relyingPartyPartition.get(entityId);
    }

    /** {@inheritDoc} */
    public boolean updateAgreedTermsOfUse(final Principal principal, final TermsOfUse termsOfUse, final DateTime agreeDate) {
        if (!agreedTermsOfUsePartition.containsKey(principal)) {
            return false;
        }
        AgreedTermsOfUse agreedTermsOfUse = new AgreedTermsOfUse(termsOfUse, agreeDate);
        return agreedTermsOfUsePartition.get(principal).replace(termsOfUse, agreedTermsOfUse) != null;
    }

    /** {@inheritDoc} */
    public boolean updateAttributeReleaseConsent(final Principal principal, final RelyingParty relyingParty,
            final Attribute attribute, final DateTime relaseDate) {
        
        if (!attributeReleaseConsentPartition.containsKey(principal) || !attributeReleaseConsentPartition.get(principal).containsKey(relyingParty)) {
            return false;
        }
        
        final AttributeReleaseConsent attributeReleaseConsent = new AttributeReleaseConsent(attribute, relaseDate);
        return attributeReleaseConsentPartition.get(principal).get(relyingParty).replace(attribute, attributeReleaseConsent) != null;
    }

    /** {@inheritDoc} */
    public boolean updatePrincipal(final Principal principal) {
        return principalPartition.replace(principal.getUniqueId(), principal) != null;
    }

    /** {@inheritDoc} */
    public boolean updateRelyingParty(final RelyingParty relyingParty) {
        throw new UnsupportedOperationException();
    }
}
