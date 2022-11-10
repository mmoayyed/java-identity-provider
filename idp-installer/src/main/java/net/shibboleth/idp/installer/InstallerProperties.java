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
import java.util.Set;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.apache.tools.ant.BuildException;

import net.shibboleth.shared.component.InitializableComponent;

/** Interface to describe simply parameterization and status of the installation.
 */
public interface InstallerProperties extends InitializableComponent {

    /** Those modules enabled by default. */
    public static final Set<String> DEFAULT_MODULES = Set.of("idp.authn.Password", "idp.admin.Hello");

    /** Get where we are installing/updating/building the war.
     * @return the target directory
     * @throws BuildException if something goes awry.
     */
    @Nonnull public Path getTargetDir() throws BuildException;

    /** Does the user want us to *not* tidy up.
     * @return do we not tidy up?*/
    public boolean isNoTidy();

    /** Mode to set on all files in credentials.
    * @return the mode
    */
    @Nonnull public String getCredentialsKeyFileMode();

    /** Group to set on all files in credentials and conf.
    * @return the mode or null if none to be set
    */
    @Nullable public String getCredentialsGroup();

    /** Do we set the mode?
    * @return do we the mode
    */
    public boolean isSetGroupAndMode();

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
    @Nullable public Path getIdPMergeProperties() throws BuildException;

    /** Get the a file to merge with ldap.properties or null.
     *
     * @return the path or null if it none required.
     * @throws BuildException  if badness happens
     */
    @Nullable public Path getLDAPMergeProperties() throws BuildException;

    /** Get the LDAP password iff one was provided.  DO NOT PROMPT
     *
     * @return the password if provided by a properties
     * @throws BuildException  if badness happens
     */
    @Nullable public String getLDAPPassword()  throws BuildException;

    /** Get a directory to use to "pre-overlay" the conf directory.
     * Files will be copied from here if they don't already exist in conf,
     * <b>before</b> the files are copied from the distribution.
     * @return the path or null if non specified.
     * @throws BuildException  if badness happens
     */
    @Nullable public Path getConfPreOverlay() throws BuildException;

    /** Get a path to use to do the initial edit-webapp populate.
     * @return the path or null if non specified.
     * @throws BuildException  if badness happens
     */
    @Nullable public Path getInitialEditWeb() throws BuildException;
    /** Get the modules to enable after first install.
     * @return the modules
     */
    @Nonnull public default Set<String> getModulesToEnable() {
        return DEFAULT_MODULES;
    }
}
