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

import java.io.IOException;

import net.shibboleth.idp.installer.impl.CurrentInstallState;
import net.shibboleth.idp.installer.metadata.impl.MetadataGeneratorImpl;
import net.shibboleth.shared.component.ComponentInitializationException;
/**
 *
 */
public class Test {

    /**
     * @param args ...
     * 
     * @throws IOException ...
     * @throws ComponentInitializationException ...
     */
    public static void main(String[] args) throws IOException, ComponentInitializationException {

        //System.setProperty(InstallerPropertiesImpl.TARGET_DIR,"H:\\Downloads\\idp");
        System.setProperty(InstallerPropertiesImpl.SOURCE_DIR,
                "h:\\Perforce\\Juno\\V5\\java-identity-provider\\idp-distribution\\target\\shibboleth-identity-provider-5.0.0-SNAPSHOT");
        System.setProperty(InstallerPropertiesImpl.ANT_BASE_DIR,
                "h:\\Perforce\\Juno\\V5\\java-identity-provider\\idp-distribution\\target\\shibboleth-identity-provider-5.0.0-SNAPSHOT\\bin");
        System.setProperty(InstallerPropertiesImpl.KEY_STORE_PASSWORD, "p1");
        System.setProperty(InstallerPropertiesImpl.SEALER_PASSWORD, "p1");
        System.setProperty(InstallerPropertiesImpl.HOST_NAME, "machine.org.uk");

        final InstallerProperties ip = new InstallerPropertiesImpl(false);
        ip.initialize();
        final CurrentInstallState is = new CurrentInstallState(ip);
        is.initialize();

        final CopyDistribution dist = new CopyDistribution(ip, is);
        dist.initialize();
        dist.execute();

        final V4Install inst = new V4Install(ip, is);
        inst.setMetadataGenerator(new MetadataGeneratorImpl());
        inst.initialize();
        inst.execute();

        final BuildWar bw = new BuildWar(ip, is);
        bw.initialize();
        bw.execute();
    }

}
