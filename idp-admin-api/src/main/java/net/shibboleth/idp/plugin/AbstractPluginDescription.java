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

import java.io.IOException;
import java.net.URL;
import java.nio.file.Path;
import java.util.Collections;
import java.util.List;

import javax.annotation.Nonnegative;
import javax.annotation.Nonnull;

import net.shibboleth.utilities.java.support.collection.Pair;

/**
 * A base class {@link PluginDescription} which defaults many common settings.
 */
public abstract class AbstractPluginDescription implements PluginDescription {
    
    /** {@inheritDoc} */
    @Nonnull public List<String> getAdditionalPropertyFiles() {
        return Collections.emptyList();
    }
    
    /** {@inheritDoc} */
    @Nonnull public List<Path> getFilePathsToCopy() {
        return Collections.emptyList();
    }
    
    /** {@inheritDoc} */
    @Nonnull public List<Pair<URL, Path>> getExternalFilePathsToCopy() throws IOException {
        return Collections.emptyList();
    }

    /** {@inheritDoc} */
    @Nonnull public List<Pair<Path, List<String>>> getPropertyMerges() {
        return Collections.emptyList();
    }
    
    /** {@inheritDoc} */
    @Nonnegative public int getPatchVersion() {
        return 0;
    }
}
