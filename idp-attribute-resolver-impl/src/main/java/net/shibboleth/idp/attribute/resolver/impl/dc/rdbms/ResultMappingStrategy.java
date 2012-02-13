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

import java.sql.ResultSet;
import java.util.Map;

import javax.annotation.Nonnull;

import net.shibboleth.idp.attribute.Attribute;
import net.shibboleth.idp.attribute.resolver.AttributeResolutionException;

import com.google.common.base.Optional;

/** Strategy for mapping from a {@link ResultSet} to a collection of {@link Attribute}s. */
public interface ResultMappingStrategy {

    /**
     * Maps the given result set to a collection of {@link Attribute} indexed by the attribute's ID. This method
     * <strong>MUST NOT</strong> close the given {@link ResultSet}.
     * 
     * @param results current result set
     * 
     * @return the mapped attributes
     * 
     * @throws AttributeResolutionException thrown if there is a problem reading data from the result set or mapping it
     */
    @Nonnull public Optional<Map<String, Attribute>> map(@Nonnull ResultSet results)
            throws AttributeResolutionException;
}