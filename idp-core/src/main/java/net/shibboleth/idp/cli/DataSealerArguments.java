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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.beust.jcommander.Parameter;

import net.shibboleth.utilities.java.support.security.DataSealer;

/**
 * Arguments for DataSealer CLI.
 */
public class DataSealerArguments extends net.shibboleth.ext.spring.cli.AbstractCommandLineArguments {

    /**
     * Name of a specific {@link DataSealer}, if one has been requested.
     */
    @Parameter(names = "--dataSealer")
    @Nullable private String dataSealerName;

    /** The Log. */
    @Nullable private Logger log;

    /** Operation enum. */
    public enum OperationType {
        /** Wrap/encrypt. */
        WRAP,
        /** Unwrap/decrypt. */
        UNWRAP,
    }
    
    /** Requested operation. */
    @Nullable private OperationType operation;

    /** {@inheritDoc} */
    public Logger getLog() {
        if (log == null) {
            log = LoggerFactory.getLogger(DataSealerArguments.class);
        }
        return log;
    }

    /**
     * Get name of {@link DataSealer} bean to access.
     * 
     * @return bean name
     */
    @Nullable public String getDataSealerName() {
        return dataSealerName;
    }
    
    /**
     * Get operation to perform.
     * 
     * @return operation
     */
    @Nullable public OperationType getOperation() {
        return operation;
    }

    /** {@inheritDoc} */
    public void validate() throws IllegalArgumentException {
        super.validate();
        
        if (getOtherArgs().size() < 3) {
            throw new IllegalArgumentException("Missing one or more required arguments");
        }
        
        if ("enc".equals(getOtherArgs().get(1))) {
            operation = OperationType.WRAP;
        } else if ("dec".equals(getOtherArgs().get(1))) {
            operation = OperationType.UNWRAP;
        } else {
            throw new IllegalArgumentException("Invalid operation requested, must be one of enc|dec");
        }
    }

    /** {@inheritDoc} */
    public void printHelp(final PrintStream out) {
        out.println("DataSealerCLI");
        out.println("Provides a command line interface for DataSealer wrap/unwrap operations.");
        out.println();
        out.println("   DataSealerCLI [options] springConfiguration encrypt|decrypt string");
        out.println();
        out.println("      springConfiguration      name of Spring configuration resource to use");
        out.println("      enc|dec                  encrypt or decrypt operation");
        out.println("      string                   value to encrypt or decrypt");
        super.printHelp(out);
        out.println();
        out.println(String.format("  --%-20s %s", "dataSealer", "Specifies a non-default DataSealer bean to use."));
        out.println();
    }
}