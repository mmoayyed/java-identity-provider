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

package net.shibboleth.idp.attribute.cli;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import net.shibboleth.idp.spring.SpringSupport;
import net.shibboleth.utilities.java.support.component.ComponentInitializationException;
import net.shibboleth.utilities.java.support.resource.FilesystemResource;
import net.shibboleth.utilities.java.support.resource.Resource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.webflow.executor.FlowExecutionResult;
import org.springframework.webflow.executor.FlowExecutor;
import org.springframework.webflow.test.MockExternalContext;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;


/**
 * Entry point for command line attribute utility.
 */
public final class Main {

    /** Class logger. */
    private static final Logger LOG = LoggerFactory.getLogger(Main.class);

    /** List of assumed Spring configuration files used with the AACLI. */
    private static final String[] CONFIG_FILES = { "internal.xml", "webflow-config.xml", };
    
    /** Command line argument container. */
    private static final CommandLineArgs COMMAND_LINE_ARGS = new CommandLineArgs();
    
    /** Constructor. */
    private Main() {
        
    }
    
    /**
     * Command line entry point.
     * 
     * @param args  command line arguments
     */
    public static void main(@Nonnull final String[] args) {

        // Parse command line.
        JCommander jc = new JCommander(COMMAND_LINE_ARGS, args);
        if (COMMAND_LINE_ARGS.help) {
            jc.setProgramName(Main.class.getName());
            jc.usage();
            return;
        }
                
        ApplicationContext appCtx = buildApplicationContext();
        
        FlowExecutor flowExecutor = appCtx.getBean("flowExecutor", FlowExecutor.class);
        MockExternalContext mockCtx = new MockExternalContext();
        FlowExecutionResult result = flowExecutor.launchExecution("cli-flow", null, mockCtx);
        if (result.isEnded()) {
            if ("end".equals(result.getOutcome().getId())) {
                System.out.print(mockCtx.getMockResponseWriter().toString());
            } else {
                System.err.println("Flow did not end successfully.");
            }
        } else {
            System.err.println("Flow did not end.");
        }
    }

    /**
     * Builds the Spring application context for the tool.
     * 
     * @return  a Spring app context
     */
    private static ApplicationContext buildApplicationContext() {
        
        // Verify config directory is available.
        File configDirectory = new File(COMMAND_LINE_ARGS.configDir);
        if (!configDirectory.exists() || !configDirectory.isDirectory() || !configDirectory.canRead()) {
            errorAndExit("Configuration directory " + configDirectory
                    + " does not exist, is not a directory, or is not readable", null);
        }

        // Combine built-in and extension configs in one list.
        List<String> configNames = new ArrayList<String>();
        for (String cfile : CONFIG_FILES) {
            configNames.add(cfile);
        }
        configNames.addAll(COMMAND_LINE_ARGS.springExts);
        
        List<Resource> configs = new ArrayList<Resource>();
        
        // Wrap each config file in a resource.
        for (String cfile : configNames) {
            File config;
            if (cfile.startsWith(File.separator)) {
                config = new File(cfile);
            } else {
                config = new File(configDirectory.getPath() + File.separator + cfile);
            }
            if (config.isDirectory() || !config.canRead()) {
                errorAndExit("Configuration file " + config.getAbsolutePath() + " is a directory or is not readable",
                        null);
            }
            Resource r = new FilesystemResource(config.getPath());
            try {
                r.initialize();
            } catch (ComponentInitializationException e) {
                errorAndExit("Configuration resource " + config.getPath() + " failed to initialize", e);
            }
            configs.add(r);
        }
                
        return SpringSupport.newContext(Main.class.getName(), configs, null);
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
        public static final String HELP = "--help";

        /** Configuration directory. */
        public static final String CONFIG_DIR = "--configDir";
        
        /** List of Spring extension files to load. */
        public static final String SPRING_EXT = "--springExt";

        /** Attribute requester identity. */
        public static final String REQUESTER = "--requester";

        /** Attribute issuer identity. */
        public static final String ISSUER = "--issuer";

        /** Identity of attribute subject. */
        public static final String PRINCIPAL = "--principal";

        /** Authentication method of attribute subject. */
        public static final String AUTHN_METHOD = "--authnMethod";

        /** Show results without external encoding.  */
        public static final String INTERNAL_ENC = "--internal";

        /** Show results with SAML 1.1 encoding.  */
        public static final String SAML1_ENC = "--saml1";

        /** Show results with SAML 2.0 encoding.  */
        public static final String SAML2_ENC = "--saml2";
        
        /** Display command usage. */
        @Parameter(names = HELP, description = "Display program usage", help = true)
        private boolean help;

        /** Configuration directory. */
        @Parameter(names = CONFIG_DIR, description = "Configuration directory")
        private String configDir = System.getenv("IDP_HOME") + "/conf";
        
        /** List of Spring extension files to load. */
        @Parameter(names = SPRING_EXT, description = "Spring extension file to load")
        private List<String> springExts = new ArrayList<String>();

        /** Attribute requester identity. */
        @Parameter(names = REQUESTER, description = "Attribute requester identity")
        private String requester;

        /** Attribute issuer identity. */
        @Parameter(names = ISSUER, description = "Attribute issuer identity")
        private String issuer;

        /** Identity of attribute subject. */
        @Parameter(names = PRINCIPAL, required = false, description = "Identity of attribute subject")
        private String principal;

        /** Authentication method of attribute subject. */
        @Parameter(names = AUTHN_METHOD, description = "Authentication method of attribute subject")
        private String authnMethod;
        
        /** Show results without external encoding. */
        @Parameter(names = INTERNAL_ENC, description = "Show results without external encoding")
        private boolean internalEncoding;

        /** Show results with SAML 1.1 encoding. */
        @Parameter(names = SAML1_ENC, description = "Show results with SAML 1.1 encoding")
        private boolean saml1Encoding;

        /** Show results with SAML 2.0 encoding. */
        @Parameter(names = SAML2_ENC, description = "Show results with SAML 2.0 encoding")
        private boolean saml2Encoding = true;
    }
}