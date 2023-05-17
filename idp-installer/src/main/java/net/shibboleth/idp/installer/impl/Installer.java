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

import org.slf4j.Logger;

import net.shibboleth.idp.installer.BuildWar;
import net.shibboleth.idp.installer.CopyDistribution;
import net.shibboleth.idp.installer.V4Install;
import net.shibboleth.shared.component.ComponentInitializationException;

/**
 * Entry point to run the main classes.
 */
public final class Installer {

    /** hidden  Constructor. */
    private Installer() {}

    /** simulate the ant tasks.
     * @param args what
     * @throws ComponentInitializationException if badness occurrs
     */
    public static void main(final String[] args) throws ComponentInitializationException {
        final Logger log = InstallationLogger.getLogger(Installer.class);
        if (args.length !=1) {
            log.error("One Parameter only {}", (Object[]) args);
            return;
        }
        boolean copyInstall = false;
        boolean doInstall = false;
        if ("install".equals(args[0])) {
            copyInstall = true;
            doInstall = true;
        } else if ("install-nocopy".equals(args[0])) {
            doInstall = true;
        } else if (!"build-war".equals(args[0])) {
            log.error("Parameter must be \"install\", \"install-nocopy\" or \"build-war\" was \"{}\"", args[0]);
            return;
        }
        final InstallerProperties ip = new InstallerProperties(!copyInstall);
        ip.initialize();
        final CurrentInstallState is = new CurrentInstallState(ip);
        is.initialize();

        if (copyInstall) {
            final CopyDistribution dist = new CopyDistribution(ip, is);
            dist.execute();
        }

        if (doInstall) {
            final V4Install inst = new V4Install(ip, is);
            inst.execute();
        }

        final BuildWar bw = new BuildWar(ip, is);
        bw.execute();

    }

}
