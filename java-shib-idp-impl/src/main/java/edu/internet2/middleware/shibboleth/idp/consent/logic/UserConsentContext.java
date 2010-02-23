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

package edu.internet2.middleware.shibboleth.idp.consent.logic;

import java.util.Collection;
import java.util.Date;

import edu.internet2.middleware.shibboleth.idp.consent.entities.Attribute;
import edu.internet2.middleware.shibboleth.idp.consent.entities.Principal;
import edu.internet2.middleware.shibboleth.idp.consent.entities.RelyingParty;

/**
 *
 */
public class UserConsentContext {
    
    // TODO
    // Session?
    
    private Principal principal;

    private RelyingParty relyingParty;

    private Collection<Attribute> attributesToBeReleased;

    private Date accessTime;
    
    private boolean consentRevocationRequested;

    /**
     * @return Returns the accessTime.
     */
    public Date getAccessTime() {
        return accessTime;
    }

    /**
     * @return Returns the attributesToBeReleased.
     */
    public Collection<Attribute> getAttributesToBeReleased() {
        return attributesToBeReleased;
    }

    /**
     * @return Returns the principal.
     */
    public Principal getPrincipal() {
        return principal;
    }

    /**
     * @return Returns the relyingParty.
     */
    public RelyingParty getRelyingParty() {
        return relyingParty;
    }

    /**
     * @param accessTime The accessTime to set.
     */
    public void setAccessTime(final Date accessTime) {
        this.accessTime = accessTime;
    }

    /**
     * @param attributesToBeReleased The attributesToBeReleased to set.
     */
    public void setAttributesToBeReleased(final Collection<Attribute> attributesToBeReleased) {
        this.attributesToBeReleased = attributesToBeReleased;
    }

    /**
     * @param principal The principal to set.
     */
    public void setPrincipal(final Principal principal) {
        this.principal = principal;
    }
    /** {@inheritDoc} */


    /**
     * @param relyingParty The relyingParty to set.
     */
    public void setRelyingParty(final RelyingParty relyingParty) {
        this.relyingParty = relyingParty;
    }
    
    public String toString() {
        return "UserConsentContext [principal=" + principal + ", relyingParty=" + relyingParty + ", accessTime="
                + accessTime + ", attributesToBeReleased=" + attributesToBeReleased + "]";
    }

    /**
     * @param consentRevocationRequested The consentRevocationRequested to set.
     */
    public void setConsentRevocationRequested(boolean consentRevocationRequested) {
        this.consentRevocationRequested = consentRevocationRequested;
    }

    /**
     * @return Returns the consentRevocationRequested.
     */
    public boolean isConsentRevocationRequested() {
        return consentRevocationRequested;
    }
}
