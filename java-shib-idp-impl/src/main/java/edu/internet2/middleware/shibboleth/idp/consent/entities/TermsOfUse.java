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

/**
 *
 */
public class TermsOfUse {
    private String version;

    private int fingerprint;

    private String text;

    
    public boolean equalsFingerprint(TermsOfUse termsOfUse) {
        return this.equals(termsOfUse) && this.getFingerprint() == termsOfUse.getFingerprint();
    }
    
    /**
     * @return Returns the fingerprint.
     */
    public int getFingerprint() {
        return fingerprint != 0 ? fingerprint : this.hashCode();
    }

    /**
     * @return Returns the text.
     */
    public String getText() {
        return text;
    }

    /**
     * @return Returns the version.
     */
    public String getVersion() {
        return version;
    }

    /**
     * @param fingerprint The fingerprint to set.
     */
    public void setFingerprint(final int fingerprint) {
        this.fingerprint = fingerprint;
    }

    /**
     * @param text The text to set.
     */
    public void setText(final String text) {
        this.text = text;
    }

    /**
     * @param version The version to set.
     */
    public void setVersion(final String version) {
        this.version = version;
    }
    
    /** {@inheritDoc} */
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((version == null) ? 0 : version.hashCode());
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
        TermsOfUse other = (TermsOfUse) obj;
        if (version == null) {
            if (other.version != null)
                return false;
        } else if (!version.equals(other.version))
            return false;
        return true;
    }


    /** {@inheritDoc} */
    @Override
    public String toString() {
        return "TermsOfUse [fingerprint=" + this.getFingerprint() + ", text=" + text + ", version=" + version + "]";
    }

}
