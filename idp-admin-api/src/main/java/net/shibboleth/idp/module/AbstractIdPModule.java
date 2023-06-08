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

import net.shibboleth.idp.Version;
import net.shibboleth.profile.module.AbstractModule;
import net.shibboleth.shared.annotation.constraint.NotEmpty;

/**
 * {@link IdPModule} base class implementing basic file management.
 * 
 * @since 4.1.0
 */
public abstract class AbstractIdPModule extends AbstractModule implements IdPModule {

    /** Extension for preserving user files. */
    @Nonnull @NotEmpty public static final String IDPSAVE_EXT = ".idpsave";

    /** Base extension for adding new default files. */
    @Nonnull @NotEmpty public static final String IDPNEW_EXT_BASE = ".idpnew";

    /** Constructor. */
    public AbstractIdPModule() {
        super(Version.getVersion(), IDPNEW_EXT_BASE, IDPSAVE_EXT);
    }
    
}