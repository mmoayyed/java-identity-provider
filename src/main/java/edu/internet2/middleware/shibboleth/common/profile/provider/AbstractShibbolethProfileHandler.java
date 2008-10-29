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

import org.opensaml.Configuration;
import org.opensaml.ws.transport.http.HTTPInTransport;
import org.opensaml.ws.transport.http.HTTPOutTransport;
import org.opensaml.xml.XMLObjectBuilderFactory;
import org.opensaml.xml.parse.ParserPool;

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
public abstract class AbstractShibbolethProfileHandler<RPManagerType extends SAMLMDRelyingPartyConfigurationManager, SessionType extends Session>
        extends AbstractRequestURIMappedProfileHandler<HTTPInTransport, HTTPOutTransport> {

    /** Pool of XML parsers. */
    private ParserPool parserPool;
    
    /** Relying party configuration manager for the profile handler. */
    private RPManagerType rpManager;

    /** Session manager for the profile handler. */
    private SessionManager<SessionType> sessionManager;

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
     * Gets the pool of XML parsers.
     * 
     * @return pool of XML parsers.
     */
    public ParserPool getParserPool() {
        return parserPool;
    }
    
    /**
     * Sets the pool of XML parsers.
     * 
     * @param pool pool of XML parsers
     */
    public void setParserPool(ParserPool pool) {
        parserPool = pool;
    }

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
     * Convenience method for getting the XML object builder factory.
     * 
     * @return XML object builder factory
     */
    public XMLObjectBuilderFactory getBuilderFactory() {
        return builderFactory;
    }
}