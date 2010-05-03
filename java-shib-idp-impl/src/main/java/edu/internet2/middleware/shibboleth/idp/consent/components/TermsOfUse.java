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

package edu.internet2.middleware.shibboleth.idp.consent.components;

/**
 *
 */
public class TermsOfUse {
    final private String version;

    final private int fingerprint;

    final private String text;
    
    public TermsOfUse(final String version, final String text) {
    	this.version = version;
    	this.text = text;
    	this.fingerprint = hashCode();
    }
    
    public TermsOfUse(final String version, final int fingerprint) {
    	this.version = version;
    	this.fingerprint = fingerprint;
    	text = null;
    }
        
    /**
     * @return Returns the fingerprint.
     */
    public int getFingerprint() {
        return fingerprint;
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
    
    /** {@inheritDoc} */
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((text == null) ? 0 : text.hashCode());
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
		if (fingerprint != other.fingerprint)
			return false;
		return true;
	}

	/** {@inheritDoc} */
    @Override
    public String toString() {
        return "TermsOfUse [version=" + version + "]";
    }

}
