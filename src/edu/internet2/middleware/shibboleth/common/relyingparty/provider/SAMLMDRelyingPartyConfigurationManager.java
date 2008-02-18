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

package edu.internet2.middleware.shibboleth.common.relyingparty.provider;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.locks.Lock;

import org.opensaml.saml2.metadata.EntitiesDescriptor;
import org.opensaml.saml2.metadata.EntityDescriptor;
import org.opensaml.saml2.metadata.provider.MetadataProvider;
import org.opensaml.saml2.metadata.provider.MetadataProviderException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;

import edu.internet2.middleware.shibboleth.common.config.BaseReloadableService;
import edu.internet2.middleware.shibboleth.common.config.relyingparty.RelyingPartyGroup;
import edu.internet2.middleware.shibboleth.common.relyingparty.RelyingPartyConfiguration;
import edu.internet2.middleware.shibboleth.common.relyingparty.RelyingPartyConfigurationManager;
import edu.internet2.middleware.shibboleth.common.service.ServiceException;

/**
 * A relying party manager that uses SAML metadata to lookup information about requested entities. Relying party
 * configuration information is looked up as follows:
 * 
 * If the given entity ID is null, empty, or contains only whitespace the anonymous relying party configuration is
 * returned. Otherwise, the given relying party entity ID is looked for in the list of registered
 * {@link RelyingPartyConfiguration}s and if found is returned. If no configuration is registered for the specific
 * entity ID the entity descriptor for the relying party is located using the {@link MetadataProvider}. The name of
 * ancestral entities descriptors are then looked up, in ascending order (i.e. the parent entities descriptor, then the
 * grandparent, great-grandparent, etc.), with the first configuration found being returned. If no configuration is
 * found once the top of the tree is reached the default configuration is returned.
 */
public class SAMLMDRelyingPartyConfigurationManager extends BaseReloadableService implements
        RelyingPartyConfigurationManager {

    /** ID used for anonymous relying party. */
    public static final String ANONYMOUS_RP_NAME = "anonymous";

    /** ID used for default relying party. */
    public static final String DEFAULT_RP_NAME = "default";

    /** Class logger. */
    private final Logger log = LoggerFactory.getLogger(SAMLMDRelyingPartyConfigurationManager.class);

    /** Metadata provider used to lookup information about entities. */
    private MetadataProvider metadataProvider;

    /** Regisered relying party configurations. */
    private HashMap<String, RelyingPartyConfiguration> rpConfigs;

    /** Constructor. */
    public SAMLMDRelyingPartyConfigurationManager() {
        super();
        rpConfigs = new HashMap<String, RelyingPartyConfiguration>();
    }

    /** {@inheritDoc} */
    public RelyingPartyConfiguration getAnonymousRelyingConfiguration() {
        return rpConfigs.get(ANONYMOUS_RP_NAME);
    }

    /** {@inheritDoc} */
    public RelyingPartyConfiguration getDefaultRelyingPartyConfiguration() {
        return rpConfigs.get(DEFAULT_RP_NAME);
    }

    /**
     * Gets the metadata provider used to lookup information about entities.
     * 
     * @return metadata provider used to lookup information about entities
     */
    public MetadataProvider getMetadataProvider() {
        return metadataProvider;
    }

    /**
     * Sets the metadata provider used to lookup information about entities.
     * 
     * @param provider metadata provider used to lookup information about entities
     */
    public void setMetadataProvider(MetadataProvider provider) {
        metadataProvider = provider;
    }

    /** {@inheritDoc} */
    public RelyingPartyConfiguration getRelyingPartyConfiguration(String relyingPartyEntityID) {
        Lock readLock = getReadWriteLock().readLock();
        readLock.lock();

        log.debug("Looking up relying party configuration for {}", relyingPartyEntityID);
        if (rpConfigs.containsKey(relyingPartyEntityID)) {
            log.debug("Relying party configuration found for {}", relyingPartyEntityID);
            readLock.unlock();
            return rpConfigs.get(relyingPartyEntityID);
        }

        log.debug("No configuration was registered for {}, looking up configuration based on metadata groups",
                relyingPartyEntityID);
        try {
            if (metadataProvider == null) {
                log.debug("No metadata provider available, unable to lookup configuration based on entity group");
            } else {
                EntityDescriptor entityDescriptor = metadataProvider.getEntityDescriptor(relyingPartyEntityID);
                if (entityDescriptor != null) {
                    EntitiesDescriptor entityGroup = (EntitiesDescriptor) entityDescriptor.getParent();
                    while (entityGroup != null) {
                        if (rpConfigs.containsKey(entityGroup.getName())) {
                            log.debug("Relying party configuration found for {} as member of metadata group {}",
                                    relyingPartyEntityID, entityGroup.getName());
                            readLock.unlock();
                            return rpConfigs.get(entityGroup.getName());
                        }
                        entityGroup = (EntitiesDescriptor) entityGroup.getParent();
                    }
                }
            }
        } catch (MetadataProviderException e) {
            log.error("Error fetching metadata for relying party " + relyingPartyEntityID, e);
        }

        log.debug("No relying party configuration found for {} using default configuration", relyingPartyEntityID);
        readLock.unlock();

        return getDefaultRelyingPartyConfiguration();
    }

    /** {@inheritDoc} */
    public Map<String, RelyingPartyConfiguration> getRelyingPartyConfigurations() {
        return rpConfigs;
    }

    /** {@inheritDoc} */
    protected void onNewContextCreated(ApplicationContext newServiceContext) throws ServiceException {
        String[] relyingPartyGroupNames = newServiceContext.getBeanNamesForType(RelyingPartyGroup.class);
        if (relyingPartyGroupNames == null || relyingPartyGroupNames.length == 0) {
            log.error("No relying party group definition loaded, retaining existng configuration");
            log.debug("Configuration reources: {}", getServiceConfigurations());
            log.debug("Initialized: {}", isInitialized());
            throw new ServiceException("No relying party group definition loaded");
        }

        RelyingPartyGroup rpGroup = (RelyingPartyGroup) newServiceContext.getBean(relyingPartyGroupNames[0]);

        Lock writeLock = getReadWriteLock().writeLock();
        try {
            writeLock.lock();

            metadataProvider = rpGroup.getMetadataProvider();

            rpConfigs.clear();
            List<RelyingPartyConfiguration> newRpConfigs = rpGroup.getRelyingParties();
            if (newRpConfigs != null) {
                for (RelyingPartyConfiguration newRpConfig : newRpConfigs) {
                    rpConfigs.put(newRpConfig.getRelyingPartyId(), newRpConfig);
                    log.debug("Registering configuration for relying party: {}", newRpConfig.getRelyingPartyId());
                }
            }

            rpConfigs.put(ANONYMOUS_RP_NAME, rpGroup.getAnonymousRP());
            rpConfigs.put(DEFAULT_RP_NAME, rpGroup.getDefaultRP());
        } catch (Exception e) {
            log.error("Error loading information from new context", e);
        } finally {
            writeLock.unlock();
        }
    }
}