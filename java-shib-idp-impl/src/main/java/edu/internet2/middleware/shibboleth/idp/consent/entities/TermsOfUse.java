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

    private String fingerprint;

    private String text;

    /**
     * @return Returns the version.
     */
    public String getVersion() {
        return version;
    }

    /**
     * @param version The version to set.
     */
    public void setVersion(String version) {
        this.version = version;
    }

    /**
     * @return Returns the fingerprint.
     */
    public String getFingerprint() {
        return fingerprint;
    }

    /**
     * @param fingerprint The fingerprint to set.
     */
    public void setFingerprint(String fingerprint) {
        this.fingerprint = fingerprint;
    }

    /**
     * @return Returns the text.
     */
    public String getText() {
        return text;
    }

    /**
     * @param text The text to set.
     */
    public void setText(String text) {
        this.text = text;
    }

    /** {@inheritDoc} */
    public boolean equals(Object object) {
        if (object instanceof TermsOfUse) {
            return this.version == ((TermsOfUse) object).version
                    && this.fingerprint == ((TermsOfUse) object).fingerprint;
        }
        return false;
    }

    /** {@inheritDoc} */
    public String toString() {
        return "TermsOfUse [fingerprint=" + fingerprint + ", text=" + text + ", version=" + version + "]";
    }

}
