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

    private Attribute attribute;

    private Date releaseDate;

    /**
     * @return Returns the attribute.
     */
    public Attribute getAttribute() {
        return attribute;
    }

    /**
     * @return Returns the releaseDate.
     */
    public Date getReleaseDate() {
        return releaseDate;
    }

    /**
     * @param attribute The attribute to set.
     */
    public void setAttribute(final Attribute attribute) {
        this.attribute = attribute;
    }

    /**
     * @param releaseDate The releaseDate to set.
     */
    public void setReleaseDate(final Date releaseDate) {
        this.releaseDate = releaseDate;
    }

    /** {@inheritDoc} */
    public String toString() {
        return "AttributeReleaseConsent [attribute=" + attribute + ", releaseDate=" + releaseDate + "]";
    }

    /** {@inheritDoc} */
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((attribute == null) ? 0 : attribute.hashCode());
        result = prime * result + ((releaseDate == null) ? 0 : releaseDate.hashCode());
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
        AttributeReleaseConsent other = (AttributeReleaseConsent) obj;
        if (attribute == null) {
            if (other.attribute != null)
                return false;
        } else if (!attribute.equals(other.attribute))
            return false;
        if (releaseDate == null) {
            if (other.releaseDate != null)
                return false;
        } else if (!releaseDate.equals(other.releaseDate))
            return false;
        return true;
    }

}
