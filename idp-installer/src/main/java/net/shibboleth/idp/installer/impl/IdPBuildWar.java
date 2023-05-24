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

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.slf4j.Logger;

import net.shibboleth.idp.Version;
import net.shibboleth.shared.cli.AbstractCommandLine;
import net.shibboleth.shared.primitive.LoggerFactory;

/**
 * Command line for 'build'
 */
public class IdPBuildWar extends AbstractCommandLine<IdPBuildArguments> {

    /** Logger. */
    @Nullable private Logger log;
    
    /** {@inheritDoc} */
    @Override
    @Nonnull
    protected Class<IdPBuildArguments> getArgumentClass() {
        return IdPBuildArguments.class;
    }

    /** {@inheritDoc} */
    @Override
    @Nonnull
    protected String getVersion() {
        final String result = Version.getVersion();
        assert result != null;
        return result;
    }

    /** {@inheritDoc} */
    @Override
    @Nonnull
    protected Logger getLogger() {
        Logger localLog = log;
        if (localLog == null) {
            localLog = log = LoggerFactory.getLogger(IdPBuildWar.class);
        }
        return localLog;
    }

    /** {@inheritDoc} */
    protected int doRun(@Nonnull final IdPBuildArguments args) {

        super.doRun(args);
        
        getLogger().debug("{}", args);
        getLogger().info("{}", args);

        if (args.getIdPHome() == null) {
            getLogger().error("--home must be specified");
            return RC_INIT;
        }

        final Path idpHome = Path.of(args.getIdPHome());
        assert idpHome!=null;
        if (!Files.exists(idpHome)) {
            getLogger().error("Could not find {}", idpHome);
            return RC_INIT;
        }
        final BuildWar build = new BuildWar(idpHome);
        build.execute();

        return RC_OK;
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
       final IdPBuildWar cli = new IdPBuildWar();

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
