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
import java.io.IOException;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.util.Map;

import org.apache.log4j.ConsoleAppender;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.log4j.PatternLayout;
import org.opensaml.Configuration;
import org.opensaml.DefaultBootstrap;
import org.opensaml.common.SAMLObject;
import org.opensaml.saml2.metadata.provider.MetadataProvider;
import org.opensaml.saml2.metadata.provider.MetadataProviderException;
import org.opensaml.xml.io.Marshaller;
import org.opensaml.xml.io.MarshallingException;
import org.opensaml.xml.util.DatatypeHelper;
import org.opensaml.xml.util.XMLHelper;
import org.springframework.beans.factory.xml.XmlBeanDefinitionReader;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.GenericApplicationContext;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.w3c.dom.Element;

import edu.internet2.middleware.shibboleth.common.attribute.provider.ShibbolethAttributeRequestContext;
import edu.internet2.middleware.shibboleth.common.config.SpringDocumentLoader;
import edu.internet2.middleware.shibboleth.common.relyingparty.ProfileConfiguration;
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
    private static Logger log = Logger.getLogger(AttributeAuthorityCLI.class);

    /** Loaded metadata provider. */
    private static MetadataProvider metadataProvider;

    /** Loaded SAML 1 Attribute Authority. */
    private static SAML1AttributeAuthority saml1AA;

    /** Loaded SAML 2 Attribute Authority. */
    private static SAML2AttributeAuthority saml2AA;

    /**
     * Runs this application. Help message prints if no arguments are given or if the "help" argument is given.
     * 
     * @param args command line arguements
     * 
     * @throws Exception thrown if there is a problem during program execution
     */
    public static void main(String[] args) throws Exception {
        CmdLineParser parser = initialize(args);
        ApplicationContext appCtx = loadConfigurations((String) parser.getOptionValue(CLIParserBuilder.CONFIG_DIR_ARG));

        metadataProvider = (MetadataProvider) appCtx.getBean("SAMLMetadataProvider");
        saml1AA = (SAML1AttributeAuthority) appCtx.getBean("SAML1AttributeAuthority");
        saml2AA = (SAML2AttributeAuthority) appCtx.getBean("SAML2AttributeAuthority");

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
     * Initialize the application.
     * 
     * @param args command line arguments
     * 
     * @return parsed command line arguments
     * 
     * @throws Exception thrown if the underlying libraries could not be initialized
     */
    private static CmdLineParser initialize(String[] args) throws Exception {
        DefaultBootstrap.bootstrap();
        configureLogging();

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
     */
    private static ApplicationContext loadConfigurations(String configDir) throws IOException {
        File configDirectory = new File(configDir);
        if (!configDirectory.exists() || !configDirectory.isDirectory() || !configDirectory.canRead()) {
            errorAndExit("Configuration directory " + configDir
                    + " does not exist, is not a directory, or is not readable", null);
        }

        File[] configs = configDirectory.listFiles();

        GenericApplicationContext gContext = new GenericApplicationContext();
        XmlBeanDefinitionReader configReader = new XmlBeanDefinitionReader(gContext);
        configReader.setDocumentLoader(new SpringDocumentLoader());

        File config;
        Resource[] configSources = new Resource[configs.length];
        for (int i = 0; i < configs.length; i++) {
            config = configs[i];
            if (configDirectory.isDirectory() || !configDirectory.canRead()) {
                errorAndExit("Configuration file " + config.getAbsolutePath() + " is a directory or is not readable",
                        null);
            }
            configSources[i] = new FileSystemResource(config);
        }

        configReader.loadBeanDefinitions(configSources);

        return gContext;
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
        ShibbolethAttributeRequestContext requestCtx = buildAttributeRequestContext(parser, appCtx);

        try {
            return saml1AA.performAttributeQuery(requestCtx);
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
        ShibbolethAttributeRequestContext requestCtx = buildAttributeRequestContext(parser, appCtx);

        try {
            return saml2AA.performAttributeQuery(requestCtx);
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
    private static ShibbolethAttributeRequestContext buildAttributeRequestContext(CmdLineParser parser,
            ApplicationContext appCtx) {
        String issuer = (String) parser.getOptionValue(CLIParserBuilder.ISSUER_ARG);
        String requester = (String) parser.getOptionValue(CLIParserBuilder.REQUESTER_ARG);
        SimpleRelyingPartyConfiguration rpConfig = new SimpleRelyingPartyConfiguration(issuer, requester);

        try {
            ShibbolethAttributeRequestContext attribReqCtx = new ShibbolethAttributeRequestContext(metadataProvider,
                    rpConfig);

            String principal = (String) parser.getOptionValue(CLIParserBuilder.PRINCIPAL_ARG);
            attribReqCtx.setPrincipalName(principal);

            String authnMethod = (String) parser.getOptionValue(CLIParserBuilder.AUTHN_METHOD_ARG);
            attribReqCtx.setPrincipalAuthenticationMethod(authnMethod);

            return attribReqCtx;
        } catch (MetadataProviderException e) {
            errorAndExit("Error looking up metadata for issuer or requester", e);
        }

        return null;
    }

    /**
     * Prints the given attribute statement to system output.
     * 
     * @param attributeStatement attribute statement to print
     */
    private static void printAttributeStatement(SAMLObject attributeStatement) {
        Marshaller statementMarshaller = Configuration.getMarshallerFactory().getMarshaller(attributeStatement);

        try {
            Element statement = statementMarshaller.marshall(attributeStatement);
            System.out.println(XMLHelper.nodeToString(statement));
        } catch (MarshallingException e) {
            errorAndExit("Unable to marshall attribute statement", e);
        }
    }

    /**
     * Configures the logging for this tool. Default logging level is error.
     */
    private static void configureLogging() {
        ConsoleAppender console = new ConsoleAppender();
        console.setWriter(new PrintWriter(System.err));
        console.setName("stderr");
        console.setLayout(new PatternLayout("%d{ABSOLUTE} %-5p [%c{1}] %m%n"));

        log = Logger.getLogger("edu.internet2.middleware.shibboleth.common.attribute");
        log.addAppender(console);
        log.setLevel(Level.ERROR);

        Logger.getRootLogger().setLevel(Level.OFF);
    }

    /**
     * Prints a help message to the given output stream.
     * 
     * @param out output to print the help message to
     */
    private static void printHelp(PrintStream out) {
        // TODO fix the usage statement to reflect invocation by script
        out.println("usage: java edu.internet2.middleware.shibboleth.common.attribute.AttributeAuthorityCLI");
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

    /**
     * Simple implemenation of {@link RelyingPartyConfiguration} that does not allow profile configurations.
     */
    private static class SimpleRelyingPartyConfiguration implements RelyingPartyConfiguration {

        /** Entity ID of the issuer. */
        private String issuerId;

        /** Entity ID of the relying party. */
        private String relyingPartyId;

        /**
         * Constructor.
         * 
         * @param issuer entity ID of the issuer
         * @param relyingParty entity ID of the relying party
         */
        public SimpleRelyingPartyConfiguration(String issuer, String relyingParty) {
            issuerId = DatatypeHelper.safeTrimOrNullString(issuer);
            relyingPartyId = DatatypeHelper.safeTrimOrNullString(relyingParty);
        }

        /** {@inheritDoc} */
        public String getProviderID() {
            return issuerId;
        }

        /** {@inheritDoc} */
        public String getRelyingPartyID() {
            return relyingPartyId;
        }

        /** {@inheritDoc} */
        public Map<String, ProfileConfiguration> getProfileConfigurations() {
            return null;
        }
    }

}