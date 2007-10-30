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

import java.util.ArrayList;

import org.opensaml.ws.security.SecurityPolicy;
import org.opensaml.ws.security.SecurityPolicyResolver;
import org.opensaml.xml.security.CriteriaSet;
import org.opensaml.xml.security.SecurityException;
import org.opensaml.xml.security.criteria.PeerEntityIDCriteria;

import edu.internet2.middleware.shibboleth.common.security.CommunicationProfileCriteria;

/**
 * A security policy resolver that selects the active security policy based on {@link PeerEntityIDCriteria} and
 * {@link CommunicationProfileCriteria}.
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
    }

    /** {@inheritDoc} */
    public Iterable<SecurityPolicy> resolve(CriteriaSet criteria) throws SecurityException {
        ArrayList<SecurityPolicy> policies = new ArrayList<SecurityPolicy>();
        policies.add(resolveSingle(criteria));
        return policies;
    }

    /** {@inheritDoc} */
    public SecurityPolicy resolveSingle(CriteriaSet criteria) throws SecurityException {
        PeerEntityIDCriteria peerCriteria = criteria.get(PeerEntityIDCriteria.class);
        if (peerCriteria == null) {
            throw new SecurityException(
                    "Unable to select security policy, no communication profile criteria available.");
        }

        RelyingPartyConfiguration rpConfig = rpConfigManager.getRelyingPartyConfiguration(peerCriteria.getPeerID());
        if (rpConfig == null) {
            return null;
        }

        CommunicationProfileCriteria profileCriteria = criteria.get(CommunicationProfileCriteria.class);
        if (profileCriteria == null) {
            throw new SecurityException(
                    "Unable to select security policy, no communication profile criteria available.");
        }

        ProfileConfiguration profileConfig = rpConfig
                .getProfileConfiguration(profileCriteria.getCommunicationProfile());
        if (profileConfig == null) {
            return null;
        }

        return profileConfig.getSecurityPolicy();
    }
}