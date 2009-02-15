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

package edu.internet2.middleware.shibboleth.common.relyingparty;

import java.util.Collections;

import org.opensaml.ws.message.MessageContext;
import org.opensaml.ws.security.SecurityPolicy;
import org.opensaml.ws.security.SecurityPolicyResolver;
import org.opensaml.xml.security.SecurityException;
import org.opensaml.xml.util.DatatypeHelper;

/**
 * A security policy resolver that selects the active security policy based on the inbound message issuer ID and the
 * communication profile used.
 */
public class RelyingPartySecurityPolicyResolver implements SecurityPolicyResolver {

    /** Relying party configuration manager. */
    private RelyingPartyConfigurationManager rpConfigManager;

    /**
     * Constructor.
     * 
     * @param configManager configuration manager used to resolve relying party specific configuration information, may
     *            not be null
     */
    public RelyingPartySecurityPolicyResolver(RelyingPartyConfigurationManager configManager) {
        if (configManager == null) {
            throw new IllegalArgumentException("Relying party configuraiton manager may not be null");
        }
        
        rpConfigManager = configManager;
    }

    /** {@inheritDoc} */
    public Iterable<SecurityPolicy> resolve(MessageContext messageContext) throws SecurityException {
        return Collections.singletonList(resolveSingle(messageContext));
    }

    /** {@inheritDoc} */
    public SecurityPolicy resolveSingle(MessageContext messageContext) throws SecurityException {
        String peerEntityId = messageContext.getInboundMessageIssuer();
        if (DatatypeHelper.isEmpty(peerEntityId)) {
            throw new SecurityException(
                    "Unable to select security policy, ID of the peer unknown.");
        }

        RelyingPartyConfiguration rpConfig = rpConfigManager.getRelyingPartyConfiguration(peerEntityId);
        if (rpConfig == null) {
            return null;
        }

        String profileId = messageContext.getCommunicationProfileId();
        if (DatatypeHelper.isEmpty(profileId)) {
            throw new SecurityException(
                    "Unable to select security policy, communication profile ID unknown.");
        }

        ProfileConfiguration profileConfig = rpConfig.getProfileConfiguration(profileId);
        if (profileConfig == null) {
            return null;
        }

        return profileConfig.getSecurityPolicy();
    }
}