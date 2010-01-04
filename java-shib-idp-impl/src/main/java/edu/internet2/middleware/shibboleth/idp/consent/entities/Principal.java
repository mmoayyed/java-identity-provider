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
import java.util.Set;

/**
 *
 */
public class Principal {

    private int id;

    private String uniqueId;

    private Date firstAccess;

    private Date lastAccess;

    private boolean globalConsent;

    Set<AgreedTermsOfUse> agreedTermsOfUse;

    Set<AttributeReleaseConsent> attributeReleaseConsents;

    /**
     * @return Returns the id.
     */
    public int getId() {
        return id;
    }

    /**
     * @param id The id to set.
     */
    public void setId(int id) {
        this.id = id;
    }

    /**
     * @return Returns the uniqueId.
     */
    public String getUniqueId() {
        return uniqueId;
    }

    /**
     * @param uniqueId The uniqueId to set.
     */
    public void setUniqueId(String uniqueId) {
        this.uniqueId = uniqueId;
    }

    /**
     * @return Returns the firstAccess.
     */
    public Date getFirstAccess() {
        return firstAccess;
    }

    /**
     * @param firstAccess The firstAccess to set.
     */
    public void setFirstAccess(Date firstAccess) {
        this.firstAccess = firstAccess;
    }

    /**
     * @return Returns the lastAccess.
     */
    public Date getLastAccess() {
        return lastAccess;
    }

    /**
     * @param lastAccess The lastAccess to set.
     */
    public void setLastAccess(Date lastAccess) {
        this.lastAccess = lastAccess;
    }

    /**
     * @return Returns the globalConsent.
     */
    public boolean hasGlobalConsent() {
        return globalConsent;
    }

    /**
     * @param globalConsent The globalConsent to set.
     */
    public void setGlobalConsent(boolean globalConsent) {
        this.globalConsent = globalConsent;
    }

    /**
     * @return Returns the agreedTermsOfUse.
     */
    public Set<AgreedTermsOfUse> getAgreedTermsOfUse() {
        return agreedTermsOfUse;
    }

    /**
     * @param agreedTermsOfUse The agreedTermsOfUse to set.
     */
    public void setAgreedTermsOfUse(Set<AgreedTermsOfUse> agreedTermsOfUse) {
        this.agreedTermsOfUse = agreedTermsOfUse;
    }

    /**
     * @return Returns the attributeReleaseConsents.
     */
    public Set<AttributeReleaseConsent> getAttributeReleaseConsents() {
        return attributeReleaseConsents;
    }

    /**
     * @param attributeReleaseConsents The attributeReleaseConsents to set.
     */
    public void setAttributeReleaseConsents(Set<AttributeReleaseConsent> attributeReleaseConsents) {
        this.attributeReleaseConsents = attributeReleaseConsents;
    }

}
