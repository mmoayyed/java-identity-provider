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

package net.shibboleth.idp.cli.impl;

import java.io.PrintStream;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.slf4j.Logger;

import com.beust.jcommander.Parameter;

import net.shibboleth.idp.cli.AbstractIdPHomeAwareCommandLineArguments;
import net.shibboleth.shared.primitive.LoggerFactory;

/**
 * Command line arguments for {@link JarCheckCLI}.
 */
public class JarCheckArguments extends AbstractIdPHomeAwareCommandLineArguments {

    /** Logger. */
    @Nullable private Logger log;

    /** look inside the jars. */
    @Parameter(names= {"-d", "--detailed"})
    private boolean detailed;

    /** provide a sorted list. */
    @Parameter(names= {"-l", "--list"})
    private boolean list;

    /** {@inheritDoc} */
    @Nonnull public Logger getLog() {
        if (log == null) {
            log = LoggerFactory.getLogger(JarCheckArguments.class);
        }
        assert log!=null;
        return log;
    }
    
    /** Are we doing an in depth survey?.
     * @return Returns detailed.
     */
    public boolean isDetailed() {
        return detailed;
    }
    
    /** are we listing everything?
     * @return Returns list.
     */
    public boolean isList() {
        return list;
    }

    /** {@inheritDoc} */
    @Override
    public void printHelp(@Nonnull final PrintStream out) {
        out.println("JarCheck");
        out.println("Provides a command line interface to look for duplicates in an installation");
        out.println();
        out.println("   XXXX [options] ");
        out.println();
        super.printHelp(out);
        out.println();
        out.println(String.format("  %-22s %s", "-l, --list", "Output all modules in sorted order"));
        out.println(String.format("  %-22s %s", "-d, --details", "Do class level analysis"));
    }
}
