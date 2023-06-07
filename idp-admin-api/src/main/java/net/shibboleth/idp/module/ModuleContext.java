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

package net.shibboleth.idp.module;

import javax.annotation.Nonnull;

import net.shibboleth.shared.annotation.constraint.NotEmpty;

/**
 * Information required to perform some module operations.
 * 
 * @since 4.1.0
 */
@Deprecated(forRemoval = true, since = "5.0.0")
public final class ModuleContext extends net.shibboleth.profile.module.ModuleContext {

    /**
     * Constructor.
     *
     * @param home location of IdP install
     */
    public ModuleContext(@Nonnull @NotEmpty final String home) {
        super(home);
    }

}