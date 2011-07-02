/*
 * Licensed to the University Corporation for Advanced Internet Development, 
 * Inc. (UCAID) under one or more contributor license agreements.  See the 
 * NOTICE file distributed with this work for additional information regarding
 * copyright ownership. The UCAID licenses this file to You under the Apache 
 * License, Version 2.0 (the "License"); you may not use this file except in 
 * compliance with the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package net.shibboleth.idp.saml.relyingparty.saml2;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import net.shibboleth.idp.profile.ProfileRequestContext;
import net.shibboleth.idp.saml.relyingparty.AbstractSAMLProfileConfiguration;

import org.opensaml.util.Assert;
import org.opensaml.util.StringSupport;
import org.opensaml.xml.security.EvaluableCriteria;
import org.opensaml.xml.security.StaticResponseEvaluableCritieria;

/** Base class for SAML 2 profile configurations. */
public abstract class AbstractSAML2ProfileConfiguration extends AbstractSAMLProfileConfiguration {

    /** Criteria used to determine name identifiers should be encrypted. */
    private EvaluableCriteria<ProfileRequestContext> encryptNameIDsCriteria;

    /** Criteria used to determine assertions should be encrypted. */
    private EvaluableCriteria<ProfileRequestContext> encryptAssertionsCriteria;

    /** Maximum proxy count for an assertion. Default value: 0 */
    private int proxyCount;

    /** Audiences for the proxy. */
    private Set<String> proxyAudiences;

    /**
     * Constructor.
     * 
     * @param profileId ID of the the communication profile, never null or empty
     */
    public AbstractSAML2ProfileConfiguration(String profileId) {
        super(profileId);
        encryptNameIDsCriteria = StaticResponseEvaluableCritieria.FALSE_RESPONSE;
        encryptAssertionsCriteria = StaticResponseEvaluableCritieria.TRUE_RESPONSE;
        proxyCount = 0;
        proxyAudiences = Collections.emptySet();
    }

    /**
     * Gets the maximum number of times an assertion may be proxied.
     * 
     * @return maximum number of times an assertion may be proxied
     */
    public int getProxyCount() {
        return proxyCount;
    }

    /**
     * Gets the maximum number of times an assertion may be proxied.
     * 
     * @param count maximum number of times an assertion may be proxied
     */
    public void setProxyCount(int count) {
        proxyCount = count;
    }

    /**
     * Gets the unmodified collection of audiences for a proxied assertion.
     * 
     * @return audiences for a proxied assertion, never null nor containing null entries
     */
    public Collection<String> getProxyAudiences() {
        return proxyAudiences;
    }

    /**
     * Gets the criteria used to determine name identifiers should be encrypted.
     * 
     * @return criteria used to determine name identifiers should be encrypted, never null
     */
    public EvaluableCriteria<ProfileRequestContext> getEncryptNameIDsCriteria() {
        return encryptNameIDsCriteria;
    }

    /**
     * Sets the criteria used to determine name identifiers should be encrypted.
     * 
     * @param criteria criteria used to determine name identifiers should be encrypted, never null
     */
    public void setEncryptNameIDsCriteria(EvaluableCriteria<ProfileRequestContext> criteria) {
        Assert.isNotNull(criteria, "Criteria to determine if name identifiers should be encrypted can not be null");
        encryptNameIDsCriteria = criteria;
    }

    /**
     * Gets the criteria used to determine assertions should be encrypted.
     * 
     * @return criteria used to determine assertions should be encrypted, never null
     */
    public EvaluableCriteria<ProfileRequestContext> getEncryptAssertionsCriteria() {
        return encryptAssertionsCriteria;
    }

    /**
     * Sets the criteria used to determine assertions should be encrypted.
     * 
     * @param criteria criteria used to determine assertions should be encrypted, never null
     */
    public void setEncryptAssertionsCriteria(EvaluableCriteria<ProfileRequestContext> criteria) {
        Assert.isNotNull(criteria, "Criteria to determine if assertions should be enecrypted can not be null");
        encryptAssertionsCriteria = criteria;
    }

    /**
     * Sets the proxy audiences to be added to responses.
     * 
     * @param audiences proxy audiences to be added to responses, may be null or contain null elements
     */
    public void setProxyAudiences(Collection<String> audiences) {
        if (audiences == null || audiences.isEmpty()) {
            proxyAudiences = Collections.emptySet();
            return;
        }

        HashSet<String> newAudiences = new HashSet<String>();
        String trimmedAudience;
        for (String audience : audiences) {
            trimmedAudience = StringSupport.trimOrNull(audience);
            if (trimmedAudience != null) {
                newAudiences.add(trimmedAudience);
            }
        }

        if (newAudiences.isEmpty()) {
            proxyAudiences = Collections.emptySet();
        } else {
            proxyAudiences = Collections.unmodifiableSet(newAudiences);
        }
    }
}