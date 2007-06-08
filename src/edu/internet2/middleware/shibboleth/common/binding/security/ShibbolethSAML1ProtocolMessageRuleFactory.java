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

import javax.servlet.ServletRequest;

import org.apache.log4j.Logger;
import org.opensaml.common.binding.security.SAMLSecurityPolicyContext;
import org.opensaml.saml1.binding.security.SAML1ProtocolMessageRuleFactory;
import org.opensaml.saml1.core.AttributeQuery;
import org.opensaml.saml1.core.RequestAbstractType;
import org.opensaml.ws.security.SecurityPolicyRule;


/**
 * Specialization of {@link SAML1ProtocolMessageRuleFactory} which produces rules specific to
 * handling Shibboleth SAML 1.x profile requirements.
 */
public class ShibbolethSAML1ProtocolMessageRuleFactory extends SAML1ProtocolMessageRuleFactory {
    
    /** {@inheritDoc} */
    public SecurityPolicyRule<ServletRequest> createRuleInstance() {
        return new ShibbolethSAML1ProtocolMessageRule();
    }

    /**
     * Specialization of {@link SAML1ProtocolMessageRule} which handles
     * Shibboleth SAML 1.x profile requirements.
     */
    public class ShibbolethSAML1ProtocolMessageRule extends SAML1ProtocolMessageRule {

        /** {@inheritDoc} */
        protected void extractRequestInfo(SAMLSecurityPolicyContext samlContext, RequestAbstractType request) {
            super.extractRequestInfo(samlContext, request);
            
            Logger log = Logger.getLogger(ShibbolethSAML1ProtocolMessageRule.class);
            
            if (request instanceof AttributeQuery) {
                log.info("Attempting to extract issuer from enclosed SAML 1.x AttributeQuery Resource attribute");
                AttributeQuery query = (AttributeQuery) request;
                if (query.getResource() != null) {
                    samlContext.setIssuer(query.getResource());
                }
            }
        }
        
    }

}
