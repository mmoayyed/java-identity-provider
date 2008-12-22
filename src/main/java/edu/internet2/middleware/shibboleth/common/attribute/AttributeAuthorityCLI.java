/*
 * Copyright [2007] [University Corporation for Advanced Internet Development, Inc.]
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package edu.internet2.middleware.shibboleth.common.attribute;

import jargs.gnu.CmdLineParser;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.opensaml.Configuration;
import org.opensaml.common.SAMLObject;
import org.opensaml.saml2.metadata.provider.MetadataProvider;
import org.opensaml.util.resource.FilesystemResource;
import org.opensaml.util.resource.Resource;
import org.opensaml.util.resource.ResourceException;
import org.opensaml.xml.io.Marshaller;
import org.opensaml.xml.io.MarshallingException;
import org.opensaml.xml.util.XMLHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.GenericApplicationContext;
import org.w3c.dom.Element;

import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.joran.JoranConfigurator;
import ch.qos.logback.core.joran.spi.JoranException;
import ch.qos.logback.core.status.ErrorStatus;
import ch.qos.logback.core.status.InfoStatus;
import ch.qos.logback.core.status.StatusManager;

import edu.internet2.middleware.shibboleth.common.attribute.provider.SAML1AttributeAuthority;
import edu.internet2.middleware.shibboleth.common.attribute.provider.SAML2AttributeAuthority;
import edu.internet2.middleware.shibboleth.common.config.SpringConfigurationUtils;
import edu.internet2.middleware.shibboleth.common.profile.provider.BaseSAMLProfileRequestContext;
import edu.internet2.middleware.shibboleth.common.relyingparty.RelyingPartyConfiguration;

/**
 * A command line tool that allows individuals to invoke an attribute authority and inspect the resultant attribute
 * statement.
 * 
 * This tool expects to retrieve the {@link MetadataProvider} it uses under the bean name SAMLMetadataProvider, a
 * {@link SAML1AttributeAuthority} under the bean name SAML1AttributeAuthority, and a {@link SAML2AttributeAuthority}
 * under the bean name SAML2AttributeAuthority.
 */
public class AttributeAuthorityCLI {

    /** Class logger. */
    private static Logger log = LoggerFactory.getLogger(AttributeAuthorityCLI.class);

    /** List of configuration files used with the AACLI. */
    private static String[] aacliConfigs = { "/internal.xml", "/service.xml", };

    /** Loaded SAML 1 Attribute Authority. */
    private static SAML1AttributeAuthority saml1AA;

    /** Loaded SAML 2 Attribute Authority. */
    private static SAML2AttributeAuthority saml2AA;

    /**
     * Runs this application. Help message prints if no arguments are given or if the "help" argument is given.
     * 
     * @param args command line arguments
     * 
     * @throws Exception thrown if there is a problem during program execution
     */
    public static void main(String[] args) throws Exception {
        CmdLineParser parser = parseCommandArguments(args);
        ApplicationContext appCtx = loadConfigurations((String) parser.getOptionValue(CLIParserBuilder.CONFIG_DIR_ARG));

        saml1AA = (SAML1AttributeAuthority) appCtx.getBean("shibboleth.SAML1AttributeAuthority");
        saml2AA = (SAML2AttributeAuthority) appCtx.getBean("shibboleth.SAML2AttributeAuthority");

        SAMLObject attributeStatement;
        Boolean saml1 = (Boolean) parser.getOptionValue(CLIParserBuilder.SAML1_ARG, Boolean.FALSE);
        if (saml1.booleanValue()) {
            attributeStatement = performSAML1AttributeResolution(parser, appCtx);
        } else {
            attributeStatement = performSAML2AttributeResolution(parser, appCtx);
        }

        printAttributeStatement(attributeStatement);
    }

    /**
     * Parses the command line arguments
     * 
     * @param args command line arguments
     * 
     * @return parsed command line arguments
     * 
     * @throws Exception thrown if the underlying libraries could not be initialized
     */
    private static CmdLineParser parseCommandArguments(String[] args) throws Exception {
        if (args.length < 2) {
            printHelp(System.out);
            System.out.flush();
            System.exit(0);
        }

        CmdLineParser parser = CLIParserBuilder.buildParser();

        try {
            parser.parse(args);
        } catch (CmdLineParser.OptionException e) {
            errorAndExit(e.getMessage(), e);
        }

        Boolean helpEnabled = (Boolean) parser.getOptionValue(CLIParserBuilder.HELP_ARG);
        if (helpEnabled != null) {
            printHelp(System.out);
            System.out.flush();
            System.exit(0);
        }

        return parser;
    }

    /**
     * Loads the configuration files into a Spring application context.
     * 
     * @param configDir directory containing spring configuration files
     * 
     * @return loaded application context
     * 
     * @throws IOException throw if there is an error loading the configuration files
     * @throws ResourceException if there is an error loading the configuration files
     */
    private static ApplicationContext loadConfigurations(String configDir) throws IOException, ResourceException {
        File configDirectory;

        if (configDir != null) {
            configDirectory = new File(configDir);
        } else {
            configDirectory = new File(System.getenv("IDP_HOME") + "/conf");
        }

        if (!configDirectory.exists() || !configDirectory.isDirectory() || !configDirectory.canRead()) {
            errorAndExit("Configuration directory " + configDir
                    + " does not exist, is not a directory, or is not readable", null);
        }
        
        loadLoggingConfiguration(configDirectory.getAbsolutePath());

        List<Resource> configs = new ArrayList<Resource>();

        File config;
        for (int i = 0; i < aacliConfigs.length; i++) {
            config = new File(configDirectory.getPath() + aacliConfigs[i]);
            if (config.isDirectory() || !config.canRead()) {
                errorAndExit("Configuration file " + config.getAbsolutePath() + " is a directory or is not readable",
                        null);
            }
            configs.add(new FilesystemResource(config.getPath()));
        }

        GenericApplicationContext gContext = new GenericApplicationContext();
        SpringConfigurationUtils.populateRegistry(gContext, configs);
        gContext.refresh();
        return gContext;
    }
    

    /**
     * Loads the logging configuration.
     * 
     * @param configDir IdP configuration directory
     */
    private static void loadLoggingConfiguration(String configDir) {
        String loggingConfig = configDir + File.pathSeparator + "logging.xml";

        LoggerContext loggerContext = (LoggerContext) LoggerFactory.getILoggerFactory();
        StatusManager statusManager = loggerContext.getStatusManager();
        statusManager.add(new InfoStatus("Loading logging configuration file: " + loggingConfig, null));
        try {
            //loggerContext.stop();
            loggerContext.reset();
            JoranConfigurator configurator = new JoranConfigurator();
            configurator.setContext(loggerContext);
            configurator.doConfigure(new FileInputStream(loggingConfig));
            loggerContext.start();
        } catch (JoranException e) {
            statusManager.add(new ErrorStatus("Error loading logging configuration file: " + configDir, null, e));
        } catch (IOException e) {
            statusManager.add(new ErrorStatus("Error loading logging configuration file: " + configDir, null, e));
        }
    }

    /**
     * Constructs a SAML 1 attribute statement with the retrieved and filtered attributes.
     * 
     * @param parser command line arguments
     * @param appCtx spring application context with loaded attribute authority
     * 
     * @return SAML 1 attribute statement
     */
    private static SAMLObject performSAML1AttributeResolution(CmdLineParser parser, ApplicationContext appCtx) {
        BaseSAMLProfileRequestContext requestCtx = buildAttributeRequestContext(parser, appCtx);

        try {
            Map<String, BaseAttribute> attributes = saml1AA.getAttributes(requestCtx);
            return saml1AA.buildAttributeStatement(null, attributes.values());
        } catch (AttributeRequestException e) {
            errorAndExit("Error encountered during attribute resolution and filtering", e);
        }

        return null;
    }

    /**
     * Constructs a SAML 2 attribute statement with the retrieved and filtered attributes.
     * 
     * @param parser command line arguments
     * @param appCtx spring application context with loaded attribute authority
     * 
     * @return SAML 2 attribute statement
     */
    private static SAMLObject performSAML2AttributeResolution(CmdLineParser parser, ApplicationContext appCtx) {
        BaseSAMLProfileRequestContext requestCtx = buildAttributeRequestContext(parser, appCtx);

        try {
            Map<String, BaseAttribute> attributes = saml2AA.getAttributes(requestCtx);
            return saml2AA.buildAttributeStatement(null, attributes.values());
        } catch (AttributeRequestException e) {
            errorAndExit("Error encountered during attribute resolution and filtering", e);
        }

        return null;
    }

    /**
     * Builds the attribute request context from the command line arguments.
     * 
     * @param parser command line argument parser
     * @param appCtx spring application context
     * 
     * @return attribute request context
     */
    private static BaseSAMLProfileRequestContext buildAttributeRequestContext(CmdLineParser parser,
            ApplicationContext appCtx) {
        String issuer = (String) parser.getOptionValue(CLIParserBuilder.ISSUER_ARG);
        String requester = (String) parser.getOptionValue(CLIParserBuilder.REQUESTER_ARG);

        RelyingPartyConfiguration rpConfig = new RelyingPartyConfiguration(requester, issuer);

        BaseSAMLProfileRequestContext attribReqCtx = new BaseSAMLProfileRequestContext();
        attribReqCtx.setInboundMessageIssuer(requester);
        attribReqCtx.setOutboundMessageIssuer(issuer);
        attribReqCtx.setLocalEntityId(issuer);
        attribReqCtx.setRelyingPartyConfiguration(rpConfig);

        String principal = (String) parser.getOptionValue(CLIParserBuilder.PRINCIPAL_ARG);
        attribReqCtx.setPrincipalName(principal);

        String authnMethod = (String) parser.getOptionValue(CLIParserBuilder.AUTHN_METHOD_ARG);
        attribReqCtx.setPrincipalAuthenticationMethod(authnMethod);

        return attribReqCtx;
    }

    /**
     * Prints the given attribute statement to system output.
     * 
     * @param attributeStatement attribute statement to print
     */
    private static void printAttributeStatement(SAMLObject attributeStatement) {
        if (attributeStatement == null) {
            System.out.println("No attribute statement.");
            return;
        }

        Marshaller statementMarshaller = Configuration.getMarshallerFactory().getMarshaller(attributeStatement);

        try {
            Element statement = statementMarshaller.marshall(attributeStatement);
            System.out.println();
            System.out.println(XMLHelper.prettyPrintXML(statement));
        } catch (MarshallingException e) {
            errorAndExit("Unable to marshall attribute statement", e);
        }
    }

    /**
     * Prints a help message to the given output stream.
     * 
     * @param out output to print the help message to
     */
    private static void printHelp(PrintStream out) {
        out.println("Attribute Authority, Command Line Interface");
        out.println("  This tools provides a command line interface to the Shibboleth Attribute Authority,");
        out.println("  providing deployers a means to test their attribute resolution and configurations.");
        out.println();
        out.println("usage:");
        out.println("  On Unix systems:       ./aacli.sh <PARAMETERS>");
        out.println("  On Windows systems:    .\\aacli.bat <PARAMETERS>");
        out.println();
        out.println("Required Parameters:");
        out.println(String.format("  --%-16s %s", CLIParserBuilder.CONFIG_DIR,
                "Directory containing attribute authority configuration files"));
        out.println(String.format("  --%-16s %s", CLIParserBuilder.PRINCIPAL,
                "Principal name (user id) of the person whose attributes will be retrieved"));

        out.println();

        out.println("Optional Parameters:");
        out.println(String.format("  --%-16s %s", CLIParserBuilder.HELP, "Print this message"));
        out.println(String.format("  --%-16s %s", CLIParserBuilder.REQUESTER,
                "SAML entity ID of the relying party requesting the attributes. For example, the SPs entity ID"));
        out.println(String.format("  --%-16s %s", CLIParserBuilder.ISSUER,
                "SAML entity ID of the attribute issuer. For example, the IdPs entity ID"));
        out.println(String
                .format("  --%-16s %s", CLIParserBuilder.AUTHN_METHOD, "Method used to authenticate the user"));
        out
                .println(String
                        .format("  --%-16s %s", CLIParserBuilder.SAML1,
                                "No-value parameter indicating the attribute authority should answer as if it received a SAML 1 request"));

        out.println();
    }

    /**
     * Logs, as an error, the error message and exits the program.
     * 
     * @param errorMessage error message
     * @param e exception that caused it
     */
    private static void errorAndExit(String errorMessage, Exception e) {
        if (e == null) {
            log.error(errorMessage);
        } else {
            log.error(errorMessage, e);
        }

        System.out.flush();
        System.exit(1);
    }

    /**
     * Helper class that creates the command line argument parser.
     */
    private static class CLIParserBuilder {

        // Command line arguments
        public static final String HELP = "help";

        public static final String CONFIG_DIR = "configDir";

        public static final String REQUESTER = "requester";

        public static final String ISSUER = "issuer";

        public static final String PRINCIPAL = "principal";

        public static final String AUTHN_METHOD = "authnMethod";

        public static final String SAML1 = "saml1";

        // Command line parser arguments
        public static CmdLineParser.Option HELP_ARG;

        public static CmdLineParser.Option CONFIG_DIR_ARG;

        public static CmdLineParser.Option REQUESTER_ARG;

        public static CmdLineParser.Option ISSUER_ARG;

        public static CmdLineParser.Option PRINCIPAL_ARG;

        public static CmdLineParser.Option AUTHN_METHOD_ARG;

        public static CmdLineParser.Option SAML1_ARG;

        /**
         * Create a new command line parser.
         * 
         * @return command line parser
         */
        public static CmdLineParser buildParser() {
            CmdLineParser parser = new CmdLineParser();

            HELP_ARG = parser.addBooleanOption(HELP);
            CONFIG_DIR_ARG = parser.addStringOption(CONFIG_DIR);
            REQUESTER_ARG = parser.addStringOption(REQUESTER);
            ISSUER_ARG = parser.addStringOption(ISSUER);
            PRINCIPAL_ARG = parser.addStringOption(PRINCIPAL);
            AUTHN_METHOD_ARG = parser.addStringOption(AUTHN_METHOD);
            SAML1_ARG = parser.addBooleanOption(SAML1);

            return parser;
        }
    }
}