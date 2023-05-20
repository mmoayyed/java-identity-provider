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

import java.io.PrintStream;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.slf4j.Logger;

import net.shibboleth.idp.cli.AbstractIdPHomeAwareCommandLineArguments;
import net.shibboleth.shared.primitive.LoggerFactory;

/**
 * Command line arguments for the "build" verb.
 */
public class IdPBuildArguments extends AbstractIdPHomeAwareCommandLineArguments {

    /** Logger. */
    @Nullable private Logger log;

    /** {@inheritDoc} */
    public @Nonnull Logger getLog() {
        if (log == null) {
            log = LoggerFactory.getLogger(IdPBuildArguments.class);
        }
        assert log != null;
        return log;
    }
    
    /** {@inheritDoc} */
    @Override public void printHelp(final @Nonnull PrintStream out) {
        out.println("Build");
        out.println("Rebuilds the IdP war");
        out.println();
    }

}