package edu.internet2.middleware.shibboleth.common.binding.security;

import org.opensaml.common.binding.security.SAMLMDClientCertAuthRule;
import org.opensaml.xml.security.trust.TrustEngine;
import org.opensaml.xml.security.x509.X509Credential;

/**
 * Specialization of {@link SAMLMDClientCertAuthRule} which may include Shibboleth-specific
 * method overrides for client certificate authentication processing.
 */
class ShibbolethClientCertAuthRule extends SAMLMDClientCertAuthRule {

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