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

package net.shibboleth.idp.admin.impl;

import java.util.Properties;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import net.shibboleth.profile.installablecomponent.InstallableComponentInfo;
import net.shibboleth.profile.installablecomponent.InstallableComponentVersion;

/** Implementation of {@link InstallableComponentInfo} for an IdP Version.  This is keyed in
 * to the format of the idp-versions.properties file (which doesn't specify max and min "supported IdP versions". */
public final class IdPInfo extends InstallableComponentInfo {

    /** The "plugin Id" to look up idp versions with. */
    @Nonnull public static String IDP_PLUGIN_ID = "net.shibboleth.idp";

     /**
      * Constructor.
      * @param props The property file to populate from
      */
     public IdPInfo(@Nonnull Properties props) {
         super(IDP_PLUGIN_ID, props);
     }

     /** {@inheritDoc} */
     @Override
     protected InstallableComponentVersion getMaxVersion(@Nonnull Properties props, @Nonnull String version) {
         // The maximum version that version "us" can be installed in is "us" (a re-intall).
         return new InstallableComponentVersion(version);
     }

     /** {@inheritDoc} */
     @Override
     @Nullable
     protected InstallableComponentVersion getMinVersion(@Nonnull Properties props, @Nonnull String version) {
         // We can always be on anything from V4.0.0
         return new InstallableComponentVersion(4,0,0);
     }
}
