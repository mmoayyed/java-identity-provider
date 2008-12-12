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

package edu.internet2.middleware.shibboleth.common.security;

import java.lang.ref.SoftReference;
import java.security.cert.CRLException;
import java.security.cert.CertificateException;
import java.security.cert.X509CRL;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import javax.xml.namespace.QName;

import org.opensaml.saml2.common.Extensions;
import org.opensaml.saml2.metadata.EntitiesDescriptor;
import org.opensaml.saml2.metadata.EntityDescriptor;
import org.opensaml.saml2.metadata.KeyDescriptor;
import org.opensaml.saml2.metadata.RoleDescriptor;
import org.opensaml.saml2.metadata.provider.MetadataProvider;
import org.opensaml.saml2.metadata.provider.MetadataProviderException;
import org.opensaml.saml2.metadata.provider.ObservableMetadataProvider;
import org.opensaml.security.MetadataCriteria;
import org.opensaml.xml.XMLObject;
import org.opensaml.xml.security.CriteriaSet;
import org.opensaml.xml.security.SecurityException;
import org.opensaml.xml.security.credential.UsageType;
import org.opensaml.xml.security.criteria.EntityIDCriteria;
import org.opensaml.xml.security.criteria.UsageCriteria;
import org.opensaml.xml.security.keyinfo.KeyInfoHelper;
import org.opensaml.xml.security.x509.BasicPKIXValidationInformation;
import org.opensaml.xml.security.x509.PKIXValidationInformation;
import org.opensaml.xml.security.x509.PKIXValidationInformationResolver;
import org.opensaml.xml.signature.KeyInfo;
import org.opensaml.xml.signature.KeyName;
import org.opensaml.xml.util.DatatypeHelper;
import org.opensaml.xml.util.LazySet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import edu.internet2.middleware.shibboleth.common.xmlobject.ShibbolethMetadataKeyAuthority;

/**
 * An implementation of {@link PKIXValidationInformationResolver} which resolves {@link PKIXValidationInformation} based
 * on information stored in SAML 2 metadata. Validation information is retrieved from Shibboleth-specific metadata
 * extensions to {@link EntityDescriptor} and {@link EntitiesDescriptor} elements, represented by instances of
 * {@link ShibbolethMetadataKeyAuthority}.
 * 
 * Resolution of trusted names for an entity is also supported, based on {@link KeyName} information contained within
 * the {@link KeyInfo} of a role descriptor's {@link KeyDescriptor} element.
 */
public class MetadataPKIXValidationInformationResolver implements PKIXValidationInformationResolver {

    /** Default value for Shibboleth KeyAuthority verify depth. */
    public static final int KEY_AUTHORITY_VERIFY_DEPTH_DEFAULT = 1;
    
    /** Class logger. */
    private final Logger log = LoggerFactory.getLogger(MetadataPKIXValidationInformationResolver.class);

    /** Metadata provider from which to fetch the credentials. */
    private MetadataProvider metadata;

    /** Cache of resolved info. [MetadataCacheKey, Credentials] */
    private Map<MetadataCacheKey, SoftReference<List<PKIXValidationInformation>>> entityPKIXCache;

    /** Cache of resolved info. [Extensions, Credentials] */
    private Map<Extensions, SoftReference<List<PKIXValidationInformation>>> extensionsCache;

    /** Cache of resolved info. [MetadataCacheKey, Strings(trusted key names)] */
    private Map<MetadataCacheKey, SoftReference<Set<String>>> entityNamesCache;
    
    /** Lock used to synchronize access to the caches. */
    private ReadWriteLock rwlock;

    /**
     * Constructor.
     * 
     * @param metadataProvider provider of the metadata
     * 
     * @throws IllegalArgumentException thrown if the supplied provider is null
     */
    public MetadataPKIXValidationInformationResolver(MetadataProvider metadataProvider) {
        super();
        if (metadataProvider == null) {
            throw new IllegalArgumentException("Metadata provider may not be null");
        }
        metadata = metadataProvider;

        entityPKIXCache = new HashMap<MetadataCacheKey, SoftReference<List<PKIXValidationInformation>>>();
        extensionsCache = new HashMap<Extensions, SoftReference<List<PKIXValidationInformation>>>();
        entityNamesCache = new HashMap<MetadataCacheKey, SoftReference<Set<String>>>();
        
        rwlock = new ReentrantReadWriteLock();

        if (metadata instanceof ObservableMetadataProvider) {
            ObservableMetadataProvider observable = (ObservableMetadataProvider) metadataProvider;
            observable.getObservers().add(new MetadataProviderObserver());
        }

    }

    /** {@inheritDoc} */
    public PKIXValidationInformation resolveSingle(CriteriaSet criteriaSet) throws SecurityException {
        Iterable<PKIXValidationInformation> pkixInfo = resolve(criteriaSet);
        if (pkixInfo.iterator().hasNext()) {
            return pkixInfo.iterator().next();
        } else {
            return null;
        }
    }

    /** {@inheritDoc} */
    public Iterable<PKIXValidationInformation> resolve(CriteriaSet criteriaSet) throws SecurityException {

        checkCriteriaRequirements(criteriaSet);

        String entityID = criteriaSet.get(EntityIDCriteria.class).getEntityID();
        MetadataCriteria mdCriteria = criteriaSet.get(MetadataCriteria.class);
        QName role = mdCriteria.getRole();
        String protocol = mdCriteria.getProtocol();
        UsageCriteria usageCriteria = criteriaSet.get(UsageCriteria.class);
        UsageType usage = null;
        if (usageCriteria != null) {
            usage = usageCriteria.getUsage();
        } else {
            usage = UsageType.UNSPECIFIED;
        }
        
        // See Jira issue SIDP-229.
        log.debug("Forcing on-demand metadata provider refresh if necessary");
        try {
            metadata.getMetadata();
        } catch (MetadataProviderException e) {
            // don't care about errors at this level
        }

        MetadataCacheKey cacheKey = new MetadataCacheKey(entityID, role, protocol, usage);
        List<PKIXValidationInformation> pkixInfoSet = retrievePKIXInfoFromCache(cacheKey);

        if (pkixInfoSet == null) {
            pkixInfoSet = retrievePKIXInfoFromMetadata(entityID, role, protocol, usage);
            cachePKIXInfo(cacheKey, pkixInfoSet);
        }

        return pkixInfoSet;
    }

    /** {@inheritDoc} */
    public Set<String> resolveTrustedNames(CriteriaSet criteriaSet) throws SecurityException,
            UnsupportedOperationException {

        checkCriteriaRequirements(criteriaSet);

        String entityID = criteriaSet.get(EntityIDCriteria.class).getEntityID();
        MetadataCriteria mdCriteria = criteriaSet.get(MetadataCriteria.class);
        QName role = mdCriteria.getRole();
        String protocol = mdCriteria.getProtocol();
        UsageCriteria usageCriteria = criteriaSet.get(UsageCriteria.class);
        UsageType usage = null;
        if (usageCriteria != null) {
            usage = usageCriteria.getUsage();
        } else {
            usage = UsageType.UNSPECIFIED;
        }
        
        // See Jira issue SIDP-229.
        log.debug("Forcing on-demand metadata provider refresh if necessary");
        try {
            metadata.getMetadata();
        } catch (MetadataProviderException e) {
            // don't care about errors at this level
        }

        MetadataCacheKey cacheKey = new MetadataCacheKey(entityID, role, protocol, usage);
        Set<String> trustedNames = retrieveTrustedNamesFromCache(cacheKey);

        if (trustedNames == null) {
            trustedNames = retrieveTrustedNamesFromMetadata(entityID, role, protocol, usage);
            cacheTrustedNames(cacheKey, trustedNames);
        }

        return trustedNames;
    }

    /** {@inheritDoc} */
    public boolean supportsTrustedNameResolution() {
        return true;
    }
    
    /**
     * Get the lock instance used to synchronize access to the caches.
     * 
     * @return a read-write lock instance
     */
    protected ReadWriteLock getReadWriteLock() {
        return rwlock;
    }

    /**
     * Check that all necessary criteria are available.
     * 
     * @param criteriaSet the criteria set to evaluate
     */
    protected void checkCriteriaRequirements(CriteriaSet criteriaSet) {
        EntityIDCriteria entityCriteria = criteriaSet.get(EntityIDCriteria.class);
        MetadataCriteria mdCriteria = criteriaSet.get(MetadataCriteria.class);
        if (entityCriteria == null) {
            throw new IllegalArgumentException("Entity criteria must be supplied");
        }
        if (mdCriteria == null) {
            throw new IllegalArgumentException("SAML metadata criteria must be supplied");
        }
        if (DatatypeHelper.isEmpty(entityCriteria.getEntityID())) {
            throw new IllegalArgumentException("Entity ID criteria value must be supplied");
        }
        if (mdCriteria.getRole() == null) {
            throw new IllegalArgumentException("Metadata role criteria value must be supplied");
        }
    }

    /**
     * Retrieves validation information from the provided metadata.
     * 
     * @param entityID entity ID for which to resolve validation information
     * @param role role in which the entity is operating
     * @param protocol protocol over which the entity is operating (may be null)
     * @param usage usage specifier for role descriptor key descriptors to evaluate
     * 
     * @return collection of resolved validation information, possibly empty
     * 
     * @throws SecurityException thrown if the key, certificate, or CRL information is represented in an unsupported
     *             format
     */
    protected List<PKIXValidationInformation> retrievePKIXInfoFromMetadata(String entityID, QName role,
            String protocol, UsageType usage) throws SecurityException {

        log.debug("Attempting to retrieve PKIX validation info from metadata for entity: {}", entityID);
        List<PKIXValidationInformation> pkixInfoSet = new ArrayList<PKIXValidationInformation>();
        
        List<RoleDescriptor> roleDescriptors = getRoleDescriptors(entityID, role, protocol);
        if(roleDescriptors == null || roleDescriptors.isEmpty()){
            return pkixInfoSet;
        }

        for (RoleDescriptor roleDescriptor : roleDescriptors) {
            List<PKIXValidationInformation> roleInfo = resolvePKIXInfo(roleDescriptor);
            if (roleInfo != null && !roleInfo.isEmpty()) {
                pkixInfoSet.addAll(roleInfo);
            }
        }

        return pkixInfoSet;
    }

    /**
     * Retrieves validation information from the provided role descriptor.
     * 
     * @param roleDescriptor the role descriptor from which to resolve information.
     * @return collection of resolved validation information, possibly empty
     * @throws SecurityException thrown if the key, certificate, or CRL information is represented in an unsupported
     *             format
     * 
     */
    protected List<PKIXValidationInformation> resolvePKIXInfo(RoleDescriptor roleDescriptor)
            throws SecurityException {

        List<PKIXValidationInformation> pkixInfoSet = new ArrayList<PKIXValidationInformation>();

        XMLObject current = roleDescriptor.getParent();
        while (current != null) {
            if (current instanceof EntityDescriptor) {
                pkixInfoSet.addAll(resolvePKIXInfo(((EntityDescriptor) current).getExtensions()));
            } else if (current instanceof EntitiesDescriptor) {
                pkixInfoSet.addAll(resolvePKIXInfo(((EntitiesDescriptor) current).getExtensions()));
            }
            current = current.getParent();
        }
        return pkixInfoSet;
    }

    /**
     * Retrieves validation information from the metadata extension element.
     * 
     * @param extensions the extension element from which to resolve information
     * @return collection of resolved validation information, possibly empty
     * @throws SecurityException thrown if the key, certificate, or CRL information is represented in an unsupported
     *             format
     */
    protected List<PKIXValidationInformation> resolvePKIXInfo(Extensions extensions) throws SecurityException {
        if (extensions == null) {
            return Collections.emptyList();
        }

        List<PKIXValidationInformation> pkixInfoSet = retrieveExtensionsInfoFromCache(extensions);
        if (pkixInfoSet != null) {
            return pkixInfoSet;
        }
        
        if (log.isDebugEnabled()) {
            String parentName = getExtensionsParentName(extensions);
            if (parentName != null) {
                if (extensions.getParent() instanceof EntityDescriptor) {
                    log.debug("Resolving PKIX validation info for Extensions "
                            + "with EntityDescriptor parent: {}", parentName);
                } else  if (extensions.getParent() instanceof EntitiesDescriptor) {
                    log.debug("Resolving PKIX validation info for Extensions " 
                            + "with EntitiesDescriptor parent: {}", parentName);
                }
            } else {
                log.debug("Resolving PKIX validation info for Extensions " 
                        + "with unidentified parent");
            }
        }

        pkixInfoSet = new ArrayList<PKIXValidationInformation>();
        
        List<XMLObject> authorities = 
            extensions.getUnknownXMLObjects(ShibbolethMetadataKeyAuthority.DEFAULT_ELEMENT_NAME);
        if (authorities == null || authorities.isEmpty()) {
            return pkixInfoSet;
        }
        
        for (XMLObject xmlObj : authorities) {
            PKIXValidationInformation authoritySet = resolvePKIXInfo((ShibbolethMetadataKeyAuthority) xmlObj);
            if (authoritySet != null) {
                pkixInfoSet.add(authoritySet);
            }            
        }
        cacheExtensionsInfo(extensions, pkixInfoSet);
        return pkixInfoSet;
    }

    /**
     * Retrieves validation information from the Shibboleth KeyAuthority metadata extension element.
     * 
     * @param keyAuthority the Shibboleth KeyAuthority element from which to resolve information
     * @return an instance of resolved validation information
     * @throws SecurityException thrown if the key, certificate, or CRL information is represented in an unsupported
     *             format
     */
    protected PKIXValidationInformation resolvePKIXInfo(ShibbolethMetadataKeyAuthority keyAuthority)
            throws SecurityException {

        List<X509Certificate> certs = new ArrayList<X509Certificate>();
        List<X509CRL> crls = new ArrayList<X509CRL>();
        Integer depth = keyAuthority.getVerifyDepth();
        if (depth == null) {
            depth = KEY_AUTHORITY_VERIFY_DEPTH_DEFAULT;
        }
        
        List<KeyInfo> keyInfos = keyAuthority.getKeyInfos();
        if (keyInfos == null || keyInfos.isEmpty()) {
            return null;
        }
        
        for (KeyInfo keyInfo : keyInfos) {
            certs.addAll(getX509Certificates(keyInfo));
            crls.addAll(getX509CRLs(keyInfo));
        }
        
        // Unlikely, but go ahead and check.
        if (certs.isEmpty() && crls.isEmpty()) {
            return null;
        }

        return new BasicPKIXValidationInformation(certs, crls, depth);
    }

    /**
     * Extract certificates from a KeyInfo element.
     * 
     * @param keyInfo the KeyInfo instance from which to extract certificates
     * @return a collection of X509 certificates, possibly empty
     * @throws SecurityException thrown if the certificate information is represented in an unsupported format
     */
    protected List<X509Certificate> getX509Certificates(KeyInfo keyInfo) throws SecurityException {
        try {
            return KeyInfoHelper.getCertificates(keyInfo);
        } catch (CertificateException e) {
            throw new SecurityException("Error extracting certificates from KeyAuthority KeyInfo", e);
        }

    }

    /**
     * Extract CRL's from a KeyInfo element.
     * 
     * @param keyInfo the KeyInfo instance from which to extract CRL's
     * @return a collection of X509 CRL's, possibly empty
     * @throws SecurityException thrown if the CRL information is represented in an unsupported format
     */
    protected List<X509CRL> getX509CRLs(KeyInfo keyInfo) throws SecurityException {
        try {
            return KeyInfoHelper.getCRLs(keyInfo);
        } catch (CRLException e) {
            throw new SecurityException("Error extracting CRL's from KeyAuthority KeyInfo", e);
        }

    }

    /**
     * Retrieves trusted name information from the provided metadata.
     * 
     * @param entityID entity ID for which to resolve trusted names
     * @param role role in which the entity is operating
     * @param protocol protocol over which the entity is operating (may be null)
     * @param usage usage specifier for role descriptor key descriptors to evaluate
     * 
     * @return collection of resolved trusted name information, possibly empty
     * 
     * @throws SecurityException thrown if there is an error extracting trusted name information
     */
    protected Set<String> retrieveTrustedNamesFromMetadata(String entityID, QName role, String protocol,
            UsageType usage) throws SecurityException {

        log.debug("Attempting to retrieve trusted names for PKIX validation from metadata for entity: {}", entityID);
        Set<String> trustedNames = new LazySet<String>();
        
        List<RoleDescriptor> roleDescriptors = getRoleDescriptors(entityID, role, protocol);
        if(roleDescriptors == null || roleDescriptors.isEmpty()){
            return trustedNames;
        }

        for (RoleDescriptor roleDescriptor : roleDescriptors) {
            List<KeyDescriptor> keyDescriptors = roleDescriptor.getKeyDescriptors();
            if(keyDescriptors == null || keyDescriptors.isEmpty()){
                return trustedNames;
            }         
            for (KeyDescriptor keyDescriptor : keyDescriptors) {
                UsageType mdUsage = keyDescriptor.getUse();
                if (mdUsage == null) {
                    mdUsage = UsageType.UNSPECIFIED;
                }
                if (matchUsage(mdUsage, usage)) {
                    if (keyDescriptor.getKeyInfo() != null) {
                        trustedNames.addAll(getTrustedNames(keyDescriptor.getKeyInfo()));
                    }
                }
            }

        }

        return trustedNames;
    }

    /**
     * Extract trusted names from a KeyInfo element.
     * 
     * @param keyInfo the KeyInfo instance from which to extract trusted names
     * @return set of trusted names, possibly empty
     */
    protected Set<String> getTrustedNames(KeyInfo keyInfo) {
        // TODO return anything if there are things other than names in the KeyInfo ?
        Set<String> names = new LazySet<String>();
        names.addAll(KeyInfoHelper.getKeyNames(keyInfo));
        return names;
    }

    /**
     * Match usage enum type values from metadata KeyDescriptor and from specified resolution criteria.
     * 
     * @param metadataUsage the value from the 'use' attribute of a metadata KeyDescriptor element
     * @param criteriaUsage the value from specified criteria
     * @return true if the two usage specifiers match for purposes of resolving validation information, false otherwise
     */
    protected boolean matchUsage(UsageType metadataUsage, UsageType criteriaUsage) {
        if (metadataUsage == UsageType.UNSPECIFIED || criteriaUsage == UsageType.UNSPECIFIED) {
            return true;
        }
        return metadataUsage == criteriaUsage;
    }

    /**
     * Get the list of metadata role descriptors which match the given entityID, role and protocol.
     * 
     * @param entityID entity ID of the metadata entity descriptor to resolve
     * @param role role in which the entity is operating
     * @param protocol protocol over which the entity is operating (may be null)
     * @return a list of role descriptors matching the given parameters, or null
     * @throws SecurityException thrown if there is an error retrieving role descriptors from the metadata provider
     */
    protected List<RoleDescriptor> getRoleDescriptors(String entityID, QName role, String protocol)
            throws SecurityException {
        try {
            if (DatatypeHelper.isEmpty(protocol)) {
                return metadata.getRole(entityID, role);
            } else {
                RoleDescriptor roleDescriptor = metadata.getRole(entityID, role, protocol);
                if (roleDescriptor == null) {
                    return null;
                }
                List<RoleDescriptor> roles = new ArrayList<RoleDescriptor>();
                roles.add(roleDescriptor);
                return roles;
            }
        } catch (MetadataProviderException e) {
            log.error("Unable to read metadata from provider", e);
            throw new SecurityException("Unable to read metadata provider", e);
        }
    }

    /**
     * Retrieves pre-resolved PKIX validation information from the cache.
     * 
     * @param cacheKey the key to the metadata cache
     * @return the collection of cached info or null
     */
    protected List<PKIXValidationInformation> retrievePKIXInfoFromCache(MetadataCacheKey cacheKey) {
        log.debug("Attempting to retrieve PKIX validation info from cache using index: {}", cacheKey);
        Lock readLock = getReadWriteLock().readLock();
        readLock.lock();
        log.debug("Read lock over cache acquired");
        try {
            if (entityPKIXCache.containsKey(cacheKey)) {
                SoftReference<List<PKIXValidationInformation>> reference = entityPKIXCache.get(cacheKey);
                if (reference.get() != null) {
                    log.debug("Retrieved PKIX validation info from cache using index: {}", cacheKey);
                    return reference.get();
                }
            }
        } finally {
            readLock.unlock();
            log.debug("Read lock over cache released");
        }

        log.debug("Unable to retrieve PKIX validation info from cache using index: {}", cacheKey);
        return null;
    }

    /**
     * Retrieves pre-resolved PKIX validation information from the cache.
     * 
     * @param extensions the key to the metadata cache
     * @return the collection of cached info or null
     */
    protected List<PKIXValidationInformation> retrieveExtensionsInfoFromCache(Extensions extensions) {
        if (log.isDebugEnabled()) {
            String parentName = getExtensionsParentName(extensions);
            if (parentName != null) {
                if (extensions.getParent() instanceof EntityDescriptor) {
                    log.debug("Attempting to retrieve PKIX validation info from cache for Extensions "
                            + "with EntityDescriptor parent: {}", parentName);
                } else  if (extensions.getParent() instanceof EntitiesDescriptor) {
                    log.debug("Attempting to retrieve PKIX validation info from cache for Extensions " 
                            + "with EntitiesDescriptor parent: {}", parentName);
                }
            } else {
                log.debug("Attempting to retrieve PKIX validation info from cache for Extensions " 
                        + "with unidentified parent");
            }
        }
        
        Lock readLock = getReadWriteLock().readLock();
        readLock.lock();
        log.debug("Read lock over cache acquired");
        try {
            if (extensionsCache.containsKey(extensions)) {
                SoftReference<List<PKIXValidationInformation>> reference = extensionsCache.get(extensions);
                if (reference.get() != null) {
                    log.debug("Retrieved PKIX validation info from cache using index: {}", extensions);
                    return reference.get();
                }
            }
        } finally {
            readLock.unlock();
            log.debug("Read lock over cache released");
        }
        log.debug("Unable to retrieve PKIX validation info from cache using index: {}", extensions);
        return null;
    }
    
    /**
     * Retrieves pre-resolved trusted names from the cache.
     * 
     * @param cacheKey the key to the metadata cache
     * @return the set of cached info or null
     */
    protected Set<String> retrieveTrustedNamesFromCache(MetadataCacheKey cacheKey) {
        log.debug("Attempting to retrieve trusted names from cache using index: {}", cacheKey);
        Lock readLock = getReadWriteLock().readLock();
        readLock.lock();
        log.debug("Read lock over cache acquired");
        try {
            if (entityNamesCache.containsKey(cacheKey)) {
                SoftReference<Set<String>> reference = entityNamesCache.get(cacheKey);
                if (reference.get() != null) {
                    log.debug("Retrieved trusted names from cache using index: {}", cacheKey);
                    return reference.get();
                }
            }
        } finally {
            readLock.unlock();
            log.debug("Read lock over cache released");
        }

        log.debug("Unable to retrieve trusted names from cache using index: {}", cacheKey);
        return null;
    }

    /**
     * Adds resolved PKIX validation information to the cache.
     * 
     * @param cacheKey the key for caching the information
     * @param pkixInfo collection of PKIX information to cache
     */
    protected void cachePKIXInfo(MetadataCacheKey cacheKey, List<PKIXValidationInformation> pkixInfo) {
        Lock writeLock = getReadWriteLock().writeLock();
        writeLock.lock();
        log.debug("Write lock over cache acquired");
        try {
            entityPKIXCache.put(cacheKey, new SoftReference<List<PKIXValidationInformation>>(pkixInfo));
            log.debug("Added new PKIX info to entity cache with key: {}", cacheKey);
        } finally {
            writeLock.unlock();
            log.debug("Write lock over cache released"); 
        }
    }

    /**
     * Adds resolved PKIX validation information to the cache.
     * 
     * @param extensions the key for caching the information
     * @param pkixInfo collection of PKIX information to cache
     */
    protected void cacheExtensionsInfo(Extensions extensions, List<PKIXValidationInformation> pkixInfo) {
        Lock writeLock = getReadWriteLock().writeLock();
        writeLock.lock();
        log.debug("Write lock over cache acquired");
        try {
            extensionsCache.put(extensions, new SoftReference<List<PKIXValidationInformation>>(pkixInfo));
            if (log.isDebugEnabled()) {
                log.debug("Added new PKIX info to cache for Extensions with parent: {}",
                        getExtensionsParentName(extensions));
            }
        } finally {
            writeLock.unlock();
            log.debug("Write lock over cache released"); 
        }
    }

    /**
     * Adds resolved trusted name information to the cache.
     * 
     * @param cacheKey the key for caching the information
     * @param names collection of names to cache
     */
    protected void cacheTrustedNames(MetadataCacheKey cacheKey, Set<String> names) {
        Lock writeLock = getReadWriteLock().writeLock();
        writeLock.lock();
        log.debug("Write lock over cache acquired");
        try {
            entityNamesCache.put(cacheKey, new SoftReference<Set<String>>(names));
            log.debug("Added new PKIX info to entity cache with key: {}", cacheKey);
        } finally {
            writeLock.unlock();
            log.debug("Write lock over cache released"); 
        }
    }

    /**
     * Get the name of the parent element of an {@link Extensions} element in metadata, mostly
     * useful for logging purposes.
     * 
     * If the parent is an EntityDescriptor, return the entityID value.  If an EntitiesDescriptor,
     * return the name value.
     * 
     * @param extensions the Extensions element
     * @return the Extensions element's parent's name
     */
    protected String getExtensionsParentName(Extensions extensions) {
        XMLObject parent = extensions.getParent();
        if (parent == null) {
            return null;
        }
        if (parent instanceof EntityDescriptor) {
            return ((EntityDescriptor) parent).getEntityID();
        } else  if (extensions.getParent() instanceof EntitiesDescriptor) {
            return ((EntitiesDescriptor) parent).getName();
        }
        return null;
    }

    /**
     * A class which serves as the key into the cache of information previously resolved.
     */
    protected class MetadataCacheKey {

        /** Entity ID associated with resolved information. */
        private String id;

        /** Role in which the entity is operating. */
        private QName role;

        /** Protocol over which the entity is operating (may be null). */
        private String protocol;

        /** Intended usage specifier of the role descriptor's key descriptor. */
        private UsageType usage;

        /**
         * Constructor.
         * 
         * @param entityID entity ID of the metadata entity descriptor to resolve
         * @param entityRole role in which the entity is operating
         * @param entityProtocol protocol over which the entity is operating (may be null)
         * @param entityUsage usage specifier of the role descriptor's key descriptor
         */
        protected MetadataCacheKey(String entityID, QName entityRole, String entityProtocol, UsageType entityUsage) {
            if (entityID == null) {
                throw new IllegalArgumentException("Entity ID may not be null");
            }
            if (entityRole == null) {
                throw new IllegalArgumentException("Entity role may not be null");
            }
            if (entityUsage == null) {
                throw new IllegalArgumentException("Usage may not be null");
            }
            id = entityID;
            role = entityRole;
            protocol = entityProtocol;
            usage = entityUsage;
        }

        /** {@inheritDoc} */
        public boolean equals(Object obj) {
            if (obj == this) {
                return true;
            }
            if (!(obj instanceof MetadataCacheKey)) {
                return false;
            }
            MetadataCacheKey other = (MetadataCacheKey) obj;
            if (!this.id.equals(other.id) || !this.role.equals(other.role) || this.usage != other.usage) {
                return false;
            }
            if (this.protocol == null) {
                if (other.protocol != null) {
                    return false;
                }
            } else {
                if (!this.protocol.equals(other.protocol)) {
                    return false;
                }
            }
            return true;
        }

        /** {@inheritDoc} */
        public int hashCode() {
            int result = 17;
            result = 37 * result + id.hashCode();
            result = 37 * result + role.hashCode();
            if (protocol != null) {
                result = 37 * result + protocol.hashCode();
            }
            result = 37 * result + usage.hashCode();
            return result;
        }

        /** {@inheritDoc} */
        public String toString() {
            return String.format("[%s,%s,%s,%s]", id, role, protocol, usage);
        }

    }

    /**
     * An observer that clears the credential cache if the underlying metadata changes.
     */
    protected class MetadataProviderObserver implements ObservableMetadataProvider.Observer {

        /** {@inheritDoc} */
        public void onEvent(MetadataProvider provider) {
            Lock writeLock = getReadWriteLock().writeLock();
            writeLock.lock();
            log.debug("Write lock over cache acquired");
            try {
                entityPKIXCache.clear();
                extensionsCache.clear();
                entityNamesCache.clear();
                log.info("PKIX validation info cache cleared");
            } finally {
                writeLock.unlock();
                log.debug("Write lock over cache released"); 
            }
        }
    }

}
