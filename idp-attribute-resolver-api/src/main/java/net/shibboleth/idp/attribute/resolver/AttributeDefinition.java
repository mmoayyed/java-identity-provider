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

import javax.annotation.concurrent.ThreadSafe;

import net.shibboleth.idp.attribute.IdPAttribute;

/** Definition of attribute definition resolver plugins. */
@ThreadSafe
public interface AttributeDefinition extends ResolverPlugin<IdPAttribute> {

    /**
     * Gets whether this attribute definition is only a dependency and thus its values should never be released outside
     * the resolver.
     * 
     * @return true if this attribute is only used as a dependency, false otherwise
     */
    boolean isDependencyOnly();

    /**
     * Gets whether this attribute definition has to be resolved prior in a first pass (in order to prime
     * contexts which can be used in the "main pass").
     * @return true if this is a definition to be done "first"
     */
    boolean isPreRequested();
}