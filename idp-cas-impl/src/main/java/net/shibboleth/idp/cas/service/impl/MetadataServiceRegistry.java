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

package net.shibboleth.idp.cas.service.impl;

import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import com.google.common.collect.Lists;
import net.shibboleth.idp.cas.config.impl.AbstractProtocolConfiguration;
import net.shibboleth.idp.cas.service.Service;
import net.shibboleth.idp.cas.service.ServiceRegistry;
import net.shibboleth.utilities.java.support.resolver.CriteriaSet;
import net.shibboleth.utilities.java.support.resolver.ResolverException;
import org.opensaml.core.xml.XMLObject;
import org.opensaml.saml.criterion.EndpointCriterion;
import org.opensaml.saml.criterion.EntityRoleCriterion;
import org.opensaml.saml.criterion.ProtocolCriterion;
import org.opensaml.saml.criterion.StartsWithLocationCriterion;
import org.opensaml.saml.metadata.resolver.MetadataResolver;
import org.opensaml.saml.saml2.metadata.AssertionConsumerService;
import org.opensaml.saml.saml2.metadata.EntitiesDescriptor;
import org.opensaml.saml.saml2.metadata.EntityDescriptor;
import org.opensaml.saml.saml2.metadata.KeyDescriptor;
import org.opensaml.saml.saml2.metadata.SPSSODescriptor;
import org.opensaml.saml.saml2.metadata.SingleLogoutService;
import org.opensaml.saml.saml2.metadata.impl.AssertionConsumerServiceBuilder;
import org.opensaml.xmlsec.keyinfo.KeyInfoSupport;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * CAS service registry implementation that queries SAML metadata for a CAS service given a CAS service URL using
 * the following strategy. A {@link MetadataResolver} is queried for an {@link EntityDescriptor} that contains at
 * least one <code>AssertionConsumerService</code> endpoint that meets the following criteria:
 *
 * <ol>
 *     <li>Defines <code>https://www.apereo.org/cas/protocol</code> in its <code>protocolSupportEnumeration</code>
 *         attribute.</li>
 *     <li>Defines a <code>Location</code> URL where the given service URL starts with the ACS URL.</li>
 * </ol>
 *
 * If a single match is found, it is converted to a {@link Service} and returned; if more than result is found, a
 * {@link ResolverException} is raised, otherwise null is returned.
 *
 * @author Marvin S. Addison
 */
public class MetadataServiceRegistry implements ServiceRegistry {

    /** Class logger. */
    private final Logger log = LoggerFactory.getLogger(MetadataServiceRegistry.class);

    /** SAML metadata resolver. */
    @Nonnull
    private final MetadataResolver metadataResolver;


    /**
     * Create a new instance that queries the given metadata resolver.
     *
     * @param resolver SAML metadata resolver.
     */
    public MetadataServiceRegistry(@Nonnull final MetadataResolver resolver) {
        metadataResolver = resolver;
    }

    @Nullable
    @Override
    public Service lookup(@Nonnull String serviceURL) {
        try {
            final List<EntityDescriptor> entities = Lists.newArrayList(metadataResolver.resolve(criteria(serviceURL)));
            if (entities.size() > 1) {
                throw new ResolverException("Multiple results found");
            } else if (entities.size() == 1) {
                return create(serviceURL, entities.get(0));
            }
        } catch (ResolverException e) {
            log.warn("Metadata resolution failed for {}", serviceURL, e);
        }
        return null;
    }

    /**
     * Create the set of criteria used to find a unique CAS service given a CAS service URL.
     *
     * @param serviceURL CAS service URL.
     *
     * @return Metadata resolver criteria set.
     */
    @Nonnull
    protected CriteriaSet criteria(@Nonnull final String serviceURL) {
        final AssertionConsumerService acs = new AssertionConsumerServiceBuilder().buildObject();
        acs.setLocation(serviceURL);
        return new CriteriaSet(
                new EntityRoleCriterion(SPSSODescriptor.DEFAULT_ELEMENT_NAME),
                new EndpointCriterion<>(acs),
                new ProtocolCriterion(AbstractProtocolConfiguration.PROTOCOL_URI),
                new StartsWithLocationCriterion());
    }

    /**
     * Create a CAS {@link Service} from an input service URL and the matching {@link EntityDescriptor} that was
     * resolved from the metadata source.
     *
     * @param serviceURL CAS service URL.
     * @param entity Entity resolved from metadata.
     *
     * @return CAS service created from inputs.
     */
    @Nonnull
    protected Service create(@Nonnull final String serviceURL, @Nonnull final EntityDescriptor entity) {
        final XMLObject parent = entity.getParent();
        final SPSSODescriptor descriptor = entity.getSPSSODescriptor(AbstractProtocolConfiguration.PROTOCOL_URI);
        if (descriptor == null) {
            throw new IllegalStateException("SPSSODescriptor element not found for entity " + entity.getEntityID());
        }
        final Service service = new Service(
                serviceURL,
                parent instanceof EntitiesDescriptor ? ((EntitiesDescriptor) parent).getName() : "unknown",
                isAuthorizedToProxy(descriptor),
                hasSingleLogoutService(descriptor));
        service.setEntityDescriptor(entity);
        return service;
    }

    private boolean isAuthorizedToProxy(@Nonnull final SPSSODescriptor descriptor) {
        final Set<X509Certificate> certs = new HashSet<>();
        for (KeyDescriptor kd : descriptor.getKeyDescriptors()) {
            if (kd.getKeyInfo() != null) {
                try {
                    certs.addAll(KeyInfoSupport.getCertificates(kd.getKeyInfo()));
                } catch (CertificateException e) {
                    throw new RuntimeException("Error decoding metadata certificate", e);
                }
            }
        }
        return certs.size() > 0;
    }

    private boolean hasSingleLogoutService(@Nonnull final SPSSODescriptor descriptor) {
        if (descriptor != null) {
            return descriptor.getEndpoints(SingleLogoutService.DEFAULT_ELEMENT_NAME).size() > 0;
        }
        return false;
    }
}
