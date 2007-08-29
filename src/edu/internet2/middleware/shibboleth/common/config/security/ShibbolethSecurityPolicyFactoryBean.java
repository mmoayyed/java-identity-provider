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

package edu.internet2.middleware.shibboleth.common.config.security;

import java.util.ArrayList;

import org.opensaml.ws.security.SecurityPolicyRule;
import org.springframework.beans.factory.config.AbstractFactoryBean;

import edu.internet2.middleware.shibboleth.common.security.ShibbolethSecurityPolicy;

/**
 * Spring factory bean for producing {@link ShibbolethSecurityPolicy}s.
 */
public class ShibbolethSecurityPolicyFactoryBean extends AbstractFactoryBean {

    /** Unique ID of the policy. */
    private String policyId;

    /** Rules that make up the policy. */
    private ArrayList<SecurityPolicyRule> policyRules;

    /** {@inheritDoc} */
    public Class getObjectType() {
        return ShibbolethSecurityPolicy.class;
    }

    /**
     * Gets the unique ID of the policy.
     * 
     * @return unique ID of the policy
     */
    public String getPolicyId() {
        return policyId;
    }

    /**
     * Sets the unique ID of the policy.
     * 
     * @param id unique ID of the policy
     */
    public void setPolicyId(String id) {
        policyId = id;
    }

    /**
     * Gets the rules that make up the policy.
     * 
     * @return rules that make up the policy
     */
    public ArrayList<SecurityPolicyRule> getPolicyRules() {
        return policyRules;
    }

    /**
     * Sets the rules that make up the policy.
     * 
     * @param rules rules that make up the policy
     */
    public void setPolicyRules(ArrayList<SecurityPolicyRule> rules) {
        policyRules = rules;
    }

    /** {@inheritDoc} */
    protected Object createInstance() throws Exception {
        ShibbolethSecurityPolicy policy = new ShibbolethSecurityPolicy(getPolicyId());
        if (getPolicyRules() != null) {
            policy.getPolicyRules().addAll(getPolicyRules());
        }

        return policy;
    }
}
