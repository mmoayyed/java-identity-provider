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

package net.shibboleth.idp.installer.impl;

import java.nio.file.Files;
import java.nio.file.Path;

import org.apache.tools.ant.Project;
import org.apache.tools.ant.taskdefs.Copy;
import org.apache.tools.ant.types.FileSet;

/** General common names and helper functions for the installer. */
public final class InstallerSupport {

    /** The name of the file and the property with the current V4 installation value.*/
    public static final String VERSION_NAME = "idp.installed.version";
    
    /** A psuedo ant-project as parent. */
    public static final Project ANT_PROJECT = new Project();

    /** Private Constructor. */
    private InstallerSupport() {}
    
    /** Copy files.  We use ant rather than {@link Files#copy(Path, Path, java.nio.file.CopyOption...)}
     * because the latter has issues with the Windows ReadOnly Attribute, and the former is tried
     * and tested technology.
     * @param from where to copy from
     * @param to where to copy to
     * @return a partially populated {@link Copy} task 
     */
    protected static Copy getCopyTask(final Path from, final Path to) {
        final Copy result = new Copy();
        result.setTodir(to.toFile());
        final FileSet fromSet = new FileSet();
        fromSet.setDir(from.toFile());
        result.addFileset(fromSet);
        result.setProject(ANT_PROJECT);
        return result;
    }

}
