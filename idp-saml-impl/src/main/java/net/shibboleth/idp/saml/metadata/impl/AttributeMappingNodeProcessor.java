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

package net.shibboleth.idp.saml.metadata.impl;

import java.util.Collection;
import java.util.List;

import javax.annotation.Nonnull;
import javax.annotation.concurrent.NotThreadSafe;

import net.shibboleth.idp.attribute.AttributeDecodingException;
import net.shibboleth.idp.attribute.IdPAttribute;
import net.shibboleth.idp.attribute.IdPRequestedAttribute;
import net.shibboleth.idp.attribute.transcoding.AttributeTranscoder;
import net.shibboleth.idp.attribute.transcoding.AttributeTranscoderRegistry;
import net.shibboleth.idp.attribute.transcoding.TranscoderSupport;
import net.shibboleth.idp.attribute.transcoding.TranscodingRule;
import net.shibboleth.idp.saml.attribute.transcoding.AttributesMapContainer;
import net.shibboleth.utilities.java.support.annotation.constraint.Live;
import net.shibboleth.utilities.java.support.annotation.constraint.NonnullElements;
import net.shibboleth.utilities.java.support.logic.Constraint;
import net.shibboleth.utilities.java.support.service.ReloadableService;
import net.shibboleth.utilities.java.support.service.ServiceableComponent;

import org.opensaml.core.xml.XMLObject;
import org.opensaml.saml.ext.saml2mdattr.EntityAttributes;
import org.opensaml.saml.metadata.resolver.filter.FilterException;
import org.opensaml.saml.metadata.resolver.filter.MetadataNodeProcessor;
import org.opensaml.saml.saml2.core.Attribute;
import org.opensaml.saml.saml2.metadata.AttributeConsumingService;
import org.opensaml.saml.saml2.metadata.EntityDescriptor;
import org.opensaml.saml.saml2.metadata.Extensions;
import org.opensaml.saml.saml2.metadata.RequestedAttribute;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;

/**
 * An implementation of {@link MetadataNodeProcessor} which extracts {@link IdPRequestedAttribute}s from any
 * {@link AttributeConsumingService} we find and {@link IdPAttribute}s from any {@link EntityDescriptor} that we find.
 */
@NotThreadSafe
public class AttributeMappingNodeProcessor implements MetadataNodeProcessor {

    /** Class logger. */
    @Nonnull private final Logger log = LoggerFactory.getLogger(AttributeMappingNodeProcessor.class);

    /** Service used to get the registry of decoding rules. */
    @Nonnull private final ReloadableService<AttributeTranscoderRegistry> transcoderRegistry;

    /**
     * Constructor.
     * 
     * @param registry the service for the decoding rules
     */
    public AttributeMappingNodeProcessor(@Nonnull final ReloadableService<AttributeTranscoderRegistry> registry) {
        transcoderRegistry = Constraint.isNotNull(registry, "AttributeTranscoderRegistry cannot be null");
    }
    
    /** {@inheritDoc} */
    @Override public void process(final XMLObject metadataNode) throws FilterException {
        
        ServiceableComponent<AttributeTranscoderRegistry> component = null;
        
        try {
            if (metadataNode instanceof AttributeConsumingService) {
                component = transcoderRegistry.getServiceableComponent();
                if (component == null) {
                    log.error("Attribute transcoding service unavailable");
                } else {
                    handleAttributeConsumingService(component.getComponent(), (AttributeConsumingService) metadataNode);
                }
            } else if (metadataNode instanceof EntityDescriptor) {
                component = transcoderRegistry.getServiceableComponent();
                if (component == null) {
                    log.error("Attribute transcoding service unavailable");
                } else {
                    handleEntityDescriptor(component.getComponent(), (EntityDescriptor) metadataNode);
                }
            }
        } finally {
            if (component != null) {
                component.unpinComponent();
            }
        }
    }

    /**
     * Look inside the {@link AttributeConsumingService} for any {@link RequestedAttribute}s and map them.
     * 
     * @param registry the registry service
     * @param acs the {@link AttributeConsumingService} to look at
     */
    private void handleAttributeConsumingService(@Nonnull final AttributeTranscoderRegistry registry,
            @Nonnull final AttributeConsumingService acs) {
        
        final List<RequestedAttribute> requestedAttributes = acs.getRequestAttributes();
        if (null == requestedAttributes || requestedAttributes.isEmpty()) {
            return;
        }
        
        final Multimap<String,IdPAttribute> results = HashMultimap.create();
        for (final RequestedAttribute req : requestedAttributes) {
            try {
                decodeAttribute(registry, req, results);
            } catch (final AttributeDecodingException e) {
                log.warn("Error decoding RequestedAttribute '{}'", req.getName(), e);
            }
        }
        
        if (!results.isEmpty()) {
            acs.getObjectMetadata().put(new AttributesMapContainer<>(results));
        }
    }

    /**
     * Look inside the {@link EntityDescriptor} for entities Attributes and map them.
     * 
     * @param registry the registry service
     * @param entity the entity
     */
//CheckStyle: ReturnCount OFF
    private void handleEntityDescriptor(@Nonnull final AttributeTranscoderRegistry registry,
            @Nonnull final EntityDescriptor entity) {
        final Extensions extensions = entity.getExtensions();
        if (null == extensions) {
            return;
        }
        
        final List<XMLObject> entityAttributesList =
                extensions.getUnknownXMLObjects(EntityAttributes.DEFAULT_ELEMENT_NAME);
        if (null == entityAttributesList || entityAttributesList.isEmpty()) {
            return;
        }
        
        final Multimap<String,IdPAttribute> results = HashMultimap.create();
        
        for (final XMLObject xmlObj : entityAttributesList) {
            if (xmlObj instanceof EntityAttributes) {
                final EntityAttributes ea = (EntityAttributes) xmlObj;
                for (final Attribute attr : ea.getAttributes()) {
                    try {
                        decodeAttribute(registry, attr, results);
                    } catch (final AttributeDecodingException e) {
                        log.warn("Error decoding RequestedAttribute '{}'", attr.getName(), e);
                    }
                }
            }
        }
        
        if (!results.isEmpty()) {
            entity.getObjectMetadata().put(new AttributesMapContainer<>(results));
        }
    }
  //CheckStyle: ReturnCount ON

    /**
     * Access the registry of transcoding rules to decode the input object.
     * 
     * @param <T> input type
     * @param registry  registry of transcoding rules
     * @param input input object
     * @param results collection to add results to
     * 
     * @throws AttributeDecodingException if an error occurs or no results were obtained
     */
    protected <T> void decodeAttribute(@Nonnull final AttributeTranscoderRegistry registry,
            @Nonnull final T input, @Nonnull @NonnullElements @Live final Multimap<String,IdPAttribute> results)
                    throws AttributeDecodingException {
        
        final Collection<TranscodingRule> rulesets = registry.getTranscodingRules(input);
        
        for (final TranscodingRule rules : rulesets) {
            final AttributeTranscoder<T> transcoder = TranscoderSupport.getTranscoder(rules);
            final IdPAttribute decodedAttribute = transcoder.decode(null, input, rules);
            if (decodedAttribute != null) {
                results.put(decodedAttribute.getId(), decodedAttribute);
            }
        }
    }

}