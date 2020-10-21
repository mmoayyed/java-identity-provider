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

package net.shibboleth.idp.attribute.resolver.dc.storage;

import java.io.IOException;

import javax.annotation.Nonnull;

import org.opensaml.storage.StorageRecord;
import org.opensaml.storage.StorageService;

import net.shibboleth.idp.attribute.resolver.dc.ExecutableSearch;

/**
 * A search that can be executed against a {@link StorageService} to fetch a result.
 * 
 * @since 4.1.0
 */
public interface StorageServiceSearch extends ExecutableSearch {

    /**
     * Executes the search and returns the result.
     * 
     * @param storageService storage service to search
     * 
     * @return the result of the executed search
     * 
     * @throws IOException thrown if there is a problem executing the search
     */
    @Nonnull StorageRecord<?> execute(@Nonnull StorageService storageService) throws IOException;
}