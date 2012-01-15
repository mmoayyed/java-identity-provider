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

import net.jcip.annotations.ThreadSafe;
import net.shibboleth.idp.attribute.Attribute;
import net.shibboleth.utilities.java.support.component.UnmodifiableComponentException;
import net.shibboleth.utilities.java.support.primitive.StringSupport;

/** Base class for data connector resolver plugins. */
@ThreadSafe
public abstract class BaseDataConnector extends BaseResolverPlugin<Map<String, Attribute<?>>> {

    /** ID of the data connector to use if this one fails. */
    private String failoverDataConnectorId;

    /**
     * Gets the ID of the {@link BaseDataConnector} whose values will be used in the event that this data connector
     * experiences an error.
     * 
     * @return ID of the {@link BaseDataConnector} whose values will be used in the event that this data connector
     *         experiences an error
     */
    public String getFailoverDataConnectorId() {
        return failoverDataConnectorId;
    }

    /**
     * Set the ID of the {@link BaseDataConnector} whose values will be used in the event that this data connector
     * experiences an error.
     * 
     * @param id ID of the {@link BaseDataConnector} whose values will be used in the event that this data connector
     *            experiences an error
     */
    public synchronized void setFailoverDataConnectorId(final String id) {
        if (isInitialized()) {
            throw new UnmodifiableComponentException("Attribute resolver plugin " + getId()
                    + " has already been initialized, failover connector can not be changed.");
        }

        failoverDataConnectorId = StringSupport.trimOrNull(id);
    }

    /**
     * {@inheritDoc}
     * 
     * This method delegates to {@link #doDataConnectorResolve(AttributeResolutionContext)}. It serves as a future
     * extension point for introducing new common behavior.
     */
    public final Map<String, Attribute<?>> doResolve(final AttributeResolutionContext resolutionContext)
            throws AttributeResolutionException {
        return doDataConnectorResolve(resolutionContext);
    }

    /**
     * Retrieves a collection of attributes from some data source.
     * 
     * @param resolutionContext current resolution context
     * 
     * @return collected attributes indexed by attribute ID
     * 
     * @throws AttributeResolutionException thrown if there is a problem resolving the attributes
     */
    protected abstract Map<String, Attribute<?>> doDataConnectorResolve(
            final AttributeResolutionContext resolutionContext) throws AttributeResolutionException;
}