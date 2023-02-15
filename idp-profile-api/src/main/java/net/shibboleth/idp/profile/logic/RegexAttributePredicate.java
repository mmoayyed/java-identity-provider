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

package net.shibboleth.idp.profile.logic;

import net.shibboleth.shared.primitive.DeprecationSupport;
import net.shibboleth.shared.primitive.DeprecationSupport.ObjectType;

/**
 * Deprecated stub for relocated class.
 * 
 * @deprecated
 */
@Deprecated(since="5.0.0", forRemoval=true)
public class RegexAttributePredicate extends net.shibboleth.profile.context.logic.RegexAttributePredicate {

    /** Constructor. */
    public RegexAttributePredicate() {
        DeprecationSupport.warn(ObjectType.CLASS, getClass().getName(), null,
                net.shibboleth.profile.context.logic.RegexAttributePredicate.class.getName());
    }

}