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

package edu.internet2.middleware.shibboleth.idp.tou;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.Resource;

import edu.vt.middleware.crypt.digest.SHA256;
import edu.vt.middleware.crypt.util.HexConverter;

/**
 *
 */
public class ToU {
    
    private final Logger logger = LoggerFactory.getLogger(ToU.class);
    
    private final String version;

    private final String fingerprint;

    private final String text;
        
    public ToU(String version, String fingerprint) {
        this.version = version;
        this.fingerprint = fingerprint;
        this.text = null;
    }
    
    public ToU(String version, Resource resource) throws TermsOfUseException {
        this.version = version;
        
        StringBuilder stringBuilder = new StringBuilder();
        try {
            BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(resource.getInputStream()));       
            String line = null;
            while ((line = bufferedReader.readLine()) != null) {
                stringBuilder.append(line + "\n");
            }
            bufferedReader.close();
        } catch (IOException e) {
            throw new TermsOfUseException("Error while initializing terms of use", e);
        }  
        this.text = stringBuilder.toString();        
        this.fingerprint = new SHA256().digest(this.text.getBytes(), new HexConverter(true));
        logger.info("ToU version {} initialized from file {}", version, resource);
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
    
    /** {@inheritDoc} */
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((fingerprint == null) ? 0 : fingerprint.hashCode());
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
        ToU other = (ToU) obj;
        if (fingerprint == null) {
            if (other.fingerprint != null)
                return false;
        } else if (!fingerprint.equals(other.fingerprint))
            return false;
        if (version == null) {
            if (other.version != null)
                return false;
        } else if (!version.equals(other.version))
            return false;
        return true;
    }

    /** {@inheritDoc} */
    public String toString() {
        return "ToU [version=" + version + "]";
    }
}
