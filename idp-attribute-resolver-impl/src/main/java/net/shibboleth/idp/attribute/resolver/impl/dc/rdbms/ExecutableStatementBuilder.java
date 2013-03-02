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

package net.shibboleth.idp.attribute.resolver.impl.dc.rdbms;

import javax.annotation.Nonnull;

import net.shibboleth.idp.attribute.resolver.AttributeResolutionContext;
import net.shibboleth.idp.attribute.resolver.ResolutionException;

/** Builder used to created {@link ExecutableStatement} instances. */
public interface ExecutableStatementBuilder {

    /**
     * Creates an statement that can be executed against a given database connection in order to produce results.
     * 
     * @param resolutionContext current request context
     * 
     * @return statement to be executed
     * 
     * @throws ResolutionException throw if their is a problem creating the statement
     */
    @Nonnull public ExecutableStatement build(@Nonnull AttributeResolutionContext resolutionContext)
            throws ResolutionException;
}