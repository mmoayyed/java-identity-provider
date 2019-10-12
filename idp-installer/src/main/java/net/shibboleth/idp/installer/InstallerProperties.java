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

import java.io.File;
import java.nio.file.Path;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.apache.tools.ant.BuildException;

import net.shibboleth.utilities.java.support.component.InitializableComponent;

/** Interface to describe simply parameterization and status of the installation.
 */
public interface InstallerProperties extends InitializableComponent {

    /** Get where we are installing/updating/building the war.
     * @return the target directory
     * @throws BuildException if something goes awry.
     */
    @Nonnull public Path getTargetDir() throws BuildException;

    /** Where is the install coming from?
     * @return the source directory
     */
    @Nullable public Path getSourceDir();

    /** Get the EntityId for this install.
     * @return the  name.
     */
    @Nonnull public String getEntityID();

    /** Get the host name for this install.
     * @return the host name.
     */
    @Nonnull public String getHostName();

    /** Get the scope for this installation.
     * @return The scope.
     */
    @Nonnull public String getScope();

    /** Get the SubjectAltName for the certificates.
     * @return the  SubjectAltName
     */
    @Nonnull public String getSubjectAltName();

    /** Get the password for the keystore for this installation.
     * @return the password.
     */
    @Nonnull public String getKeyStorePassword();

    /** Get the password for the sealer for this installation.
     * @return the password.
     */
    @Nonnull public String getSealerPassword();

    /** Get the alias for the sealer key.
     * @return the alias
     */
    @Nonnull public String getSealerAlias();

    /** Get the key size for signing, encryption and backchannel.
     * @return the keysize */
    public int getKeySize();

    /** Get the a file to merge with idp.properties or null.
     *
     * @return the file or null if it none required.
     * @throws BuildException if badness happens
     */
    @Nullable public File getIdPMergePropertiesFile() throws BuildException;

    /** Get the a file to merge with ldap.properties or null.
     *
     * @return the file or null if it none required.
     * @throws BuildException  if badness happens
     */
    public File getLDAPMergePropertiesFile() throws BuildException;
}
