/*
 * Copyright 2009 University Corporation for Advanced Internet Development, Inc.
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

package edu.internet2.middleware.shibboleth.idp.consent.entities;

import java.util.Date;
import java.util.Map;
import java.util.Set;

/**
 *
 */
public class Principal {

    private long id;

    private String uniqueId;

    private Date firstAccess;

    private Date lastAccess;

    private boolean globalConsent;

    private Set<AgreedTermsOfUse> agreedTermsOfUse;

    private Map<RelyingParty, Set<AttributeReleaseConsent>> attributeReleaseConsents;

    /**
     * @return Returns the agreedTermsOfUse.
     */
    public Set<AgreedTermsOfUse> getAgreedTermsOfUse() {
        return agreedTermsOfUse;
    }

    /**
     * @return Returns the attributeReleaseConsents.
     */
    public Map<RelyingParty, Set<AttributeReleaseConsent>> getAttributeReleaseConsents() {
        throw new UnsupportedOperationException();
    }

    public Set<AttributeReleaseConsent> getAttributeReleaseConsents(final RelyingParty relyingParty) {
        return attributeReleaseConsents.get(relyingParty);
    }

    /**
     * @return Returns the firstAccess.
     */
    public Date getFirstAccess() {
        return firstAccess;
    }

    /**
     * @return Returns the id.
     */
    public long getId() {
        return id;
    }

    /**
     * @return Returns the lastAccess.
     */
    public Date getLastAccess() {
        return lastAccess;
    }

    /**
     * @return Returns the uniqueId.
     */
    public String getUniqueId() {
        return uniqueId;
    }

    public boolean hasAcceptedTermsOfUse(final TermsOfUse termsOfUse) {
        for (AgreedTermsOfUse agreedToU : this.agreedTermsOfUse) {
            if (agreedToU.equals(termsOfUse)) {
                return true;
            }
        }
        return false;
    }

    /**
     * @return Returns the globalConsent.
     */
    public boolean hasGlobalConsent() {
        return globalConsent;
    }

    /**
     * @param agreedTermsOfUse The agreedTermsOfUse to set.
     */
    public void setAgreedTermsOfUse(final Set<AgreedTermsOfUse> agreedTermsOfUse) {
        this.agreedTermsOfUse = agreedTermsOfUse;
    }

    /**
     * @param attributeReleaseConsents The attributeReleaseConsents to set.
     */
    public void setAttributeReleaseConsents(
            final Map<RelyingParty, Set<AttributeReleaseConsent>> attributeReleaseConsents) {
        throw new UnsupportedOperationException();
    }

    public void setAttributeReleaseConsents(final RelyingParty relyingParty,
            final Set<AttributeReleaseConsent> attributeReleaseConsentsForRelyingParty) {
        attributeReleaseConsents.put(relyingParty, attributeReleaseConsentsForRelyingParty);
    }

    /**
     * @param firstAccess The firstAccess to set.
     */
    public void setFirstAccess(final Date firstAccess) {
        this.firstAccess = firstAccess;
    }

    /**
     * @param globalConsent The globalConsent to set.
     */
    public void setGlobalConsent(final boolean globalConsent) {
        this.globalConsent = globalConsent;
    }

    /**
     * @param id The id to set.
     */
    public void setId(final long id) {
        this.id = id;
    }

    /**
     * @param lastAccess The lastAccess to set.
     */
    public void setLastAccess(final Date lastAccess) {
        this.lastAccess = lastAccess;
    }

    /**
     * @param uniqueId The uniqueId to set.
     */
    public void setUniqueId(final String uniqueId) {
        this.uniqueId = uniqueId;
    }

    /** {@inheritDoc} */
    @Override
    public String toString() {
        return "Principal [id=" + id + ", uniqueId=" + uniqueId + ", firstAccess=" + firstAccess + ", lastAccess="
                + lastAccess + ", globalConsent=" + globalConsent + ", agreedTermsOfUse=" + agreedTermsOfUse
                + ", attributeReleaseConsents=" + attributeReleaseConsents + "]";
    }

}
