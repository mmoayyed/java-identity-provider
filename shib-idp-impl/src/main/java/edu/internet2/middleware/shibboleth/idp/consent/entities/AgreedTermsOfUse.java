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

import org.joda.time.DateTime;

import edu.internet2.middleware.shibboleth.idp.consent.components.TermsOfUse;

/**
 *
 */
public class AgreedTermsOfUse {
    final TermsOfUse termsOfUse;

    final DateTime agreeDate;
    
    public AgreedTermsOfUse(final TermsOfUse termsOfUse, final DateTime agreeDate) {
    	this.termsOfUse = termsOfUse;
    	this.agreeDate = agreeDate;
    }

    /**
     * @return Returns the agreeDate.
     */
    public DateTime getAgreeDate() {
        return agreeDate;
    }

    /**
     * @return Returns the TermsOfUse.
     */
    public TermsOfUse getTermsOfUse() {
        return termsOfUse;
    }


	/** {@inheritDoc} */
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((agreeDate == null) ? 0 : agreeDate.hashCode());
        result = prime * result + ((termsOfUse == null) ? 0 : termsOfUse.hashCode());
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
        AgreedTermsOfUse other = (AgreedTermsOfUse) obj;
        if (agreeDate == null) {
            if (other.agreeDate != null)
                return false;
        } else if (!agreeDate.equals(other.agreeDate))
            return false;
        if (termsOfUse == null) {
            if (other.termsOfUse != null)
                return false;
        } else if (!termsOfUse.equals(other.termsOfUse))
            return false;
        return true;
    }
    
}
