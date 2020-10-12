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
    
    /** Property Name suffix for available versions inside {@link IdPPlugin#getUpdateURLs()}. */
    @Nonnull public static final String AVAILABLE_VERSIONS_PROPERTY_SUFFIX = ".versions";

    /** Property Name for Download directory {@link IdPPlugin#getUpdateURLs()}. */
    @Nonnull public static final String DOWNLOAD_URL_INTERFIX = ".downloadURL.";

    /** Property Name for download name {@link IdPPlugin#getUpdateURLs()}. */
    @Nonnull public static final String BASE_NAME_INTERFIX = ".baseName.";

    /** Property Name for max supported IdP version inside inside {@link IdPPlugin#getUpdateURLs()}. */
    @Nonnull public static final String MAX_IDP_VERSION_INTERFIX = ".idpVersionMax.";

    /** Property Name for minimum supported IdP version inside inside {@link IdPPlugin#getUpdateURLs()}. */
    @Nonnull public static final String MIN_IDP_VERSION_INTERFIX = ".idpVersionMin.";

    /** Property Name for support level inside inside {@link IdPPlugin#getUpdateURLs()}. */
    @Nonnull public static final String SUPPORT_LEVEL_INTERFIX = ".supportLevel.";

    /** Used for specifying templated keynames. */
    @Nonnull public static final String VERSION_PATTERN = "%{version}";

    /** Used for specifying templated results. */
    @Nonnull public static final String VERSION_PATTERN_REGEX = "\\%\\{version\\}";

    /** Value for support level pointed to by {@link #SUPPORT_LEVEL_INTERFIX}.*/
    public static enum SupportLevel {
        /** The current release. */
        Current,
        /** Still working but a new version is available. */
        OutOfDate,
        /** Out of Support. */
        Unsupported,
        /** Security alerts against this plugin. */
        Secadv,
        /** Withdrawn. */
        Withdrawn,
        /** Nothing published. */
        Unknown
    }

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
