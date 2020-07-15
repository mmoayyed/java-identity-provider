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

package net.shibboleth.idp.plugin;

import javax.annotation.Nonnull;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.shibboleth.idp.Version;

/** Useful methods for supporting plugins. 
 *
 */
public final class PluginSupport {
    
    /** Where (relatively) to find available versions. */
    @Nonnull public static final String AVAILABLE_VERSIONS_PATH = "versions.properties";

    /** Where (relatively) to find available versions. */
    @Nonnull public static final String VERSION_INFO_PATH = "version.details.properties";

    /** Property Name for available versions inside {@link #AVAILABLE_VERSIONS_PATH}. */
    @Nonnull public static final String AVAILABLE_VERSIONS_PROPERTY_SUFFIX = ".versions";

    /** Property Name for max supported IdP version inside {@link #VERSION_INFO_PATH}. */
    @Nonnull public static final String MAX_IDP_VERSION = "idp.version.max";
    
    /** Property Name for minimum supported IdP version inside {@link #VERSION_INFO_PATH}. */
    @Nonnull public static final String MIN_IDP_VERSION = "idp.version.min";

    /** Property Name for support level inside {@link #VERSION_INFO_PATH}. */
    @Nonnull public static final String SUPPORT_LEVEL = "support.level";

    /** Value for support level inside {@link #VERSION_INFO_PATH}. */
    public static final int SUPPORT_LEVEL_CURRENT = 0;

    /** Value for support level inside {@link #VERSION_INFO_PATH}. */
    public static final int SUPPORT_LEVEL_OUT_OF_DATE = 1;

    /** Value for support level inside {@link #VERSION_INFO_PATH}. */
    public static final int SUPPORT_LEVEL_UNSUPPORTED = 2;

    /** Value for support level inside {@link #VERSION_INFO_PATH}. */
    public static final int SUPPORT_LEVEL_SECADV = 3;

    /** Value for support level inside {@link #VERSION_INFO_PATH}. */
    public static final int SUPPORT_LEVEL_WITHDRAWN = 4;

    
    /** Class logger. */
    @Nonnull private static Logger log = LoggerFactory.getLogger(PluginSupport.class);
    
    /** Constructor. */
    private PluginSupport() {
    }

    /** Get parse IdP Version (with fallback for testing).
     * @return a {@link PluginVersion} of the version.
     */
    public static PluginVersion getIdPVersion() {
        final String idpVersion = Version.getVersion();
        if (idpVersion!=null) {
            return new PluginVersion(idpVersion);
        }
        log.error("Could not locate IdP Version, assuming 4.1.0");
        return new PluginVersion(4,1,0);
    }
}
