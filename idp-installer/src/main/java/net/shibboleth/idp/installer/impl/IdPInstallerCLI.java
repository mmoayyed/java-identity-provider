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
import java.util.List;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.apache.hc.client5.http.classic.HttpClient;
import org.apache.tools.ant.BuildException;
import org.opensaml.security.httpclient.HttpClientSecurityParameters;
import org.slf4j.Logger;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;

import net.shibboleth.idp.Version;
import net.shibboleth.shared.cli.AbstractCommandLine;
import net.shibboleth.shared.collection.CollectionSupport;
import net.shibboleth.shared.component.ComponentInitializationException;
import net.shibboleth.shared.primitive.LoggerFactory;

/**
 * Command line installer.
 */
public class IdPInstallerCLI extends AbstractCommandLine<IdPInstallerArguments> {

    /** Logger. */
    @Nullable private Logger log;
    
    /** {@inheritDoc} */
    @Override
    @Nonnull protected Class<IdPInstallerArguments> getArgumentClass() {
        return IdPInstallerArguments.class;
    }

    /** {@inheritDoc} */
    @Override
    @Nonnull protected String getVersion() {
        final String result = Version.getVersion();
        assert result != null;
        return result;
    }

    /** {@inheritDoc} */
    @Override
    @Nonnull protected Logger getLogger() {
        Logger localLog = log;
        if (localLog == null) {
            localLog = log = LoggerFactory.getLogger(IdPInstallerCLI.class);
        }
        return localLog;
    }

    /** {@inheritDoc} */
    @Nonnull protected List<Resource> getAdditionalSpringResources() {
        return CollectionSupport.singletonList(
               new ClassPathResource("net/shibboleth/idp/conf/http-client.xml"));
    }

    /** {@inheritDoc} */
    protected int doRun(@Nonnull final IdPInstallerArguments args) {

        super.doRun(args);
        
        final Path source = Path.of(args.getSourceDir());
        assert source!=null;
        if (!Files.exists(source)) {
            getLogger().error("Could not find {}", source);
            return RC_INIT;
        }

        String clientName = args.getHttpClientName();
        if (clientName == null) {
            clientName = "shibboleth.InternalHttpClient";
        }
        
        @Nonnull final HttpClient httpClient;
        try {
            httpClient = getApplicationContext().getBean(clientName, HttpClient.class);
        } catch (final NoSuchBeanDefinitionException e) {
            getLogger().error("Could not locate HttpClient '{}'", args.getHttpClientName());
            return RC_IO;
        }

        final String securityParametersName = args.getHttpClientSecurityParametersName();
        HttpClientSecurityParameters clientSecurityParameters = null;
        if (securityParametersName != null) {
            try {
                clientSecurityParameters =
                        getApplicationContext().getBean(securityParametersName, HttpClientSecurityParameters.class);
            } catch (final NoSuchBeanDefinitionException e) {
                getLogger().error("Could not locate HttpClientSecurityParameters '{}'",
                        args.getHttpClientSecurityParametersName());
                return RC_IO;
            }
        }

        if (args.isUnattended()) {
            System.setProperty(InstallerProperties.NO_PROMPT, "true");
        }

        setIfNotNull(args.getPropertyFile(), InstallerProperties.PROPERTY_SOURCE_FILE);
        setIfNotNull(args.getTargetDirectory(), InstallerProperties.TARGET_DIR);
        setIfNotNull(args.getHostName(), InstallerProperties.HOST_NAME);
        setIfNotNull(args.getScope(), InstallerProperties.SCOPE);
        setIfNotNull(args.getEntityID(), InstallerProperties.ENTITY_ID);
        setIfNotNull(args.getKeystorePassword(), InstallerProperties.KEY_STORE_PASSWORD);
        setIfNotNull(args.getSealerPassword(), InstallerProperties.SEALER_PASSWORD);

        try {
            final InstallerProperties ip = new InstallerProperties(source);
            ip.doInitialize();
            final CurrentInstallState ic = new CurrentInstallState(ip);
            ic.initialize();

            final CopyDistribution cd = new CopyDistribution(ip);
            cd.execute();

            final V5Install install = new V5Install(ip, ic, httpClient, clientSecurityParameters);
            install.execute();

            final BuildWar bw = new BuildWar(ip.getTargetDir());
            bw.execute();

        } catch (final ComponentInitializationException e) {
            getLogger().error("Installation setup failed", e);
            return RC_IO;
        } catch (final BuildException e) {
            getLogger().error("Installation run failed", e);
            return RC_IO;
        }
        return RC_OK;
    }
    
    /** Helper for translating arguments to properties.  Look at the value and if it is non-null
     * set the associate property.
     * @param value the value to check and potentially set
     * @param propertyName the property name to set
     */
    private void setIfNotNull(@Nullable String value, @Nonnull String propertyName) {
        if (value == null) {
            return;
        }
        System.setProperty(propertyName, value);
    }

    /** Shim for CLI entry point: Allows the code to be run from a test.
    *
    * @return one of the predefines {@link AbstractCommandLine#RC_INIT},
    * {@link AbstractCommandLine#RC_IO}, {@link AbstractCommandLine#RC_OK}
    * or {@link AbstractCommandLine#RC_UNKNOWN}
    *
    * @param args arguments
    */
   public static int runMain(@Nonnull final String[] args) {
       final IdPInstallerCLI cli = new IdPInstallerCLI();

       return cli.run(args);
   }

   /**
    * CLI entry point.
    * @param args arguments
    */
   public static void main(@Nonnull final String[] args) {
       System.exit(runMain(args));
   }

}