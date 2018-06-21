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

import java.util.List;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import com.google.common.collect.Lists;
import net.shibboleth.idp.cas.config.impl.AbstractProtocolConfiguration;
import net.shibboleth.idp.cas.config.impl.LoginConfiguration;
import net.shibboleth.idp.cas.config.impl.ProxyConfiguration;
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
import org.opensaml.saml.saml2.metadata.AttributeAuthorityDescriptor;
import org.opensaml.saml.saml2.metadata.AttributeService;
import org.opensaml.saml.saml2.metadata.Endpoint;
import org.opensaml.saml.saml2.metadata.EntitiesDescriptor;
import org.opensaml.saml.saml2.metadata.EntityDescriptor;
import org.opensaml.saml.saml2.metadata.SPSSODescriptor;
import org.opensaml.saml.saml2.metadata.SingleLogoutService;
import org.opensaml.saml.saml2.metadata.impl.AssertionConsumerServiceBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * CAS service registry implementation that queries SAML metadata for a CAS service given a CAS service URL using
 * the following strategy. A {@link MetadataResolver} is queried for an {@link EntityDescriptor} that meets the
 * following criteria:
 *
 * <ol>
 *     <li>Defines <code>{@value AbstractProtocolConfiguration#PROTOCOL_URI}</code> in the
 *        <code>protocolSupportEnumeration</code> attribute of an <code>SPSSODescriptor</code> element.</li>
 *     <li>Defines an <code>AssertionConsumerService</code> element where the <code>Binding</code> URI is
 *        {@value #LOGIN_BINDING}.</li>
 *      <li>Matching <code>AssertionConsumerService</code> element also defines a <code>Location</code> attribute
 *        where the given service URL starts with the ACS location.</li>
 * </ol>
 *
 * If a single match is found, it is converted to a {@link Service} and returned; if more than result is found, a
 * {@link ResolverException} is raised, otherwise null is returned.
 * <p>
 * The presence of an <code>AttributeAuthorityDescriptor</code> element that advertises protocol support for
 * <code>{@value AbstractProtocolConfiguration#PROTOCOL_URI}</code> signals a CAS proxy endpoint. If at least one
 * <code>AttributeService</code> with a binding of <code>{@value #PROXY_BINDING}</code> is defined, the service is
 * authorized to request proxy granting tickets.
 * <p>
 * See the <a href="https://wiki.shibboleth.net/confluence/x/BQfKAg">SAML metadata profile for CAS</a> for further
 * details.
 *
 * @author Marvin S. Addison
 */
public class MetadataServiceRegistry implements ServiceRegistry {

    /** URI identifying an ACS endpoint that requests CAS service tickets. */
    public static final String LOGIN_BINDING = LoginConfiguration.PROFILE_ID;

    /** URI identifying a CAS SLO endpoint. */
    public static final String LOGOUT_BINDING = AbstractProtocolConfiguration.PROTOCOL_URI + "/logout";

    /** URI identifying a CAS proxy callback endoint. */
    public static final String PROXY_BINDING = ProxyConfiguration.PROFILE_ID;

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
        final AssertionConsumerService loginACS = new AssertionConsumerServiceBuilder().buildObject();
        loginACS.setBinding(LOGIN_BINDING);
        loginACS.setLocation(serviceURL);
        return new CriteriaSet(
                new EntityRoleCriterion(SPSSODescriptor.DEFAULT_ELEMENT_NAME),
                new EndpointCriterion<>(loginACS),
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
        final Service service = new Service(
                serviceURL,
                parent instanceof EntitiesDescriptor ? ((EntitiesDescriptor) parent).getName() : "unknown",
                isAuthorizedToProxy(entity),
                hasSingleLogoutService(entity));
        service.setEntityDescriptor(entity);
        return service;
    }

    private boolean isAuthorizedToProxy(@Nonnull final EntityDescriptor entity) {
        final AttributeAuthorityDescriptor descriptor = entity.getAttributeAuthorityDescriptor(
                AbstractProtocolConfiguration.PROTOCOL_URI);
        if (descriptor != null) {
            for (AttributeService as : descriptor.getAttributeServices()) {
                if (PROXY_BINDING.equals(as.getBinding())) {
                    return true;
                }
            }
        }
        return false;
    }

    private boolean hasSingleLogoutService(@Nonnull final EntityDescriptor entity) {
        final SPSSODescriptor descriptor = entity.getSPSSODescriptor(AbstractProtocolConfiguration.PROTOCOL_URI);
        if (descriptor != null) {
            for (Endpoint endpoint : descriptor.getEndpoints(SingleLogoutService.DEFAULT_ELEMENT_NAME)) {
                if (LOGOUT_BINDING.equals(endpoint.getBinding())) {
                    return true;
                }
            }
        }
        return false;
    }
}
