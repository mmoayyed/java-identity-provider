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

import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;


/**
 *
 */
public class Principal {

    private long id = 0;
    
    private String uniqueId;

    private Date firstAccess;

    private Date lastAccess;

    private boolean globalConsent = false;

    private Collection<AgreedTermsOfUse> agreedTermsOfUses;

    final private Map<RelyingParty, Collection<AttributeReleaseConsent>> attributeReleaseConsents = new HashMap<RelyingParty, Collection<AttributeReleaseConsent>>();

    /**
     * @return Returns the agreedTermsOfUse.
     */
    public Collection<AgreedTermsOfUse> getAgreedTermsOfUses() {
        return agreedTermsOfUses;
    }

    /**
     * @return Returns the attributeReleaseConsents.
     */
    public Map<RelyingParty, Collection<AttributeReleaseConsent>> getAttributeReleaseConsents() {
        throw new UnsupportedOperationException();
    }

    public Collection<AttributeReleaseConsent> getAttributeReleaseConsents(final RelyingParty relyingParty) {
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
        return this.agreedTermsOfUses.contains(termsOfUse);
    }
    
    public boolean hasApproved(Collection<Attribute> attributes, RelyingParty relyingParty) {
        Collection<AttributeReleaseConsent> attributeReleaseConsentsForRelyingParty = getAttributeReleaseConsents(relyingParty);      
        boolean approved;
        for (Attribute attribute: attributes) {
            approved = false;
            for (AttributeReleaseConsent attributeReleaseConsent: attributeReleaseConsentsForRelyingParty) {
                if (attributeReleaseConsent.getAttribute().equals(attribute)) {
                        approved = true;
                        break;
                }
            }
            if (!approved) {
                return false;
            }
        }     
        return true;
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
    public void setAgreedTermsOfUses(final Collection<AgreedTermsOfUse> agreedTermsOfUses) {
        this.agreedTermsOfUses = agreedTermsOfUses;
    }

    /**
     * @param attributeReleaseConsents The attributeReleaseConsents to set.
     */
    public void setAttributeReleaseConsents(
            final Map<RelyingParty, Collection<AttributeReleaseConsent>> attributeReleaseConsents) {
        throw new UnsupportedOperationException();
    }

    public void setAttributeReleaseConsents(final RelyingParty relyingParty,
            final Collection<AttributeReleaseConsent> attributeReleaseConsentsForRelyingParty) {
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
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + (int) (id ^ (id >>> 32));
        result = prime * result + ((uniqueId == null) ? 0 : uniqueId.hashCode());
        return result;
    }

    /** {@inheritDoc} */
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        Principal other = (Principal) obj;
        if (id != other.id)
            return false;
        if (uniqueId == null) {
            if (other.uniqueId != null)
                return false;
        } else if (!uniqueId.equals(other.uniqueId))
            return false;
        return true;
    }

    /** {@inheritDoc} */
    @Override
    public String toString() {
        return "Principal [id=" + id + ", uniqueId=" + uniqueId + ", firstAccess=" + firstAccess + ", lastAccess="
                + lastAccess + ", globalConsent=" + globalConsent + ", agreedTermsOfUses=" + agreedTermsOfUses
                + ", attributeReleaseConsents=" + attributeReleaseConsents + "]";
    }

}
