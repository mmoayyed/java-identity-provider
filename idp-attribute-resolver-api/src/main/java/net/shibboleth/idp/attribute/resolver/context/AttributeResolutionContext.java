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

package net.shibboleth.idp.attribute.resolver.context;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.concurrent.NotThreadSafe;

import net.shibboleth.idp.attribute.IdPAttribute;
import net.shibboleth.idp.attribute.resolver.AttributeResolver;
import net.shibboleth.idp.attribute.resolver.ResolutionException;
import net.shibboleth.utilities.java.support.annotation.constraint.Live;
import net.shibboleth.utilities.java.support.annotation.constraint.NonnullElements;
import net.shibboleth.utilities.java.support.annotation.constraint.NotLive;
import net.shibboleth.utilities.java.support.annotation.constraint.NullableElements;
import net.shibboleth.utilities.java.support.annotation.constraint.Unmodifiable;
import net.shibboleth.utilities.java.support.logic.Constraint;
import net.shibboleth.utilities.java.support.primitive.StringSupport;
import net.shibboleth.utilities.java.support.service.ReloadableService;
import net.shibboleth.utilities.java.support.service.ServiceableComponent;

import org.opensaml.messaging.context.BaseContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Predicates;
import com.google.common.collect.Collections2;
import com.google.common.collect.ImmutableMap;

/** A context supplying input to the {@link net.shibboleth.idp.attribute.resolver.AttributeResolver} interface. */
@NotThreadSafe
public final class AttributeResolutionContext extends BaseContext {

    /** (internal) Names of the attributes that have been requested to be resolved. */
    @Nonnull @NonnullElements private Set<String> requestedAttributeNames;

    /** The principal associated with this resolution. */
    @Nullable private String principal;

    /** The attribute source identity. */
    @Nullable private String attributeIssuerID;

    /** The attribute recipient identity. */
    @Nullable private String attributeRecipientID;

    /** The attribute recipient's group identity. */
    @Nullable private String attributeRecipientGroupID;

    /** How was the principal Authenticated? */
    @Deprecated
    @Nullable private String principalAuthenticationMethod;
    
    /** Whether the resolver should allow for results to come from cache. */
    private boolean allowCachedResults;
    
    /** Label distinguishing different "types" of attribute resolution for use in resolver. */
    @Nullable private String resolutionLabel;

    /** Attributes which were resolved and released by the attribute resolver. */
    @Nonnull @NonnullElements private Map<String,IdPAttribute> resolvedAttributes;
    
    /** Constructor. */
    public AttributeResolutionContext() {
        allowCachedResults = true;
        requestedAttributeNames = new HashSet<>();
        resolvedAttributes = new HashMap<>();
    }
    
    /**
     * Get whether to allow for results from cache (defaults to true).
     * 
     * @return whether to allow for results from cache
     * 
     * @since 3.3.0
     */
    public boolean getAllowCachedResults() {
        return allowCachedResults;
    }
    
    /**
     * Set whether to allow for results from cache.
     * 
     * @param flag flag to set
     * 
     * @return this context 
     * 
     * @since 3.3.0
     */
    @Nonnull public AttributeResolutionContext setAllowCachedResults(final boolean flag) {
        allowCachedResults = flag;
        
        return this;
    }
    
    /**
     * Get the optional "contextual" label associated with this attribute resolution.
     * 
     * <p>Plugins/scripts/etc. can use this field to connect their behavior back to custom
     * invocations of the resolver service.</p>
     * 
     * @return label
     * 
     * @since 3.4.0
     */
    @Nullable public String getResolutionLabel() {
        return resolutionLabel;
    }
    
    /**
     * Set the optional "contextual" label associated with this attribute resolution.
     * 
     * @param label label to set
     * 
     * @return this context
     * 
     * @since 3.4.0
     */
    @Nonnull public AttributeResolutionContext setResolutionLabel(@Nullable final String label) {
        resolutionLabel = StringSupport.trimOrNull(label);
        
        return this;
    }

    /**
     * Get the attribute issuer (me) associated with this resolution.
     * 
     * @return the attribute issuer associated with this resolution.
     */
    @Nullable public String getAttributeIssuerID() {
        return attributeIssuerID;
    }

    /**
     * Set the attribute issuer (me) associated with this resolution.
     * 
     * @param value the attribute issuer associated with this resolution.
     * 
     * @return this context
     */
    @Nullable public AttributeResolutionContext setAttributeIssuerID(@Nullable final String value) {
        attributeIssuerID = value;
        
        return this;
    }

    /**
     * Get the attribute recipient (her) associated with this resolution.
     * 
     * @return the attribute recipient associated with this resolution.
     */
    @Nullable public String getAttributeRecipientID() {
        return attributeRecipientID;
    }

    /**
     * Set the attribute recipient (her) associated with this resolution.
     * 
     * @param value the attribute recipient associated with this resolution.
     * 
     * @return this context
     */
    @Nullable public AttributeResolutionContext setAttributeRecipientID(@Nullable final String value) {
        attributeRecipientID = value;
        
        return this;
    }

    /**
     * Get the attribute recipient grouping associated with this resolution.
     * 
     * <p>This is a protocol-independent way to represent an association between the attribute recipient
     * and some larger group that may be relevant to attribute resolution.</p>
     * 
     * @return the attribute recipient group associated with this resolution
     * 
     * @since 3.4.0
     */
    @Nullable public String getAttributeRecipientGroupID() {
        return attributeRecipientGroupID;
    }

    /**
     * Set the attribute recipient grouping associated with this resolution.
     * 
     * @param value the attribute recipient group associated with this resolution
     * 
     * @return this context
     * 
     * @since 3.4.0
     */
    @Nullable public AttributeResolutionContext setAttributeRecipientGroupID(@Nullable final String value) {
        attributeRecipientGroupID = value;
        
        return this;
    }

    /**
     * Set the principal associated with this resolution.
     * 
     * @return Returns the principal.
     */
    @Nullable public String getPrincipal() {
        return principal;
    }

    /**
     * Get the principal associated with this resolution.
     * 
     * @param who the principal to set.
     * 
     * @return this context
     */
    @Nullable public AttributeResolutionContext setPrincipal(@Nullable final String who) {
        principal = who;
        
        return this;
    }

    /**
     * Get a live collection of the (internal) names of the attributes requested to be resolved.
     * 
     * @return live collection of attributes requested to be resolved
     */
    @Nonnull @NonnullElements @Live public Collection<String> getRequestedIdPAttributeNames() {
        return requestedAttributeNames;
    }

    /**
     * Set the (internal) names of the attributes requested to be resolved.
     * 
     * @param names the (internal) names of the attributes requested to be resolved
     * 
     * @return this context
     */
    @Nullable public AttributeResolutionContext setRequestedIdPAttributeNames(
            @Nonnull @NonnullElements final Collection<String> names) {
        Constraint.isNotNull(names, "Requested IdPAttribute collection cannot be null");

        requestedAttributeNames.clear();
        requestedAttributeNames.addAll(Collections2.filter(names, Predicates.notNull()));
        
        return this;
    }

    /**
     * Get the collection of resolved attributes.
     * 
     * @return set of resolved attributes
     */
    @Nonnull @NonnullElements @Unmodifiable @NotLive public Map<String, IdPAttribute> getResolvedIdPAttributes() {
        return ImmutableMap.copyOf(resolvedAttributes);
    }

    /**
     * Set the set of resolved attributes.
     * 
     * @param attributes set of resolved attributes
     * 
     * @return this context
     */
    @Nullable public AttributeResolutionContext setResolvedIdPAttributes(
            @Nullable @NullableElements final Collection<IdPAttribute> attributes) {
        resolvedAttributes = new HashMap<>();

        if (attributes != null) {
            for (final IdPAttribute attribute : attributes) {
                if (attribute != null) {
                    resolvedAttributes.put(attribute.getId(), attribute);
                }
            }
        }
        
        return this;
    }

    /**
     * Helper method to invoke an AttributeResolver service using this context.
     * 
     * @param attributeResolverService the service to invoke
     * 
     * @since 3.3.0
     */
    public void resolveAttributes(@Nonnull final ReloadableService<AttributeResolver> attributeResolverService) {

        final Logger log = LoggerFactory.getLogger(AttributeResolutionContext.class);
        ServiceableComponent<AttributeResolver> component = null;
        try {
            component = attributeResolverService.getServiceableComponent();
            if (null == component) {
                log.error("Error resolving attributes: Invalid Attribute resolver configuration");
            } else {
                final AttributeResolver attributeResolver = component.getComponent();
                attributeResolver.resolveAttributes(this);
            }
        } catch (final ResolutionException e) {
            log.error("Error resolving attributes", e);
        } finally {
            if (null != component) {
                component.unpinComponent();
            }
        }
    }
}