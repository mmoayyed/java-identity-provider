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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.Resource;
import edu.internet2.middleware.shibboleth.idp.consent.UserConsentException;
import edu.vt.middleware.crypt.digest.SHA256;
import edu.vt.middleware.crypt.util.HexConverter;

/**
 *
 */
public class TermsOfUse {

    private final Logger logger = LoggerFactory.getLogger(DescriptionBuilder.class);
    
    private String version;

    private String fingerprint;

    private String text;
    
    private Resource resource;
    
    public TermsOfUse() {}
   
    public void initialize() throws UserConsentException {
        if (resource == null || version == null) {
            throw new UserConsentException("Resource and/or version are not set");
        }
               
        StringBuilder stringBuilder = new StringBuilder();
        try {
            BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(resource.getInputStream()));       
            String line = null;
            while ((line = bufferedReader.readLine()) != null) {
                stringBuilder.append(line + "\n");
            }
            bufferedReader.close();
        } catch (IOException e) {
            throw new UserConsentException("Error while initializing terms of use", e);
        }
        
        this.text = stringBuilder.toString();
        String fingerprintInput = version+"|"+text;
        this.fingerprint = new SHA256().digest(fingerprintInput.getBytes(), new HexConverter(true));
    }
    
    public final void setVersion(final String version) {
        this.version = version;
    }
    
    public final void setResource(final Resource resource) {
        this.resource = resource;
    }
        
    public TermsOfUse(final String version, final String fingerprint) {
    	this.version = version;
    	this.fingerprint = fingerprint;
    	text = null;
    }
        
    /**
     * @return Returns the text.
     */
    public final String getText() {
        return text;
    }

    /**
     * @return Returns the version.
     */
    public final String getVersion() {
        return version;
    }
    
    /**
     * @return Returns the fingerprint.
     */
    public final String getFingerprint() {
        return fingerprint;
    }
    
    public boolean equalsFingerprint(TermsOfUse termsOfUse) {
        return equals(termsOfUse) && fingerprint.equals(termsOfUse.fingerprint);
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
    public String toString() {
        return "TermsOfUse [version=" + version + "]";
    }
    


}
