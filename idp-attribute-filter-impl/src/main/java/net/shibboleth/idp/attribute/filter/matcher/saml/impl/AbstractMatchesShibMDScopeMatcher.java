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

package net.shibboleth.idp.attribute.filter.matcher.saml.impl;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.opensaml.saml.common.messaging.context.SAMLMetadataContext;
import org.opensaml.saml.saml2.metadata.EntityDescriptor;
import org.opensaml.saml.saml2.metadata.RoleDescriptor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.shibboleth.idp.attribute.IdPAttribute;
import net.shibboleth.idp.attribute.IdPAttributeValue;
import net.shibboleth.idp.attribute.filter.Matcher;
import net.shibboleth.idp.attribute.filter.context.AttributeFilterContext;
import net.shibboleth.idp.saml.metadata.ScopesContainer;
import net.shibboleth.utilities.java.support.annotation.constraint.NonnullElements;
import net.shibboleth.utilities.java.support.annotation.constraint.NotEmpty;
import net.shibboleth.utilities.java.support.annotation.constraint.Unmodifiable;
import net.shibboleth.utilities.java.support.component.AbstractIdentifiableInitializableComponent;
import net.shibboleth.utilities.java.support.component.ComponentInitializationException;

/**
 * Base class for filters which rely on the issuer's &lt;shibmd:scope&gt; extensions.
 */

public abstract class AbstractMatchesShibMDScopeMatcher
                extends AbstractIdentifiableInitializableComponent implements Matcher {

    /** Class logger. */
    private static final Logger LOG = LoggerFactory.getLogger(AbstractMatchesShibMDScopeMatcher.class);

    /** The String used to prefix log message. */
    private String logPrefix;

    /** {@inheritDoc} */
    @Override protected void doInitialize() throws ComponentInitializationException {
        super.doInitialize();
        // Id is now definitive, reset log prefix
        logPrefix = null;
    }
    
    /**
     * {@inheritDoc}
     */
    // CheckStyle: CyclomaticComplexity OFF
    @Override @Nonnull @NonnullElements @Unmodifiable public Set<IdPAttributeValue> getMatchingValues(
            @Nonnull final IdPAttribute attribute, @Nonnull final AttributeFilterContext filterContext) {

        final SAMLMetadataContext issuerContext = filterContext.getIssuerMetadataContext();
        if (issuerContext == null) {
            LOG.warn("{} internal error: no IssueContext found",
                    getLogPrefix());
            return Collections.emptySet();
        }
        final RoleDescriptor roleDescriptor = issuerContext.getRoleDescriptor();
        final EntityDescriptor entityDescriptor = issuerContext.getEntityDescriptor();
        final List<ScopesContainer> roleContainers;
        if (roleDescriptor == null) {
            LOG.debug("{} No Role Descriptor found");
            roleContainers = Collections.emptyList();
        } else {
            roleContainers = roleDescriptor.getObjectMetadata().get(ScopesContainer.class);
        }
        
        final List<ScopesContainer> entityContainers;
        if (entityDescriptor == null) {
            LOG.debug("{} No Entity Descriptor found");
            entityContainers = Collections.emptyList(); 
        } else {
            entityContainers= entityDescriptor.getObjectMetadata().get(ScopesContainer.class);
        }
        final Set<IdPAttributeValue> matchedValues = new LinkedHashSet<>(attribute.getValues().size());
        LOG.debug("{} Applying shibmd scope comparison to all values of Attribute '{}'",
                getLogPrefix(),
                attribute.getId());

        if (entityContainers.isEmpty() && roleContainers.isEmpty()) {
            LOG.debug("{} No <shibmd:Scope> found for {}, no atributes matched",
                    getLogPrefix(),
                    entityDescriptor == null? "<unknown>" : entityDescriptor.getID());
            return Collections.emptySet();
        }
        
        for (final IdPAttributeValue value : attribute.getValues()) {
            final String compareString = getCompareString(value);
            
            if (compareString == null) {
                LOG.trace("{} not adding null-valued value {}", getLogPrefix(), value.getNativeValue().toString());
            } else {
                if (roleContainers.stream().anyMatch(e -> e.matchesScope(compareString))||
                    entityContainers.stream().anyMatch(e -> e.matchesScope(compareString))) {
                    LOG.trace("{} {} matches, adding value {}", getLogPrefix(), compareString,
                            value.getNativeValue().toString());
                    matchedValues.add(value);
                }
            }
        }
        LOG.debug("{} returning {} values", getLogPrefix(), matchedValues.size());
        return Collections.unmodifiableSet(matchedValues);
    }
    // CheckStyle: CyclomaticComplexity ON

    /** Return the string we want to compare with for the provided value. 
     * @param value the vaue we are interested.
     * @return the string, or null if empty of not relevant.
     */
    @Nullable @NotEmpty protected abstract String getCompareString(IdPAttributeValue value);

    /**
     * Return a string which is to be prepended to all log messages.
     * 
     * @return "Attribute Filter '<filterID>' :"
     */
    protected String getLogPrefix() {
        // local cache of cached entry to allow unsynchronised clearing.
        String prefix = logPrefix;
        if (null == prefix) {
            final StringBuilder builder = new StringBuilder("Attribute Filter '").append(getId()).append("':");
            prefix = builder.toString();
            if (null == logPrefix) {
                logPrefix = prefix;
            }
        }
        return prefix;
    }

}
