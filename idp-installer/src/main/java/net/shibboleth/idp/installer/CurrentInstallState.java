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

import javax.annotation.Nullable;

import org.apache.tools.ant.BuildException;

import net.shibboleth.utilities.java.support.component.InitializableComponent;

/** Tells the installers about the current install state. */
public interface CurrentInstallState extends InitializableComponent {

    /** What is the installer version.
     * @return "3" for a V3 install, null for a new install or the value we write during last install.
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
}
