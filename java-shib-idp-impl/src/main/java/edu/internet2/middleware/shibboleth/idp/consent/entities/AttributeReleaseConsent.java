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

/**
 *
 */
public class AttributeReleaseConsent {
    private RelyingParty relyingParty;

    private Attribute attribute;

    private Date releaseDate;

    /**
     * @return Returns the attribute.
     */
    public Attribute getAttribute() {
        return attribute;
    }

    /**
     * @return Returns the relyingParty.
     */
    public RelyingParty getRelyingParty() {
        return relyingParty;
    }

    /**
     * @param relyingParty The relyingParty to set.
     */
    public void setRelyingParty(RelyingParty relyingParty) {
        this.relyingParty = relyingParty;
    }

    /**
     * @param attribute The attribute to set.
     */
    public void setAttribute(Attribute attribute) {
        this.attribute = attribute;
    }

    /**
     * @return Returns the releaseDate.
     */
    public Date getReleaseDate() {
        return releaseDate;
    }

    /**
     * @param releaseDate The releaseDate to set.
     */
    public void setReleaseDate(Date releaseDate) {
        this.releaseDate = releaseDate;
    }
}
