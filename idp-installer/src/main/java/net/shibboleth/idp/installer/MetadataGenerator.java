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

import javax.annotation.Nonnull;

import org.apache.tools.ant.BuildException;

import net.shibboleth.utilities.java.support.component.InitializableComponent;

/**
 * Interface to define Metadata Generation.
 */
public interface MetadataGenerator extends InitializableComponent {

    /** Set where to write the metadata.
     * @param file what to set.
     */
    public void setOutput(@Nonnull File file);

    /** Set a description of the IdP.
     * @param what what to set.
     */
    public void setParameters(@Nonnull final MetadataGeneratorParameters what);

    /** Generate the metadata given the parameters.
     * @throws BuildException if badness occurs.
     */
    void generate() throws BuildException;
}
