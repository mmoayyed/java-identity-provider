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

    /** {@inheritDoc} */
    @Override
    public boolean equals(Object object) {
        if (object instanceof TermsOfUse) {
            return this.version.equals(((TermsOfUse) object).getVersion())
                    && this.getFingerprint() == ((TermsOfUse) object).getFingerprint();
        }
        return false;
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
    @Override
    public String toString() {
        return "TermsOfUse [fingerprint=" + this.getFingerprint() + ", text=" + text + ", version=" + version + "]";
    }

}
