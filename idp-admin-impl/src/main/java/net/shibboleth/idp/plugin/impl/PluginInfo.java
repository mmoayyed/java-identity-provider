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

package net.shibboleth.idp.plugin.impl;

import java.util.Properties;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import net.shibboleth.profile.installablecomponent.InstallableComponentInfo;
import net.shibboleth.profile.installablecomponent.InstallableComponentSupport;
import net.shibboleth.profile.installablecomponent.InstallableComponentVersion;
import net.shibboleth.shared.primitive.StringSupport;

/**
 * Information about a Plugin.
 */
public class PluginInfo extends InstallableComponentInfo {

    /**
     * Constructor.
     *
     * @param id the plugin Id to ask about.
     * @param props the properties file to load from
     */
    public PluginInfo(@Nonnull String id, @Nonnull Properties props) {
        super(id, props);
    }

    /** {@inheritDoc} */
    @Override
    protected @Nullable InstallableComponentVersion getMaxVersion(@Nonnull Properties props, @Nonnull String version) {
        final String maxVersionInfo = StringSupport.trimOrNull(
                props.getProperty(getComponentId()  + InstallableComponentSupport.MAX_IDP_VERSION_INTERFIX + version));
        if (maxVersionInfo == null) {
            return null;
        }
        return new InstallableComponentVersion(maxVersionInfo);
    }

    /** {@inheritDoc} */
    @Override
    @Nullable
    protected InstallableComponentVersion getMinVersion(@Nonnull Properties props, @Nonnull String version) {
        final String minVersionInfo = StringSupport.trimOrNull(
                props.getProperty(getComponentId() + InstallableComponentSupport.MIN_IDP_VERSION_INTERFIX + version));
        if (minVersionInfo == null) {
            return null;
        }
        return new InstallableComponentVersion(minVersionInfo);
    }

}
