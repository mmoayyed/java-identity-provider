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

package net.shibboleth.idp.cli;

import java.io.PrintStream;

import javax.annotation.Nullable;

import com.beust.jcommander.Parameter;

/**
 * An extension to {@link net.shibboleth.ext.spring.cli.AbstractCommandLineArguments}
 * that allows idp.home override.
 * 
 * @since 4.1.0
 */
public abstract class AbstractIdPHomeAwareCommandLineArguments
        extends net.shibboleth.ext.spring.cli.AbstractCommandLineArguments {

    /** IdP location. */
    @Parameter(names = "--home")
    @Nullable private String idpHome;
    
    /** 
     * Gets the configured home location.
     * 
     * @return home location
     */
    @Nullable public String getIdPHome() {
        return idpHome;
    }

    /** {@inheritDoc} */
    @Override
    public void printHelp(final PrintStream out) {
        super.printHelp(out);
        out.println(String.format("  --%-20s %s", "home",
                "Sets idp.home if not installed to default location."));
        out.println();
    }
}