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

package net.shibboleth.idp.attribute.filter.context;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.concurrent.NotThreadSafe;

import org.opensaml.messaging.context.BaseContext;
import org.opensaml.profile.context.ProxiedRequesterContext;
import org.opensaml.saml.common.messaging.context.SAMLMetadataContext;
import org.opensaml.saml.metadata.resolver.MetadataResolver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.shibboleth.idp.attribute.IdPAttribute;
import net.shibboleth.idp.attribute.filter.AttributeFilter;
import net.shibboleth.idp.attribute.filter.AttributeFilterException;
import net.shibboleth.utilities.java.support.annotation.constraint.NonnullElements;
import net.shibboleth.utilities.java.support.annotation.constraint.NotLive;
import net.shibboleth.utilities.java.support.annotation.constraint.Unmodifiable;
import net.shibboleth.utilities.java.support.logic.Constraint;
import net.shibboleth.utilities.java.support.service.ReloadableService;
import net.shibboleth.utilities.java.support.service.ServiceableComponent;

/** A context supplying input to the {@link net.shibboleth.idp.attribute.filter.AttributeFilter} interface. */
@NotThreadSafe
public final class AttributeFilterContext extends BaseContext {

    /**
     * Used to indicate the "direction" of filtering relative to the IdP.
     * 
     * @since 4.0.0
     */
    public enum Direction {
        /** Inbound filtering is used to control the acceptance of data from another party. */
        INBOUND, 
        
        /** Outbound filtering is used to control the release of data to another party. */
        OUTBOUND,
        };
    
    /** Direction of filtering. */
    @Nonnull private Direction direction;
        
    /** Attributes which are to be filtered. */
    @Nonnull private Map<String,IdPAttribute> prefilteredAttributes;

    /** Attributes which have been filtered. */
    @Nonnull private Map<String,IdPAttribute> filteredAttributes;

    /** Optional, supplemental metadata resolver. */
    @Nullable private MetadataResolver metadataResolver;
        
    /** The principal associated with the filtering. */
    @Nullable private String principal;

    /** The attribute source identity. */
    @Nullable private String attributeIssuerID;

    /** The attribute recipient identity. */
    @Nullable private String attributeRecipientID;

    /** The attribute recipient's group identity. */
    @Nullable private String attributeRecipientGroupID;

    /** Cache of IdP metadata context. */
    @Nullable private SAMLMetadataContext issuerMetadataContext;
    
    /** Cache of SP metadata context. */
    @Nullable private SAMLMetadataContext requesterMetadataContext;

    /** Cache of the proxied requester context. */
    @Nullable private ProxiedRequesterContext proxiedRequesterContext;

    /** Lookup strategy used to locate the IdP's metadata context. */
    @Nullable
    private Function<AttributeFilterContext,SAMLMetadataContext> issuerMetadataContextLookupStrategy;

    /** Lookup strategy used to locate the SP's metadata context. */
    @Nullable
    private Function<AttributeFilterContext,SAMLMetadataContext> requesterMetadataContextLookupStrategy;

    /** Lookup strategy used to locate a {@link ProxiedRequesterContext}. */
    @Nullable
    private Function<AttributeFilterContext,ProxiedRequesterContext> proxiedRequesterContextLookupStrategy;

    /** Constructor. */
    public AttributeFilterContext() {
        prefilteredAttributes = new HashMap<>();
        filteredAttributes = new HashMap<>();
        
        direction = Direction.OUTBOUND;
    }
    
    /**
     * Gets the direction of filtering.
     * 
     * @return the direction
     * 
     * @since 4.0.0
     */
    @Nonnull public Direction getDirection() {
        return direction;
    }
    
    /**
     * Sets the direction of filtering.
     * 
     * @param dir the direction
     * 
     * @return this context
     * 
     * @since 4.0.0
     */
    @Nonnull public AttributeFilterContext setDirection(@Nonnull final Direction dir) {
        direction = Constraint.isNotNull(dir, "Direction cannot be null");
        
        return this;
    }

    /**
     * Gets the collection of attributes that are to be filtered, indexed by attribute ID.
     * 
     * @return attributes to be filtered
     */
    @Nonnull @NonnullElements @NotLive @Unmodifiable public Map<String,IdPAttribute> getPrefilteredIdPAttributes() {
        return prefilteredAttributes;
    }

    /**
     * Sets the attributes which are to be filtered.
     * 
     * @param attributes attributes which are to be filtered
     * 
     * @return this context;
     */
    @Nonnull public AttributeFilterContext setPrefilteredIdPAttributes(
            @Nullable @NonnullElements final Collection<IdPAttribute> attributes) {

        if (attributes != null) {
            prefilteredAttributes = attributes.
                    stream().
                    collect(Collectors.toUnmodifiableMap(IdPAttribute::getId, e -> e));
        } else {
            prefilteredAttributes = Collections.emptyMap();
        }
        
        return this;
    }

    /**
     * Gets the collection of attributes, indexed by ID, left after the filtering process has run.
     * 
     * @return attributes left after the filtering process has run
     */
    @Nonnull @NonnullElements @Unmodifiable @NotLive public Map<String, IdPAttribute> getFilteredIdPAttributes() {
        return filteredAttributes;
    }

    /**
     * Sets the attributes that have been filtered.
     * 
     * @param attributes attributes that have been filtered
     * 
     * @return this context
     */
    @Nonnull public AttributeFilterContext setFilteredIdPAttributes(
            @Nullable @NonnullElements final Collection<IdPAttribute> attributes) {

        if (attributes != null) {
            filteredAttributes = attributes.
                    stream().
                    collect(Collectors.toUnmodifiableMap(IdPAttribute::getId, e -> e));
        } else {
            filteredAttributes = Collections.emptyMap();
        }
        
        return this;
    }
    
    /**
     * Get supplemental source of metadata for filtering rules.
     * 
     * @return metadata resolver
     * 
     * @since 3.4.0
     */
    @Nullable public MetadataResolver getMetadataResolver() {
        return metadataResolver;
    }
    
    /**
     * Set supplemental source of metadata for filtering rules.
     * 
     * @param resolver metadata resolver
     * 
     * @return this context
     * 
     * @since 3.4.0
     */
    @Nonnull public AttributeFilterContext setMetadataResolver(@Nullable final MetadataResolver resolver) {
        metadataResolver = resolver;
        
        return this;
    }

    /**
     * Sets the principal associated with the filtering.
     * 
     * @return Returns the principal.
     */
    @Nullable public String getPrincipal() {
        return principal;
    }

    /**
     * Gets the principal associated with the filtering.
     * 
     * @param who principal to set
     * 
     * @return this context
     */
    @Nonnull public AttributeFilterContext setPrincipal(@Nullable final String who) {
        principal = who;
        
        return this;
    }

    /**
     * Gets the attribute issuer (me) associated with this filtering.
     * 
     * @return the attribute issuer associated with this filtering
     */
    @Nullable public String getAttributeIssuerID() {
        return attributeIssuerID;
    }

    /**
     * Sets the attribute issuer (me) associated with this filtering.
     * 
     * @param value the attribute issuer associated with this filtering
     * 
     * @return this context
     */
    @Nonnull public AttributeFilterContext setAttributeIssuerID(@Nullable final String value) {
        attributeIssuerID = value;
        
        return this;
    }

    /**
     * Gets the attribute recipient (her) associated with this filtering.
     * 
     * @return the attribute recipient associated with this filtering
     */
    @Nullable public String getAttributeRecipientID() {
        return attributeRecipientID;
    }

    /**
     * Sets the attribute recipient (her) associated with this filtering.
     * 
     * @param value the attribute recipient associated with this filtering
     * 
     * @return this context
     */
    @Nonnull public AttributeFilterContext setAttributeRecipientID(@Nullable final String value) {
        attributeRecipientID = value;
        
        return this;
    }

    /**
     * Get the attribute recipient grouping associated with this filtering.
     * 
     * <p>This is a protocol-independent way to represent an association between the attribute recipient
     * and some larger group that may be relevant to attribute filtering.</p>
     * 
     * @return the attribute recipient group associated with this filtering
     * 
     * @since 3.4.0
     */
    @Nullable public String getAttributeRecipientGroupID() {
        return attributeRecipientGroupID;
    }

    /**
     * Set the attribute recipient grouping associated with this filtering.
     * 
     * @param value the attribute recipient group associated with this filtering
     * 
     * @return this context
     * 
     * @since 3.4.0
     */
    @Nonnull public AttributeFilterContext setAttributeRecipientGroupID(@Nullable final String value) {
        attributeRecipientGroupID = value;
        
        return this;
    }

    /**
     * Get the strategy used to locate the IdP's metadata context.
     * 
     * @return lookup strategy
     */
    @Nullable public Function<AttributeFilterContext,SAMLMetadataContext> getRequesterMetadataContextLookupStrategy() {
        return requesterMetadataContextLookupStrategy;
    }

    /**
     * Set the strategy used to locate the IdP's metadata context.
     * 
     * @param strategy lookup strategy
     * 
     * @return this context
     * 
     * @since 4.0.0
     */
    @Nonnull public AttributeFilterContext setIssuerMetadataContextLookupStrategy(
            @Nullable final Function<AttributeFilterContext,SAMLMetadataContext> strategy) {
        issuerMetadataContextLookupStrategy = strategy;
        
        return this;
    }
    
    /**
     * Get the strategy used to locate the IdP's metadata context.
     * 
     * @return lookup strategy
     * 
     * @since 4.0.0
     */
    @Nullable public Function<AttributeFilterContext,SAMLMetadataContext> getIssuerMetadataContextLookupStrategy() {
        return issuerMetadataContextLookupStrategy;
    }

    /**
     * Set the strategy used to locate the SP's metadata context.
     * 
     * @param strategy lookup strategy
     * 
     * @return this context
     */
    @Nonnull public AttributeFilterContext setRequesterMetadataContextLookupStrategy(
            @Nullable final Function<AttributeFilterContext,SAMLMetadataContext> strategy) {
        requesterMetadataContextLookupStrategy = strategy;
        
        return this;
    }

    /**
     * Get the strategy used to locate the {@link ProxiedRequesterContext}.
     * 
     * @return  lookup strategy
     * 
     * @since 3.4.0
     */
    @Nullable
    public Function<AttributeFilterContext,ProxiedRequesterContext> getProxiedRequesterContextLookupStrategy() {
        return proxiedRequesterContextLookupStrategy;
    }

    /**
     * Set the strategy used to locate the SP's metadata context.
     * 
     * @param strategy lookup strategy
     * 
     * @return this context
     * 
     * @since 3.4.0
     */
    @Nonnull public AttributeFilterContext setProxiedRequesterContextLookupStrategy(
            @Nullable final Function<AttributeFilterContext,ProxiedRequesterContext> strategy) {
        proxiedRequesterContextLookupStrategy = strategy;
        
        return this;
    }

    /** Get the Issuer Metadata context.
     * 
     * <p>This value is cached and so only calculated once.</p>
     * 
     * @return the context
     * 
     * @since 4.0.0
     */
    @Nullable public SAMLMetadataContext getIssuerMetadataContext() {
        if (null == issuerMetadataContext && null != issuerMetadataContextLookupStrategy) {
            issuerMetadataContext = issuerMetadataContextLookupStrategy.apply(this);
        }
        return issuerMetadataContext;
    }

    /** Get the Requester Metadata context.
     * 
     * <p>This value is cached and so only calculated once.</p>
     * 
     * @return the context
     */
    @Nullable public SAMLMetadataContext getRequesterMetadataContext() {
        if (null == requesterMetadataContext && null != requesterMetadataContextLookupStrategy) {
            requesterMetadataContext = requesterMetadataContextLookupStrategy.apply(this);
        }
        return requesterMetadataContext;
    }
    
    /** Get the {@link ProxiedRequesterContext}.
     * 
     * <p>This value is cached and so only calculated once.</p>
     * 
     * @return the context
     * 
     * @since 3.4.0
     */
    @Nullable public ProxiedRequesterContext getProxiedRequesterContext() {
        if (null == proxiedRequesterContext && null != proxiedRequesterContextLookupStrategy) {
            proxiedRequesterContext = proxiedRequesterContextLookupStrategy.apply(this);
        }
        return proxiedRequesterContext;
    }

    /**
     * Helper method to invoke an AttributeFilter service using this context.
     * 
     * @param attributeFilterService the service to invoke
     * 
     * @since 4.0.0
     */
    public void filterAttributes(@Nonnull final ReloadableService<AttributeFilter> attributeFilterService) {

        final Logger log = LoggerFactory.getLogger(AttributeFilterContext.class);
        ServiceableComponent<AttributeFilter> component = null;
        try {
            component = attributeFilterService.getServiceableComponent();
            if (null == component) {
                log.error("Error filtering attributes: Invalid Attribute filter configuration");
            } else {
                component.getComponent().filterAttributes(this);
            }
        } catch (final AttributeFilterException e) {
            log.error("Error filtering attributes", e);
        } finally {
            if (null != component) {
                component.unpinComponent();
            }
        }
    }

}