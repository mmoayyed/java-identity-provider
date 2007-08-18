package edu.internet2.middleware.shibboleth.common.binding.security;

import java.util.Set;

import org.opensaml.common.binding.security.SAMLMDClientCertAuthRule;
import org.opensaml.ws.security.provider.CertificateNameOptions;
import org.opensaml.xml.security.trust.TrustEngine;
import org.opensaml.xml.security.x509.X500DNHandler;
import org.opensaml.xml.security.x509.X509Credential;
import org.opensaml.xml.security.x509.X509Util;

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
    
    /**
     * Constructor.  The certificate name issuer derivation options are defaulted
     * to be consistent with the Shibboleth 1.3 identity provider.
     *
     * @param engine Trust engine used to verify the request X509Credential
     */
    public ShibbolethClientCertAuthRule(TrustEngine<X509Credential> engine) {
        super(engine, new CertificateNameOptions());
        
        CertificateNameOptions nameOptions = getCertificateNameOptions();
        
        // This is the behavior used by the Shibboleth 1.3 IdP.
        nameOptions.setX500SubjectDNFormat(X500DNHandler.FORMAT_RFC2253);
        nameOptions.setEvaluateSubjectDN(true);
        nameOptions.setEvaluateSubjectCommonName(true);
        Set<Integer> altNameTypes = nameOptions.getSubjectAltNames();
        altNameTypes.add(X509Util.DNS_ALT_NAME);
        altNameTypes.add(X509Util.URI_ALT_NAME);
    }
    
}