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

package net.shibboleth.idp.cas.service.impl;

import javax.annotation.Nonnull;

import net.shibboleth.shared.primitive.DeprecationSupport;
import net.shibboleth.shared.primitive.DeprecationSupport.ObjectType;

/**
 * Default comparator implementation for comparing CAS service URLs. URL comparison is case-insensitive and supports
 * ignoring predefined URL path parameters. The common session marker <em>;jessionid=value</em> is ignored by default.
 *
 * @author Marvin S. Addison
 * 
 * @deprecated
 */
@Deprecated(forRemoval=true, since="4.0.0")
public class DefaultServiceComparator extends net.shibboleth.idp.cas.service.DefaultServiceComparator {

    /** Creates a new instance that ignores <em>;jsessionid=value</em>. */
    public DefaultServiceComparator() {
        DeprecationSupport.warn(ObjectType.CLASS, getClass().getName(), "cas-protocol.xml",
                "net.shibboleth.idp.cas.service.DefaultServiceComparator");
    }

    /**
     * Creates a new instance that ignores the given path parameter names (and any associated values).
     *
     * @param  parameterNames  List of path parameter names to ignore.
     */
    public DefaultServiceComparator(@Nonnull final String ... parameterNames) {
        super(parameterNames);
        DeprecationSupport.warn(ObjectType.CLASS, getClass().getName(), "cas-protocol.xml",
                "net.shibboleth.idp.cas.service.DefaultServiceComparator");
    }
    
}