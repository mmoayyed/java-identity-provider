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
import java.util.Timer;
import java.util.concurrent.locks.Lock;

import org.apache.log4j.Logger;
import org.opensaml.saml2.metadata.EntitiesDescriptor;
import org.opensaml.saml2.metadata.EntityDescriptor;
import org.opensaml.saml2.metadata.provider.MetadataProvider;
import org.opensaml.saml2.metadata.provider.MetadataProviderException;
import org.opensaml.util.resource.Resource;
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
    private final Logger log = Logger.getLogger(SAMLMDRelyingPartyConfigurationManager.class);

    /** Metadata provider used to lookup information about entities. */
    private MetadataProvider metadataProvider;

    /** Regisered relying party configurations. */
    private HashMap<String, RelyingPartyConfiguration> rpConfigs;

    /**
     * Constructor.
     * 
     * @param configurations configuration resources for this service
     */
    public SAMLMDRelyingPartyConfigurationManager(List<Resource> configurations) {
        super(configurations);
        rpConfigs = new HashMap<String, RelyingPartyConfiguration>();
    }

    /**
     * Constructor.
     * 
     * @param timer timer resource polling tasks are scheduled with
     * @param configurations configuration resources for this service
     * @param pollingFrequency the frequency, in milliseconds, to poll the policy resources for changes, must be greater
     *            than zero
     */
    public SAMLMDRelyingPartyConfigurationManager(List<Resource> configurations, Timer timer, long pollingFrequency) {
        super(timer, configurations, pollingFrequency);
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
        if (log.isDebugEnabled()) {
            log.debug("Looking up relying party configuration for " + relyingPartyEntityID);
        }
        if (rpConfigs.containsKey(relyingPartyEntityID)) {
            if (log.isDebugEnabled()) {
                log.debug("Relying party configuration found for " + relyingPartyEntityID);
            }
            readLock.unlock();
            return rpConfigs.get(relyingPartyEntityID);
        }

        if (log.isDebugEnabled()) {
            log.debug("No relying party configuration was registered for " + relyingPartyEntityID
                    + " looking up configuration based on metadata groups");
        }
        try {
            EntityDescriptor entityDescriptor = metadataProvider.getEntityDescriptor(relyingPartyEntityID);
            if (entityDescriptor != null) {
                EntitiesDescriptor entityGroup = (EntitiesDescriptor) entityDescriptor.getParent();
                while (entityGroup != null) {
                    if (rpConfigs.containsKey(entityGroup.getName())) {
                        if (log.isDebugEnabled()) {
                            log.debug("Relying party configuration found for " + relyingPartyEntityID
                                    + " as member of metadata group " + entityGroup.getName());
                        }
                        readLock.unlock();
                        return rpConfigs.get(entityGroup.getName());
                    }
                    entityGroup = (EntitiesDescriptor) entityGroup.getParent();
                }
            }
        } catch (MetadataProviderException e) {
            log.error("Error fetching metadata for relying party " + relyingPartyEntityID, e);
        }

        if (log.isDebugEnabled()) {
            log.debug("No realying party configuration found for " + relyingPartyEntityID
                    + " using default configuration");
        }
        readLock.unlock();

        return getDefaultRelyingPartyConfiguration();
    }

    /** {@inheritDoc} */
    public Map<String, RelyingPartyConfiguration> getRelyingPartyConfigurations() {
        return rpConfigs;
    }

    /** {@inheritDoc} */
    protected void newContextCreated(ApplicationContext newServiceContext) throws ServiceException {
        String[] relyingPartyGroupNames = newServiceContext.getBeanNamesForType(RelyingPartyGroup.class);
        RelyingPartyGroup rpGroup = (RelyingPartyGroup) newServiceContext.getBean(relyingPartyGroupNames[0]);

        String[] rpConfigBeanNames = newServiceContext.getBeanNamesForType(RelyingPartyConfiguration.class);

        Lock writeLock = getReadWriteLock().writeLock();
        writeLock.lock();

        metadataProvider = rpGroup.getMetadataProvider();

        rpConfigs.clear();
        RelyingPartyConfiguration rpConfig;
        if (rpConfigBeanNames != null && relyingPartyGroupNames.length > 0) {
            for (String rpConfigBeanName : rpConfigBeanNames) {
                rpConfig = (RelyingPartyConfiguration) newServiceContext.getBean(rpConfigBeanName);
                rpConfigs.put(rpConfig.getRelyingPartyId(), rpConfig);
                if (log.isDebugEnabled()) {
                    log.debug("Registering configuration for relying party: " + rpConfig.getRelyingPartyId());
                }
            }
        }

        // This results, for some reason, in Spring trying to cast profile configurations as relying party
        // configurations
        // TODO figure this out
        // List<RelyingPartyConfiguration> newRpConfigs = newServiceContext.get;
        // if(rpConfigs != null){
        // for(RelyingPartyConfiguration newRpConfig : newRpConfigs){
        // rpConfigs.put(newRpConfig.getRelyingPartyId(), newRpConfig);
        // if(log.isDebugEnabled()){
        // log.debug("Registering configuration for relying party: " + newRpConfig.getRelyingPartyId());
        // }
        // }
        // }

        writeLock.unlock();
    }
}