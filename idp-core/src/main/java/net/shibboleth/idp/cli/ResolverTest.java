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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import net.shibboleth.utilities.java.support.annotation.constraint.NotEmpty;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;


/**
 * Entry point for command line attribute utility.
 */
public final class ResolverTest {

    /** Class logger. */
    @Nonnull private static final Logger LOG = LoggerFactory.getLogger(ResolverTest.class);
    
    /** Command line argument container. */
    @Nonnull private static final CommandLineArgs COMMAND_LINE_ARGS = new CommandLineArgs();
    
    /** Constructor. */
    private ResolverTest() {
        
    }

// Checkstyle: CyclomaticComplexity OFF
    /**
     * Command line entry point.
     * 
     * @param args  command line arguments
     */
    public static void main(@Nonnull final String[] args) {

        // Parse command line.
        final JCommander jc = new JCommander(COMMAND_LINE_ARGS, args);
        if (COMMAND_LINE_ARGS.help) {
            jc.setProgramName(ResolverTest.class.getName());
            jc.usage();
            return;
        }
              
        if (COMMAND_LINE_ARGS.saml1) {
            if (COMMAND_LINE_ARGS.saml2 || COMMAND_LINE_ARGS.protocol != null) {
                errorAndExit("The saml1, saml2, and protocol options are mutually exclusive", null);
            }
        } else if (COMMAND_LINE_ARGS.saml2) {
            if (COMMAND_LINE_ARGS.saml1 || COMMAND_LINE_ARGS.protocol != null) {
                errorAndExit("The saml1, saml2, and protocol options are mutually exclusive", null);
            }
        } else if (COMMAND_LINE_ARGS.protocol != null) {
            if (COMMAND_LINE_ARGS.saml1 || COMMAND_LINE_ARGS.saml2) {
                errorAndExit("The saml1, saml2, and protocol options are mutually exclusive", null);
            }
        }
        
        doRequest();
    }
// Checkstyle: CyclomaticComplexity OFF

    /**
     * Make a request using the arguments established.
     */
    private static void doRequest() {
        try {
            final StringBuilder builder = new StringBuilder("https://localhost:8443/idp/profile/admin/resolvertest?");
            builder.append("requester=").append(URLEncoder.encode(COMMAND_LINE_ARGS.requester, "UTF-8"));
            builder.append("&principal=").append(URLEncoder.encode(COMMAND_LINE_ARGS.principal, "UTF-8"));
            if (COMMAND_LINE_ARGS.index != null) {
                builder.append("&acsIndex").append(COMMAND_LINE_ARGS.index.toString());
            }
            if (COMMAND_LINE_ARGS.saml1) {
                builder.append("&saml1");
            } else if (COMMAND_LINE_ARGS.saml2) {
                builder.append("&saml2");
            } else if (COMMAND_LINE_ARGS.protocol != null) {
                builder.append("&protocol=").append(URLEncoder.encode(COMMAND_LINE_ARGS.protocol, "UTF-8"));
            }
            
            final URL url = new URL(builder.toString());
            try (final InputStream stream = url.openStream()) {
                try (final InputStreamReader reader = new InputStreamReader(stream)) {
                    try (final BufferedReader in = new BufferedReader(reader)) {
                        String line;
                        while((line = in.readLine()) != null) {
                            System.out.println(line);
                        }
                    }
                }
            }
        } catch (final MalformedURLException e) {
            errorAndExit("Malformed URL", e);
        } catch (final UnsupportedEncodingException e) {
            errorAndExit("Unsupported encoding", e);
        } catch (final IOException e) {
            errorAndExit("I/O error", e);
        }
    }
    
    /**
     * Logs, as an error, the error message and exits the program.
     * 
     * @param errorMessage error message
     * @param e exception that caused it
     */
    private static void errorAndExit(@Nonnull final String errorMessage, @Nullable final Exception e) {
        if (e == null) {
            LOG.error(errorMessage);
        } else {
            LOG.error(errorMessage, e);
        }

        System.out.flush();
        System.exit(1);
    }
    
    /** Manages command line parsing for application. */
    private static class CommandLineArgs {

        /** Display command usage. */
        @Nonnull @NotEmpty public static final String HELP = "--help";

        /** Attribute requester identity. */
        @Nonnull @NotEmpty public static final String REQUESTER = "--requester";

        /** Identity of attribute subject. */
        @Nonnull @NotEmpty public static final String PRINCIPAL = "--principal";

        /** Index into metadata. */
        @Nonnull @NotEmpty public static final String INDEX = "--acsIndex";
        
        /** Show results with a custom protocol encoding.  */
        @Nonnull @NotEmpty public static final String PROTOCOL = "--protocol";

        /** Show results with SAML 1.1 encoding.  */
        @Nonnull @NotEmpty public static final String SAML1 = "--saml1";

        /** Show results with SAML 2.0 encoding.  */
        @Nonnull @NotEmpty public static final String SAML2 = "--saml2";

        /** Configuration directory. */
        @Nonnull @NotEmpty public static final String CONFIG_DIR = "--configDir";
        
        /** List of Spring extension files to load. */
        @Nonnull @NotEmpty public static final String SPRING_EXT = "--springExt";

        /** Authentication method of attribute subject. */
        @Nonnull @NotEmpty public static final String AUTHN_METHOD = "--authnMethod";

        /** Attribute issuer identity. */
        @Nonnull @NotEmpty public static final String ISSUER = "--issuer";

        /** Display command usage. */
        @Parameter(names = HELP, description = "Display program usage", help = true)
        private boolean help;

        /** Attribute requester identity. */
        @Parameter(names = REQUESTER, required = true, description = "Relying party identity")
        @Nullable private String requester;

        /** Identity of attribute subject. */
        @Parameter(names = PRINCIPAL, required = true, description = "Subject principal name")
        @Nullable private String principal;

        /** Index into metadata.  */
        @Parameter(names = INDEX, description = "AttributeConsumingService index")
        @Nullable private Integer index;

        /** Show results with a custom protocol encoding.  */
        @Parameter(names = PROTOCOL, description = "Show results with a custom protocol encoding")
        @Nullable private String protocol;

        /** Show results with SAML 1.1 encoding. */
        @Parameter(names = SAML1, description = "Show results with SAML 1.1 encoding")
        private boolean saml1;

        /** Show results with SAML 2.0 encoding. */
        @Parameter(names = SAML2, description = "Show results with SAML 2.0 encoding")
        private boolean saml2;

        /**
         * Below are legacy options from the 2.x AACLI tool that are no longer supported.
         */
        
        /** Obsolete. */
        @Parameter(names = CONFIG_DIR, description = "This option is obsolete", hidden = true)
        @Nullable private String dummy1;
        
        /** Obsolete. */
        @Parameter(names = SPRING_EXT, description = "This option is obsolete", hidden = true)
        @Nullable private String dummy2;

        /** Obsolete. */
        @Parameter(names = ISSUER, description = "This option is obsolete", hidden = true)
        @Nullable private String dummy3;

        /** Obsolete. */
        @Parameter(names = AUTHN_METHOD, description = "This option is obsolete", hidden = true)
        @Nullable private String dummy4;
    }
    
}