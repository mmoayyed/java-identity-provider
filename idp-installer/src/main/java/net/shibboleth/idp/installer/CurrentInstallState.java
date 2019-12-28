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

package net.shibboleth.idp.installer;

import java.nio.file.Path;
import java.util.List;
import java.util.Properties;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.apache.tools.ant.BuildException;

import net.shibboleth.idp.spring.IdPPropertiesApplicationContextInitializer;
import net.shibboleth.utilities.java.support.component.InitializableComponent;

/** Tells the installers about the current install state. */
public interface CurrentInstallState extends InitializableComponent {

    /** What to return if this is V3. */
    static final String V3_VERSION = "3";

    /** What is the installer version.
     * @return {@value #V3_VERSION} for a V3 install, null for a new install or the value we wrote during last install.
     * @throws BuildException if we find an inconsistency
     */
    @Nullable String getInstalledVersion() throws BuildException;
    
    /** Was idp.properties present in the target file when we started the install?
     * @return if it was.
     */
    boolean isIdPPropertiesPresent();

    /** Was ldapp.properties present in the target file when we started the install?
     * @return if it was.
     */
    boolean isLDAPPropertiesPresent();
 
    /** Get the properties associated with the current configuration.
     * This comes idp.properties and anything it points to via
     * {@value IdPPropertiesApplicationContextInitializer#IDP_ADDITIONAL_PROPERTY}.
     * @return the properties, or null if this is a new install.
     */
    @Nullable Properties getCurrentlyInstalledProperties();

    /** Return the list of paths of files which were not there prior to the install
     * but which might be created by the installed but to no purpose.
     * @return the list of paths.
     * @return
     */
    @Nonnull List<Path> getPathsToBeDeleted();
}
