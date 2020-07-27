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

package net.shibboleth.idp.installer.ant.impl;

import java.util.Map.Entry;
import java.util.stream.Collectors;

import javax.annotation.Nonnull;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Task;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.shibboleth.idp.installer.BuildWar;
import net.shibboleth.idp.installer.CopyDistribution;
import net.shibboleth.idp.installer.InstallerPropertiesImpl;
import net.shibboleth.idp.installer.V4Install;
import net.shibboleth.idp.installer.impl.CurrentInstallStateImpl;
import net.shibboleth.idp.installer.metadata.impl.MetadataGeneratorImpl;
import net.shibboleth.utilities.java.support.collection.CollectionSupport;
import net.shibboleth.utilities.java.support.component.ComponentInitializationException;
import net.shibboleth.utilities.java.support.logic.Constraint;
import net.shibboleth.utilities.java.support.primitive.StringSupport;

/**
 * A thin veneer around the V4 installer.
 */
public class V4InstallTask extends Task {

    /** What to do?*/
    private String task;
    
    /** set what to do.
     * @param what the value to set. One of "install", "install-nocopy", "build-war"
     */
    public void setTask(@Nonnull final String what) {
        task = Constraint.isNotNull(StringSupport.trimOrNull(what), "Task must be non-null").toLowerCase();
    }
    
    @Override public void execute() {
        final Logger log = LoggerFactory.getLogger(V4InstallTask.class);
        boolean copyInstall = false;
        boolean doInstall = false;
        if ("install".equals(task)) {
            copyInstall = true;
            doInstall = true;
        } else if ("install-nocopy".equals(task)) {
            doInstall = true;
        } else if (!"build-war".equals(task)) {
            log.error("Parameter must be \"install\", \"install-nocopy\" or \"build-war\" was \"{}\"", task);
            throw new BuildException("Invalid parameter to task");
        }
        try {
            final InstallerPropertiesImpl ip = new InstallerPropertiesImpl(!copyInstall);

            // Grab the ant properties and plug in.  Note Java V2 to V11 conversion.
            ip.setInheritedProperties(
                    getProject().
                    getProperties().
                    entrySet().
                    stream().
                    filter(e -> System.getProperty(e.getKey()) == null).
                    filter(e -> e.getValue() instanceof String).
                    collect(Collectors.toUnmodifiableMap(Entry::getKey,
                            e-> (String) e.getValue(),
                            CollectionSupport.warningMergeFunction("InstallerProperties", true))));

            final CurrentInstallStateImpl is;
            ip.initialize();
            is = new CurrentInstallStateImpl(ip);
            is.initialize();
            if (copyInstall) {
                final CopyDistribution dist = new CopyDistribution(ip, is);
                dist.initialize();
                dist.execute();
            }

            if (doInstall) {
                final V4Install inst = new V4Install(ip, is);
                inst.setMetadataGenerator(new MetadataGeneratorImpl());
                inst.initialize();
                inst.execute();
            }

            final BuildWar bw = new BuildWar(ip, is);
            bw.initialize();
            bw.execute();
        } catch (final ComponentInitializationException e) {
            log.error("Could set up state", e);
            throw new BuildException(e);
        }
    }
}
