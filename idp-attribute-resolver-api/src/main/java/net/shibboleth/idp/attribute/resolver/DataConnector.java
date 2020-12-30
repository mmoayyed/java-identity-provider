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
import java.util.Map;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.concurrent.ThreadSafe;

import net.shibboleth.idp.attribute.IdPAttribute;
import net.shibboleth.utilities.java.support.annotation.constraint.NonnullElements;
import net.shibboleth.utilities.java.support.annotation.constraint.Unmodifiable;

/**
 * Base class for data connector resolver plugins.
 */
@ThreadSafe
public interface DataConnector extends ResolverPlugin<Map<String, IdPAttribute>> {

    /**
     * Get how long to wait until we declare the connector (potentially) alive again.
     *
     * @return how long to wait.
     */
     @Nonnull Duration getNoRetryDelay();

     /**
      * Get the time when this connector last succeeded.
      *
      * TODO: Remove default in V5.
      *
      * @return when it last succeeded
      * 
      * @since 4.1.0
      */
     @Nullable default Instant getLastSuccess() {
         return Instant.now();
     }

     /**
      * Get the time when this connector last failed. This will be set for any exception regardless of the setting of
      * {@link #isPropagateResolutionExceptions()}
      *
      * @return when it last failed
      */
     @Nullable Instant getLastFail();

    /**
     * Gets the ID of the {@link DataConnector} whose values will be used in the event that this data connector
     * experiences an error.
     * 
     * @return ID of the {@link DataConnector} whose values will be used in the event that this data connector
     *         experiences an error
     */
    @Nullable String getFailoverDataConnectorId();

    /**
     * Gets whether we export all attributes during resolution.
     * <p>
     * If this returns false then {@link #getExportAttributes()} returns the
     * list of attributes to return.
     *
     * @return whether we export all attributes
     */
    boolean isExportAllAttributes();

    /**
     * Gets the list of attribute names to export during resolution.
     *
     * @return the list of attribute names to export during resolution
     */
    @Nonnull @NonnullElements @Unmodifiable Collection<String> getExportAttributes();
}