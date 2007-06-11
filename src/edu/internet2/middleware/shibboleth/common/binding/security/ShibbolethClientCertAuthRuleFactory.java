/*
 * Copyright [2007] [University Corporation for Advanced Internet Development, Inc.]
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

package edu.internet2.middleware.shibboleth.common.binding.security;

import java.util.Set;

import javax.servlet.ServletRequest;

import org.opensaml.common.binding.security.SAMLMDClientCertAuthRuleFactory;
import org.opensaml.ws.security.SecurityPolicyRule;
import org.opensaml.xml.security.trust.TrustEngine;
import org.opensaml.xml.security.x509.X500DNHandler;
import org.opensaml.xml.security.x509.X509Credential;
import org.opensaml.xml.security.x509.X509Util;

/**
 * Specialization of {@link SAMLMDClientCertAuthRuleFactory} which includes Shibboleth-specific
 * defaults for client certificate authentication processing options, such as deriving
 * message issuer entityID's from data held withing the client certificate.
 */
public class ShibbolethClientCertAuthRuleFactory extends SAMLMDClientCertAuthRuleFactory {
    
    /**
     * Constructor.
     */
    public ShibbolethClientCertAuthRuleFactory() {
        super();
        // This is the behavior used by the Shibboleth 1.3 IdP
        setX500SubjectDNFormat(X500DNHandler.FORMAT_RFC2253);
        setEvaluateSubjectDN(true);
        setEvaluateSubjectCommonName(true);
        Set<Integer> altNameTypes = getSubjectAltNames();
        altNameTypes.add(X509Util.DNS_ALT_NAME);
        altNameTypes.add(X509Util.URI_ALT_NAME);
    }

    /** {@inheritDoc} */
    public SecurityPolicyRule<ServletRequest> createRuleInstance() {
        return new ShibbolethClientCertAuthRule(getTrustEngine(), getCertificateNameOptions());
    }

    /**
     * Specialization of {@link SAMLMDClientCertAuthRule} which may include Shibboleth-specific
     * method overrides for client certificate authentication processing.
     */
    protected class ShibbolethClientCertAuthRule extends SAMLMDClientCertAuthRule {

        /**
         * Constructor.
         *
         * @param engine Trust engine used to verify the request X509Credential
         * @param nameOptions options for deriving issuer names from an X.509 certificate
         */
        public ShibbolethClientCertAuthRule(TrustEngine<X509Credential> engine, CertificateNameOptions nameOptions) {
            super(engine, nameOptions);
        }
        
    }
    
}
