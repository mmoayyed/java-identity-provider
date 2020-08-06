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

package net.shibboleth.idp.attribute.resolver;

import java.time.Duration;
import java.time.Instant;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Set;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.concurrent.ThreadSafe;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.shibboleth.idp.attribute.IdPAttribute;
import net.shibboleth.idp.attribute.resolver.context.AttributeResolutionContext;
import net.shibboleth.idp.attribute.resolver.context.AttributeResolverWorkContext;
import net.shibboleth.utilities.java.support.annotation.constraint.NonnullElements;
import net.shibboleth.utilities.java.support.annotation.constraint.NotEmpty;
import net.shibboleth.utilities.java.support.annotation.constraint.Unmodifiable;
import net.shibboleth.utilities.java.support.component.ComponentInitializationException;
import net.shibboleth.utilities.java.support.component.ComponentSupport;
import net.shibboleth.utilities.java.support.primitive.StringSupport;

/** Base class for data connector resolver plugins. */
@ThreadSafe
public abstract class AbstractDataConnector extends AbstractResolverPlugin<Map<String, IdPAttribute>> implements
        DataConnector {

    /** Class logger. */
    @Nonnull private final Logger log = LoggerFactory.getLogger(AbstractDataConnector.class);

    /** ID of the data connector to use if this one fails. */
    @Nullable private String failoverDataConnectorId;

    /** cache for the log prefix - to save multiple recalculations. */
    @Nullable private String logPrefix;

    /** When did this connector last fail? */
    @Nullable private Instant lastFail;

    /** How long to wait until we declare the connector live again. */
    @Nonnull private Duration noRetryDelay;

    /** Do we release all attributes?. */
    @Deprecated(since = "4.1.0", forRemoval = true)
    private boolean exportAllAttributes;

    /** Which named attributes do we release?. */
    @Nonnull @NonnullElements @Unmodifiable private Collection<String> exportAttributes;

    /** Constructor. */
    public AbstractDataConnector() {
        noRetryDelay = Duration.ZERO; 
        exportAttributes = Collections.emptySet();
    }
    
    /**
     * Gets the ID of the {@link AbstractDataConnector} whose values will be used in the event that this data connector
     * experiences an error.
     *
     * @return ID of the {@link AbstractDataConnector} whose values will be used in the event that this data connector
     *         experiences an error
     */
    @Override @Nullable public String getFailoverDataConnectorId() {
        return failoverDataConnectorId;
    }

    /**
     * Set the ID of the {@link AbstractDataConnector} whose values will be used in the event that this data connector
     * experiences an error.
     *
     * @param id ID of the {@link AbstractDataConnector} whose values will be used in the event that this data connector
     *            experiences an error
     */
    public void setFailoverDataConnectorId(@Nullable final String id) {
        ComponentSupport.ifInitializedThrowUnmodifiabledComponentException(this);
        ComponentSupport.ifDestroyedThrowDestroyedComponentException(this);

        failoverDataConnectorId = StringSupport.trimOrNull(id);
    }

    /**
     * Set the time when this connector last failed.
     *
     * @param time what to set
     */
    public void setLastFail(@Nullable final Instant time) {
        lastFail = time;
    }

    /**
     * {@inheritDoc}
     */
    @Override @Nullable public Instant getLastFail() {
        return lastFail;
    }

    /**
     * Set how long to wait until we declare the connector (potentially) alive again.
     *
     * @param delay what to set
     */
    public void setNoRetryDelay(@Nonnull final Duration delay) {
        noRetryDelay = delay;
    }

    /** {@inheritDoc} */
    @Override @Nonnull public Duration getNoRetryDelay() {
        return noRetryDelay;
    }

    /**
     * Set whether we export all attributes.
     *
     * @param what whether we export all attributes
     */
    @Deprecated(since = "4.1.0", forRemoval = true)
    public void setExportAllAttributes(final boolean what) {
        ComponentSupport.ifDestroyedThrowDestroyedComponentException(this);
        ComponentSupport.ifInitializedThrowUnmodifiabledComponentException(this);
        exportAllAttributes = what;
    }

    /**
     * {@inheritDoc}
     */
    @Deprecated(since = "4.1.0", forRemoval = true)
    @Override public boolean isExportAllAttributes() {
        return exportAllAttributes;
    }

    /**
     *  Sets the list of attribute names to export during resolution.
     *
     * @param what the list
     */
    public void setExportAttributes(@Nonnull @NonnullElements final Collection<String> what) {
        ComponentSupport.ifDestroyedThrowDestroyedComponentException(this);
        ComponentSupport.ifInitializedThrowUnmodifiabledComponentException(this);
        exportAttributes = Set.copyOf(what);
    }

    /**
     * {@inheritDoc}
     */
    @Override @Nonnull @NonnullElements @Unmodifiable public Collection<String> getExportAttributes() {
        return exportAttributes;
    }


    /**
     * {@inheritDoc}
     * 
     * This method delegates to
     * {@link #doDataConnectorResolve(AttributeResolutionContext, AttributeResolverWorkContext)}. It serves as a future
     * extension point for introducing new common behavior.
     */
    @Override
    @Nullable public final Map<String, IdPAttribute> doResolve(
            @Nonnull final AttributeResolutionContext resolutionContext,
            @Nonnull final AttributeResolverWorkContext workContext) throws ResolutionException {

        final Map<String, IdPAttribute> result;
        try {
            result = doDataConnectorResolve(resolutionContext, workContext);
        } catch (final NoResultAnErrorResolutionException | MultipleResultAnErrorResolutionException e) {
            // Do not record these failures, they are 'expected'
            throw e;
        } catch (final Exception e) {
            setLastFail(Instant.now());
            throw e;
        }

        if (null == result) {
            log.debug("{} no attributes were produced during resolution", getId());
            return result;
        }
        log.debug("{} produced the following {} attributes during resolution {}", new Object[] {getLogPrefix(),
                result.size(), result.keySet(),});
        for (final String attrName : result.keySet()) {
            final IdPAttribute attr = result.get(attrName);
            log.debug("{} Attribute '{}': Values '{}'", new Object[] {getLogPrefix(), attrName, attr.getValues(),});
        }

        return result;
    }

    /** {@inheritDoc} */
    @Override protected void doInitialize() throws ComponentInitializationException {

        super.doInitialize();

        // The Id is now definitive. Just in case it was used prior to that, reset the getPrefixCache
        logPrefix = null;
    }

    /**
     * Retrieves a collection of attributes from some data source.
     * 
     * @param resolutionContext current resolution context
     * @param workContext current resolver work context
     * 
     * @return collected attributes indexed by attribute ID
     * 
     * @throws ResolutionException thrown if there is a problem resolving the attributes
     */
    @Nullable protected abstract Map<String, IdPAttribute> doDataConnectorResolve(
            @Nonnull final AttributeResolutionContext resolutionContext,
            @Nonnull final AttributeResolverWorkContext workContext) throws ResolutionException;

    /**
     * Return a string which is to be prepended to all log messages.
     * 
     * @return "Data connector '&lt;definitionID&gt;' :"
     */
    @Nonnull @NotEmpty protected String getLogPrefix() {
        // local cache of cached entry to allow unsynchronized clearing of per class cache.
        String prefix = logPrefix;
        if (null == prefix) {
            final StringBuilder builder = new StringBuilder("Data Connector '").append(getId()).append("':");
            prefix = builder.toString();
            if (null == logPrefix) {
                logPrefix = prefix;
            }
        }
        return prefix;
    }
}