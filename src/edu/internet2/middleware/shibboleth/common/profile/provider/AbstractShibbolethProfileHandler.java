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

package edu.internet2.middleware.shibboleth.common.profile.provider;

import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;

import org.opensaml.Configuration;
import org.opensaml.common.binding.decoding.MessageDecoder;
import org.opensaml.common.binding.encoding.MessageEncoder;
import org.opensaml.common.binding.security.SAMLSecurityPolicy;
import org.opensaml.ws.security.SecurityPolicyFactory;
import org.opensaml.xml.XMLObjectBuilderFactory;
import org.opensaml.xml.security.trust.TrustEngine;

import edu.internet2.middleware.shibboleth.common.profile.ProfileException;
import edu.internet2.middleware.shibboleth.common.profile.ProfileHandler;
import edu.internet2.middleware.shibboleth.common.profile.ProfileRequest;
import edu.internet2.middleware.shibboleth.common.profile.ProfileResponse;
import edu.internet2.middleware.shibboleth.common.relyingparty.ProfileConfiguration;
import edu.internet2.middleware.shibboleth.common.relyingparty.RelyingPartyConfiguration;
import edu.internet2.middleware.shibboleth.common.relyingparty.RelyingPartyConfigurationManager;
import edu.internet2.middleware.shibboleth.common.relyingparty.provider.SAMLMDRelyingPartyConfigurationManager;
import edu.internet2.middleware.shibboleth.common.session.Session;
import edu.internet2.middleware.shibboleth.common.session.SessionManager;

/**
 * A processor for a communication profile supported by Shibboleth.
 * 
 * Profile handlers must be stateless and thread-safe as a single instance may be used to service every incoming
 * request.
 * 
 * @param <RPManagerType> type of relying party configuration manager used by this profile handler
 * @param <SessionType> type of sessions managed by the session manager used by this profile handler
 */
public abstract class AbstractShibbolethProfileHandler<RPManagerType extends SAMLMDRelyingPartyConfigurationManager, 
    SessionType extends Session> extends AbstractRequestURIMappedProfileHandler implements ProfileHandler {

    /** Relying party configuration manager for the profile handler. */
    private RPManagerType rpManager;

    /** Session manager for the profile handler. */
    private SessionManager<SessionType> sessionManager;

    /** Factory used to get security policies to evaluate against a request. */
    private SecurityPolicyFactory<ServletRequest> securityPolicyFactory;

    /** Trust engine used to validate peer credentials. */
    private TrustEngine trustEngine;

    /** For building XML. */
    private XMLObjectBuilderFactory builderFactory;

    /** Constructor. */
    protected AbstractShibbolethProfileHandler() {
        super();
        builderFactory = Configuration.getBuilderFactory();
    }

    /**
     * Gets the ID of the profile supported by this handler.
     * 
     * @return ID of the profile supported by this handler
     */
    public abstract String getProfileId();

    /**
     * Gets the relying party manager for this profile handler.
     * 
     * @return relying party manager for this profile handler
     */
    public RPManagerType getRelyingPartyConfigurationManager() {
        return rpManager;
    }

    /**
     * Sets the relying party manager for this profile handler.
     * 
     * @param manager relying party manager for this profile handler
     */
    public void setRelyingPartyConfigurationManager(RPManagerType manager) {
        rpManager = manager;
    }

    /**
     * Gets the relying party configuration for the given entity. This is only a convenience method and is equivalent to
     * retrieving the relying party configuration by invoking {@link #getRelyingPartyConfigurationManager()} and then
     * invoking {@link RelyingPartyConfigurationManager#getRelyingPartyConfiguration(String)}.
     * 
     * @param relyingPartyId ID of the relying party
     * 
     * @return the relying party configuration or null
     */
    public RelyingPartyConfiguration getRelyingPartyConfiguration(String relyingPartyId) {
        RelyingPartyConfigurationManager rpcManager = getRelyingPartyConfigurationManager();
        if (rpcManager != null) {
            return rpcManager.getRelyingPartyConfiguration(relyingPartyId);
        }

        return null;
    }

    /**
     * Gets the profile configuration for the given entity and profile Id. This is only a convenience method and is
     * equivalent to retrieving the relying party configuration by invoking
     * {@link #getRelyingPartyConfiguration(String)} following by
     * {@link RelyingPartyConfiguration#getProfileConfiguration(String)}
     * 
     * @param relyingPartyId ID of the relying party
     * @param profileId unique ID of the profile
     * 
     * @return the profile configuration or null
     */
    public ProfileConfiguration getProfileConfiguration(String relyingPartyId, String profileId) {
        RelyingPartyConfiguration rpConfig = getRelyingPartyConfiguration(relyingPartyId);
        if (rpConfig != null) {
            return rpConfig.getProfileConfigurations().get(profileId);
        }

        return null;
    }

    /**
     * Gets the session manager for this profile handler.
     * 
     * @return session manager for this profile handler
     */
    public SessionManager<SessionType> getSessionManager() {
        return sessionManager;
    }

    /**
     * Sets the session manager for this profile handler.
     * 
     * @param manager session manager for this profile handler
     */
    public void setSessionManager(SessionManager<SessionType> manager) {
        sessionManager = manager;
    }

    /**
     * Gets the factory used to produce security policies to evaluate requests against.
     * 
     * @return factory used to produce security policies to evaluate requests against
     */
    public SecurityPolicyFactory<ServletRequest> getSecurityPolicyFactory() {
        return securityPolicyFactory;
    }

    /**
     * Sets the factory used to produce security policies to evaluate requests against.
     * 
     * @param factory factory used to produce security policies to evaluate requests against
     */
    public void setSecurityPolicyFactory(SecurityPolicyFactory<ServletRequest> factory) {
        securityPolicyFactory = factory;
    }

    /**
     * Gets the trust engine used to validate peer credentials.
     * 
     * @return trust engine used to validate peer credentials
     */
    public TrustEngine getTrustEngine() {
        return trustEngine;
    }

    /**
     * Sets the trust engine used to validate peer credentials.
     * 
     * @param engine trust engine used to validate peer credentials
     */
    public void setTrustEngine(TrustEngine engine) {
        trustEngine = engine;
    }

    /**
     * Convenience method for getting the XML object builder factory.
     * 
     * @return XML object builder factory
     */
    public XMLObjectBuilderFactory getBuilderFactory() {
        return builderFactory;
    }

    /** {@inheritDoc} */
    public abstract void processRequest(ProfileRequest<ServletRequest> request,
            ProfileResponse<ServletResponse> response) throws ProfileException;

    /**
     * Populates the given message decoder with a security policy instance and the profile handler's trust engine.
     * 
     * @param decoder the message decoder to populate
     */
    @SuppressWarnings("unchecked")
    protected void populateMessageDecoder(MessageDecoder<ServletRequest> decoder) {
        if (securityPolicyFactory != null) {
            SAMLSecurityPolicy<HttpServletRequest> securityPolicy = (SAMLSecurityPolicy) securityPolicyFactory
                    .createPolicyInstance();
            securityPolicy.setMetadataProvider(rpManager.getMetadataProvider());
            decoder.setSecurityPolicy(securityPolicy);
        }

        decoder.setTrustEngine(trustEngine);
    }

    /**
     * Currently this method does not effect the message encoder but it serves as a future extension point.
     * 
     * @param encoder the message encoder to populate
     */
    protected void populateMessageEncoder(MessageEncoder<ServletResponse> encoder) {
        encoder.setMetadataProvider(rpManager.getMetadataProvider());
    }
}