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

package net.shibboleth.idp.installer.plugin.impl;

import java.io.IOException;
import java.net.URL;
import java.nio.file.Path;
import java.util.List;

import javax.annotation.Nonnull;

import net.shibboleth.idp.plugin.PluginDescription;
import net.shibboleth.utilities.java.support.collection.Pair;

/**
 * A class which will answer questions about a plugin state as of now
 * (by querying the information Resources for the current published state). 
 */
@Deprecated public final class PluginState extends net.shibboleth.idp.plugin.impl.PluginState {

    /**
     * Constructor.
     *
     * @param description old style description
     */
    @Deprecated
    public PluginState(@Nonnull final net.shibboleth.utilities.java.support.plugin.PluginDescription description) {
        super(new PluginDescription() {            
            public List<URL> getUpdateURLs() throws IOException {
                return description.getUpdateURLs();
            }
            public List<Pair<Path, List<String>>> getPropertyMerges() {
                return description.getPropertyMerges();
            }
            public String getPluginId() {
                return description.getPluginId();
            }
            public int getPatchVersion() {
                return description.getPatchVersion();
            }
            public int getMinorVersion() {
                return description.getMinorVersion();
            }
            public int getMajorVersion() {
                return description.getMajorVersion();
            }
            public List<Path> getFilePathsToCopy() {
                return description.getFilePathsToCopy();
            }
            public List<Pair<URL, Path>> getExternalFilePathsToCopy() throws IOException {
                return description.getExternalFilePathsToCopy();
            }
            public List<String> getAdditionalPropertyFiles() {
                return description.getAdditionalPropertyFiles();
            }
        });
    }
}
