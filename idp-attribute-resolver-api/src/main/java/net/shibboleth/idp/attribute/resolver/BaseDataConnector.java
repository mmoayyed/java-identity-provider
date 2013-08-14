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

import java.util.Map;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.concurrent.ThreadSafe;

import net.shibboleth.idp.attribute.Attribute;
import net.shibboleth.utilities.java.support.annotation.constraint.NotEmpty;
import net.shibboleth.utilities.java.support.component.ComponentInitializationException;
import net.shibboleth.utilities.java.support.component.ComponentSupport;
import net.shibboleth.utilities.java.support.primitive.StringSupport;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Base class for data connector resolver plugins. */
@ThreadSafe
public abstract class BaseDataConnector extends BaseResolverPlugin<Map<String, Attribute>> {

    /** Class logger. */
    private final Logger log = LoggerFactory.getLogger(BaseDataConnector.class);

    /** ID of the data connector to use if this one fails. */
    private String failoverDataConnectorId;

    /** cache for the log prefix - to save multiple recalculations. */
    private String logPrefix;

    /**
     * Gets the ID of the {@link BaseDataConnector} whose values will be used in the event that this data connector
     * experiences an error.
     * 
     * @return ID of the {@link BaseDataConnector} whose values will be used in the event that this data connector
     *         experiences an error
     */
    @Nullable public String getFailoverDataConnectorId() {
        return failoverDataConnectorId;
    }

    /**
     * Set the ID of the {@link BaseDataConnector} whose values will be used in the event that this data connector
     * experiences an error.
     * 
     * @param id ID of the {@link BaseDataConnector} whose values will be used in the event that this data connector
     *            experiences an error
     */
    public synchronized void setFailoverDataConnectorId(@Nullable final String id) {
        ComponentSupport.ifInitializedThrowUnmodifiabledComponentException(this);
        ComponentSupport.ifDestroyedThrowDestroyedComponentException(this);

        failoverDataConnectorId = StringSupport.trimOrNull(id);
    }

    /**
     * {@inheritDoc}
     * 
     * This method delegates to {@link #doDataConnectorResolve(AttributeResolutionContext)}. It serves as a future
     * extension point for introducing new common behavior.
     */
    @Nullable public final Map<String, Attribute> doResolve(
            @Nonnull final AttributeResolutionContext resolutionContext) throws ResolutionException {
        Map<String, Attribute> result = doDataConnectorResolve(resolutionContext);

        if (null == result) {
            log.debug("{} no attributes were produced during resolution", getId());
            return result;
        } else {
            log.debug("{} produced the following {} attributes during resolution {}", new Object[] {
                    getLogPrefix(), result.size(), result.keySet(),});
            for (String attrName : result.keySet()) {
                Attribute attr = result.get(attrName);
                log.debug("Data connector '{}': Attribute '{}': Values '{}'",
                        new Object[] {getLogPrefix(), attrName, attr.getValues(),});
            }
        }

        return result;
    }
    /** {@inheritDoc} */
    protected void doInitialize() throws ComponentInitializationException {
        
        super.doInitialize();
        
        // The Id is now definitive.  Just in case it was used prior to that, reset the getPrefixCache
        logPrefix = null;
    }
    
    /**
     * Retrieves a collection of attributes from some data source.
     * 
     * @param resolutionContext current resolution context, guaranteed not to be bull
     * 
     * @return collected attributes indexed by attribute ID
     * 
     * @throws ResolutionException thrown if there is a problem resolving the attributes
     */
    @Nullable protected abstract Map<String, Attribute> doDataConnectorResolve(
            @Nonnull final AttributeResolutionContext resolutionContext) throws ResolutionException;
    
    /**
     * Return a string which is to be prepended to all log messages.
     * 
     * @return "Data connector '<definitionID>' :"
     */
    @Nonnull @NotEmpty protected String getLogPrefix() {
        // local cache of cached entry to allow unsynchronised clearing of per class cache.
        String prefix = logPrefix;
        if (null == prefix) {
            StringBuilder builder = new StringBuilder("Data Connector '").append(getId()).append("':");
            prefix = builder.toString();
            if (null == logPrefix) {
                logPrefix = prefix;
            }
        }
        return prefix;
    }

}