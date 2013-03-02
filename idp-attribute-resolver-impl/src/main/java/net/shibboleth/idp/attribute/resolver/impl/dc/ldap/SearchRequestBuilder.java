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

package net.shibboleth.idp.attribute.resolver.impl.dc.ldap;

import javax.annotation.Nonnull;

import org.ldaptive.SearchRequest;

import net.shibboleth.idp.attribute.resolver.AttributeResolutionContext;
import net.shibboleth.idp.attribute.resolver.ResolutionException;

//TODO(lajoie): probably want an abstract class that allows for setting things like which attributes are binary, handlers, scope, etc.

/** Builder used to created {@link SearchRequest} instances. */
public interface SearchRequestBuilder {

    /**
     * Creates a search request that can be executed against a given LDAP connection in order to produce results.
     * 
     * @param resolutionContext current request context
     * 
     * @return search request to be executed
     * 
     * @throws ResolutionException throw if there is a problem creating the search request
     */
    @Nonnull public SearchRequest build(@Nonnull AttributeResolutionContext resolutionContext)
            throws ResolutionException;
}