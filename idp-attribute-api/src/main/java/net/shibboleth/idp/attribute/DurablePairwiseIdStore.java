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

package net.shibboleth.idp.attribute;

import java.io.IOException;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * Extended {@link PairwiseIdStore} interface that supports reversal, mutation, and deactivation features
 * requiring durable storage.
 * 
 * @since 4.0.0
 */
public interface DurablePairwiseIdStore extends PairwiseIdStore {
    
    /**
     * Populate the underlying principal/source fields for the input object based on the
     * supplied values.
     * 
     * <p>The input object must contain values for issuer and recipient entityIDs and the
     * pairwiseId itself, and the rest of the fields will be populated as applicable on
     * output.</p>
     * 
     * <p>The object returned, if non-null, will at least contain the principal name and
     * source system ID. It may be, but does not have to be, the same physical object
     * used as input. The original input object should not be referenced further.</p>
     * 
     * @param pid object to populate
     * 
     * @return object for the given inputs or null if none exists
     * @throws IOException if an error occurs accessing the store
     */
    @Nullable PairwiseId getByIssuedValue(@Nonnull final PairwiseId pid) throws IOException;
    
    /**
     * Deactivate/revoke a pairwise ID.
     * 
     * <p>If the object's deactivation time field is null, then the current time is used.</p>
     * 
     * @param pid the object to deactivate/revoke
     * 
     * @throws IOException if there is an error updating the store
     */
    void deactivate(@Nonnull PairwiseId pid) throws IOException;

    /**
     * Attach a peer-supplied alias to a pairwise ID.
     * 
     * @param pid the object to update in storage
     * 
     * @throws IOException if there is an error updating the store
     */
    void attach(@Nonnull PairwiseId pid) throws IOException;
    
}