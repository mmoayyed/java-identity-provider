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

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.concurrent.NotThreadSafe;

import net.shibboleth.idp.attribute.IdPAttribute;
import net.shibboleth.utilities.java.support.annotation.constraint.NonnullAfterInit;
import net.shibboleth.utilities.java.support.annotation.constraint.NonnullElements;
import net.shibboleth.utilities.java.support.annotation.constraint.NullableElements;
import net.shibboleth.utilities.java.support.collection.CollectionSupport;
import net.shibboleth.utilities.java.support.logic.Constraint;

import org.opensaml.messaging.context.BaseContext;
import org.opensaml.profile.context.ProxiedRequesterContext;
import org.opensaml.saml.common.messaging.context.SAMLMetadataContext;
import org.opensaml.saml.metadata.resolver.MetadataResolver;

import com.google.common.base.Predicates;

/** A context supplying input to the {@link net.shibboleth.idp.attribute.filter.AttributeFilter} interface. */
@NotThreadSafe
public final class AttributeFilterContext extends BaseContext {

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
    
    /** How was the principal Authenticated? */
    @Deprecated
    @Nullable private String principalAuthenticationMethod;

    /** Cache of the metadata context. */
    @Nullable private SAMLMetadataContext requesterMetadataContext;

    /** Cache of the proxied requester context. */
    @Nullable private ProxiedRequesterContext proxiedRequesterContext;

    /** Lookup strategy used to locate the SP's metadata context. */
    @Nullable
    private Function<AttributeFilterContext,SAMLMetadataContext> requesterMetadataContextLookupStrategy;

    /** Lookup strategy used to locate a {@link ProxiedRequesterContext}. */
    @Nullable
    private Function<AttributeFilterContext,ProxiedRequesterContext> proxiedRequesterContextLookupStrategy;

    /** Constructor. */
    public AttributeFilterContext() {
        prefilteredAttributes = new HashMap<String, IdPAttribute>();
        filteredAttributes = new HashMap<String, IdPAttribute>();
    }

    /**
     * Gets the collection of attributes that are to be filtered, indexed by attribute ID.
     * 
     * @return attributes to be filtered
     */
    @Nonnull @NonnullElements public Map<String, IdPAttribute> getPrefilteredIdPAttributes() {
        return prefilteredAttributes;
    }

    /**
     * Sets the attributes which are to be filtered.
     * 
     * @param attributes attributes which are to be filtered
     */
    public void setPrefilteredIdPAttributes(@Nullable @NullableElements final Collection<IdPAttribute> attributes) {
        final Collection<IdPAttribute> checkedAttributes = new ArrayList<>();
        CollectionSupport.addIf(checkedAttributes, attributes, Predicates.notNull());

        prefilteredAttributes = new HashMap<String, IdPAttribute>(checkedAttributes.size());

        for (final IdPAttribute attribute : checkedAttributes) {
            prefilteredAttributes.put(attribute.getId(), attribute);
        }
    }

    /**
     * Gets the collection of attributes, indexed by ID, left after the filtering process has run.
     * 
     * @return attributes left after the filtering process has run
     */
    @Nonnull @NonnullElements public Map<String, IdPAttribute> getFilteredIdPAttributes() {
        return filteredAttributes;
    }

    /**
     * Sets the attributes that have been filtered.
     * 
     * @param attributes attributes that have been filtered
     */
    public void setFilteredIdPAttributes(@Nullable @NullableElements final Collection<IdPAttribute> attributes) {
        final Collection<IdPAttribute> checkedAttributes = new ArrayList<>();
        CollectionSupport.addIf(checkedAttributes, attributes, Predicates.notNull());

        filteredAttributes = new HashMap<String, IdPAttribute>(checkedAttributes.size());

        for (final IdPAttribute attribute : checkedAttributes) {
            filteredAttributes.put(attribute.getId(), attribute);
        }
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
     * @since 3.4.0
     */
    public void setMetadataResolver(@Nullable final MetadataResolver resolver) {
        metadataResolver = resolver;
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
     * @param who The principal to set.
     */
    public void setPrincipal(@Nullable final String who) {
        principal = who;
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
     */
    @Nullable public void setAttributeIssuerID(@Nullable final String value) {
        attributeIssuerID = value;
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
     */
    @Nullable public void setAttributeRecipientID(@Nullable final String value) {
        attributeRecipientID = value;
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
    @Nullable public AttributeFilterContext setAttributeRecipientGroupID(@Nullable final String value) {
        attributeRecipientGroupID = value;
        
        return this;
    }
    
    /**
     * Get the strategy used to locate the SP's metadata context.
     * 
     * @return lookup strategy
     */
    @NonnullAfterInit
    public Function<AttributeFilterContext, SAMLMetadataContext> getRequesterMetadataContextLookupStrategy() {
        return requesterMetadataContextLookupStrategy;
    }

    /**
     * Set the strategy used to locate the SP's metadata context.
     * 
     * @param strategy lookup strategy
     */
    public void setRequesterMetadataContextLookupStrategy(
            @Nonnull final Function<AttributeFilterContext,SAMLMetadataContext> strategy) {
        requesterMetadataContextLookupStrategy =
                Constraint.isNotNull(strategy, "MetadataContext lookup strategy cannot be null");
    }

    /**
     * Get the strategy used to locate the {@link ProxiedRequesterContext}.
     * 
     * @return  lookup strategy
     * 
     * @since 3.4.0
     */
    @NonnullAfterInit
    public Function<AttributeFilterContext,ProxiedRequesterContext> getProxiedRequesterContextLookupStrategy() {
        return proxiedRequesterContextLookupStrategy;
    }

    /**
     * Set the strategy used to locate the SP's metadata context.
     * 
     * @param strategy lookup strategy
     * 
     * @since 3.4.0
     */
    public void setProxiedRequesterContextLookupStrategy(
            @Nonnull final Function<AttributeFilterContext,ProxiedRequesterContext> strategy) {
        proxiedRequesterContextLookupStrategy =
                Constraint.isNotNull(strategy, "ProxiedRequesterContext lookup strategy cannot be null");
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

}