
package edu.internet2.middleware.shibboleth.common.binding.security;

import org.apache.log4j.Logger;
import org.opensaml.common.binding.SAMLMessageContext;
import org.opensaml.saml1.binding.security.SAML1ProtocolMessageRule;
import org.opensaml.saml1.core.Request;
import org.opensaml.saml1.core.RequestAbstractType;
import org.opensaml.xml.util.DatatypeHelper;

/**
 * Specialization of {@link SAML1ProtocolMessageRule} which handles Shibboleth SAML 1.x profile requirements.
 */
public class ShibbolethSAML1ProtocolMessageRule extends SAML1ProtocolMessageRule {

    /** Class Logger. */
    private final Logger log = Logger.getLogger(ShibbolethSAML1ProtocolMessageRule.class);

    /** {@inheritDoc} */
    protected void extractRequestInfo(SAMLMessageContext messageContext, RequestAbstractType abstractRequest) {
        super.extractRequestInfo(messageContext, abstractRequest);

        if (abstractRequest instanceof Request) {
            Request request = (Request) abstractRequest;

            if (request.getAttributeQuery() != null) {
                if (log.isDebugEnabled()) {
                    log.debug("Attempting to extract issuer from enclosed SAML 1.x AttributeQuery Resource attribute");
                }
                String resource = DatatypeHelper.safeTrimOrNullString(request.getAttributeQuery().getResource());

                if (resource != null) {
                    messageContext.setRelyingPartyEntityId(resource);
                    if (log.isDebugEnabled()) {
                        log.debug("Extracted issuer from SAML 1.x AttributeQuery Resource: " + resource);
                    }
                } else {
                    log.warn("SAML 1.x AttributeQuery Resource contained no value, "
                            + "unable to extract issuer per Shibboleth profile");
                }
            }

        }
    }

}