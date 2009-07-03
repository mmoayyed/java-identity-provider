/*
 * Copyright 2009 University Corporation for Advanced Internet Development, Inc.
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

package edu.internet2.middleware.shibboleth.common.config.resource;

import java.io.File;
import java.util.ArrayList;

import javax.xml.namespace.QName;

import org.opensaml.xml.util.DatatypeHelper;
import org.opensaml.xml.util.XMLHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.BeanCreationException;
import org.springframework.beans.factory.support.AbstractBeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.xml.ParserContext;
import org.tmatesoft.svn.core.SVNException;
import org.tmatesoft.svn.core.SVNURL;
import org.tmatesoft.svn.core.auth.SVNAuthentication;
import org.tmatesoft.svn.core.auth.SVNPasswordAuthentication;
import org.tmatesoft.svn.core.auth.SVNUserNameAuthentication;
import org.tmatesoft.svn.core.wc.SVNClientManager;
import org.w3c.dom.Element;

import edu.internet2.middleware.shibboleth.common.resource.SVNBasicAuthenticationManager;
import edu.internet2.middleware.shibboleth.common.resource.SVNResource;

/** Bean definition parser for {@link SVNResource}s. */
public class SVNResourceBeanDefinitionParser extends AbstractResourceBeanDefinitionParser {

    /** Schema type. */
    public static final QName SCHEMA_TYPE = new QName(ResourceNamespaceHandler.NAMESPACE, "SVNResource");

    /** Configuration element attribute {@value} which holds the URL to the remote repository. */
    public static final String REPOSITORY_URL_ATTRIB_NAME = "repositoryURL";

    /** Configuration element attribute {@value} which holds the timeout used when connecting to the SVN server. */
    public static final String CTX_TIMEOUT_ATTRIB_NAME = "connectionTimeout";

    /** Configuration element attribute {@value} which holds the timeout used when reading from the SVN server. */
    public static final String READ_TIMEOUT_ATTRIB_NAME = "readTimeout";

    /** Configuration element attribute {@value} which holds the path to the working copy directory. */
    public static final String WORKING_COPY_DIR_ATTRIB_NAME = "workingCopyDirectory";

    /** Configuration element attribute {@value} which holds the path to the working copy directory. */
    public static final String REVISION_ATTRIB_NAME = "revision";

    /**
     * Configuration element attribute {@value} which holds the path to the resource file represented by the SVN
     * resource.
     */
    public static final String RESOURCE_FILE_ATTRIB_NAME = "resourceFile";

    /** Configuration element attribute {@value} which holds the SVN username. */
    public static final String USERNAME_ATTRIB_NAME = "username";

    /** Configuration element attribute {@value} which holds the SVN password. */
    public static final String PASSWORD_ATTRIB_NAME = "password";

    /**
     * Configuration element attribute {@value} which holds the hostname of the proxy server used when connecting to the
     * SVN server.
     */
    public static final String PROXY_HOST_ATTRIB_NAME = "proxyHost";

    /**
     * Configuration element attribute {@value} which holds the port of the proxy server used when connecting to the SVN
     * server.
     */
    public static final String PROXY_PORT_ATTRIB_NAME = "proxyPort";

    /**
     * Configuration element attribute {@value} which holds the username used with the proxy server used when connecting
     * to the SVN server.
     */
    public static final String PROXY_USERNAME_ATTRIB_NAME = "proxyUsername";

    /**
     * Configuration element attribute {@value} which holds the password used with the proxy server used when connecting
     * to the SVN server.
     */
    public static final String PROXY_PASSWORD_ATTRIB_NAME = "proxyPassword";

    /** Default value of {@value #CTX_TIMEOUT_ATTRIB_NAME}, {@value} milliseconds. */
    public static final int DEFAULT_CTX_TIMEOUT = 3000;

    /** Default value of {@value #READ_TIMEOUT_ATTRIB_NAME}, {@value} milliseconds. */
    public static final int DEFAULT_READ_TIMEOUT = 5000;

    /** Default value of {@value #PROXY_PORT_ATTRIB_NAME}, {@value} . */
    public static final int DEFAULT_PROXY_PORT = 8080;

    /** Class logger. */
    private final Logger log = LoggerFactory.getLogger(SVNResourceBeanDefinitionParser.class);

    /** {@inheritDoc} */
    protected Class getBeanClass(Element arg0) {
        return SVNResource.class;
    }

    /** {@inheritDoc} */
    protected String resolveId(Element configElement, AbstractBeanDefinition beanDefinition, ParserContext parserContext) {
        return SVNResource.class.getName() + ":"
                + DatatypeHelper.safeTrimOrNullString(configElement.getAttributeNS(null, REPOSITORY_URL_ATTRIB_NAME));
    }

    /** {@inheritDoc} */
    protected void doParse(Element configElement, ParserContext parserContext, BeanDefinitionBuilder builder)
            throws BeanCreationException {
        super.doParse(configElement, parserContext, builder);

        builder.addConstructorArgValue(buildClientManager(configElement));

        builder.addConstructorArgValue(getRespositoryUrl(configElement));

        builder.addConstructorArgValue(getWorkingCopyDirectory(configElement));

        builder.addConstructorArgValue(getRevision(configElement));

        builder.addConstructorArgValue(getResourceFile(configElement));
        
        addResourceFilter(configElement, parserContext, builder);
    }

    /**
     * Builds the SVN client manager from the given configuration options.
     * 
     * @param configElement element bearing the configuration options
     * 
     * @return the SVN client manager
     */
    protected SVNClientManager buildClientManager(Element configElement) {
        ArrayList<SVNAuthentication> authnMethods = new ArrayList<SVNAuthentication>();
        String username = getUsername(configElement);
        if (username != null) {
            authnMethods.add(new SVNUserNameAuthentication(username, false));

            String password = getPassword(configElement);
            if (password != null) {
                authnMethods.add(new SVNPasswordAuthentication(username, password, false));
            }
        }

        String proxyHost = getProxyHost(configElement);
        int proxyPort = getProxyPort(configElement);
        String proxyUser = getProxyUsername(configElement);
        String proxyPassword = getPassword(configElement);

        SVNBasicAuthenticationManager authnManager;
        if (proxyHost == null) {
            authnManager = new SVNBasicAuthenticationManager(authnMethods);
        } else {
            authnManager = new SVNBasicAuthenticationManager(authnMethods, proxyHost, proxyPort, proxyUser,
                    proxyPassword);
        }
        authnManager.setConnectionTimeout(getConnectionTimeout(configElement));
        authnManager.setReadTimeout(getReadTimeout(configElement));

        SVNClientManager clientManager = SVNClientManager.newInstance();
        clientManager.setAuthenticationManager(authnManager);
        return clientManager;
    }

    /**
     * Gets the value of the {@value #REPOSITORY_URL_ATTRIB_NAME} attribute.
     * 
     * @param configElement resource configuration element
     * 
     * @return value of the attribute
     * 
     * @throws BeanCreationException thrown if the attribute is missing or contains an invalid SVN URL
     */
    protected SVNURL getRespositoryUrl(Element configElement) throws BeanCreationException {
        if (!configElement.hasAttributeNS(null, REPOSITORY_URL_ATTRIB_NAME)) {
            log.error("SVN resource definition missing required '" + REPOSITORY_URL_ATTRIB_NAME + "' attribute");
            throw new BeanCreationException("SVN resource definition missing required '" + REPOSITORY_URL_ATTRIB_NAME
                    + "' attribute");
        }

        String repositoryUrl = DatatypeHelper.safeTrimOrNullString(configElement.getAttributeNS(null,
                REPOSITORY_URL_ATTRIB_NAME));
        try {
            return SVNURL.parseURIDecoded(repositoryUrl);
        } catch (SVNException e) {
            log.error("SVN remote repository URL " + repositoryUrl + " is not valid", e);
            throw new BeanCreationException("SVN remote repository URL " + repositoryUrl + " is not valid", e);
        }
    }

    /**
     * Gets the value of the {@value #CTX_TIMEOUT_ATTRIB_NAME} attribute.
     * 
     * @param configElement resource configuration element
     * 
     * @return value of the attribute, or {@value #DEFAULT_CTX_TIMEOUT} if the attribute is not defined
     * 
     * @throws BeanCreationException thrown if the attribute is present but contains an empty string
     */
    protected int getConnectionTimeout(Element configElement) throws BeanCreationException {
        if (!configElement.hasAttributeNS(null, CTX_TIMEOUT_ATTRIB_NAME)) {
            return DEFAULT_CTX_TIMEOUT;
        }

        String timeout = DatatypeHelper.safeTrimOrNullString(configElement
                .getAttributeNS(null, CTX_TIMEOUT_ATTRIB_NAME));
        if (timeout == null) {
            log.error("SVN resource definition attribute '" + CTX_TIMEOUT_ATTRIB_NAME + "' may not be an empty string");
            throw new BeanCreationException("SVN resource definition attribute '" + CTX_TIMEOUT_ATTRIB_NAME
                    + "' may not be an empty string");
        }

        try {
            return (int) XMLHelper.durationToLong(timeout);
        } catch (IllegalArgumentException e) {
            log.error("SVN resource definition attribute '" + CTX_TIMEOUT_ATTRIB_NAME
                    + "' does not contain a proper XML duration value");
            throw new BeanCreationException("SVN resource definition attribute '" + CTX_TIMEOUT_ATTRIB_NAME
                    + "' does not contain a proper XML duration value");
        }
    }

    /**
     * Gets the value of the {@value #READ_TIMEOUT_ATTRIB_NAME} attribute.
     * 
     * @param configElement resource configuration element
     * 
     * @return value of the attribute, or {@value #DEFAULT_READ_TIMEOUT} if the attribute is not defined
     * 
     * @throws BeanCreationException thrown if the attribute is present but contains an empty string
     */
    protected int getReadTimeout(Element configElement) throws BeanCreationException {
        if (!configElement.hasAttributeNS(null, READ_TIMEOUT_ATTRIB_NAME)) {
            return DEFAULT_READ_TIMEOUT;
        }

        String timeout = DatatypeHelper.safeTrimOrNullString(configElement.getAttributeNS(null,
                READ_TIMEOUT_ATTRIB_NAME));
        if (timeout == null) {
            log
                    .error("SVN resource definition attribute '" + READ_TIMEOUT_ATTRIB_NAME
                            + "' may not be an empty string");
            throw new BeanCreationException("SVN resource definition attribute '" + READ_TIMEOUT_ATTRIB_NAME
                    + "' may not be an empty string");
        }

        try {
            return (int) XMLHelper.durationToLong(timeout);
        } catch (IllegalArgumentException e) {
            log.error("SVN resource definition attribute '" + READ_TIMEOUT_ATTRIB_NAME
                    + "' does not contain a proper XML duration value");
            throw new BeanCreationException("SVN resource definition attribute '" + READ_TIMEOUT_ATTRIB_NAME
                    + "' does not contain a proper XML duration value");
        }
    }

    /**
     * Gets the value of the {@value #REPOSITORY_URL_ATTRIB_NAME} attribute.
     * 
     * @param configElement resource configuration element
     * 
     * @return value of the attribute
     * 
     * @throws BeanCreationException thrown if the attribute is missing or contains an invalid directory path
     */
    protected File getWorkingCopyDirectory(Element configElement) throws BeanCreationException {
        if (!configElement.hasAttributeNS(null, WORKING_COPY_DIR_ATTRIB_NAME)) {
            log.error("SVN resource definition missing required '" + WORKING_COPY_DIR_ATTRIB_NAME + "' attribute");
            throw new BeanCreationException("SVN resource definition missing required '" + WORKING_COPY_DIR_ATTRIB_NAME
                    + "' attribute");
        }

        File directory = new File(DatatypeHelper.safeTrimOrNullString(configElement.getAttributeNS(null,
                WORKING_COPY_DIR_ATTRIB_NAME)));
        if (directory == null) {
            log.error("SVN working copy directory may not be null");
            throw new BeanCreationException("SVN working copy directory may not be null");
        }

        if (!directory.exists()) {
            boolean created = directory.mkdirs();
            if (!created) {
                log.error("SVN working copy direction " + directory.getAbsolutePath()
                        + " does not exist and could not be created");
                throw new BeanCreationException("SVN working copy direction " + directory.getAbsolutePath()
                        + " does not exist and could not be created");
            }
        }

        if (!directory.isDirectory()) {
            log.error("SVN working copy location " + directory.getAbsolutePath() + " is not a directory");
            throw new BeanCreationException("SVN working copy location " + directory.getAbsolutePath()
                    + " is not a directory");
        }

        if (!directory.canRead()) {
            log.error("SVN working copy directory " + directory.getAbsolutePath() + " can not be read by this process");
            throw new BeanCreationException("SVN working copy directory " + directory.getAbsolutePath()
                    + " can not be read by this process");
        }

        if (!directory.canWrite()) {
            log.error("SVN working copy directory " + directory.getAbsolutePath()
                    + " can not be written to by this process");
            throw new BeanCreationException("SVN working copy directory " + directory.getAbsolutePath()
                    + " can not be written to by this process");
        }

        return directory;
    }

    /**
     * Gets the value of the {@value #REVISION_ATTRIB_NAME} attribute.
     * 
     * @param configElement resource configuration element
     * 
     * @return value of the attribute
     * 
     * @throws BeanCreationException thrown if the attribute is missing or contains an invalid number
     */
    protected long getRevision(Element configElement) throws BeanCreationException {
        if (!configElement.hasAttributeNS(null, REVISION_ATTRIB_NAME)) {
            log.error("SVN resource definition missing required '" + REVISION_ATTRIB_NAME + "' attribute");
            throw new BeanCreationException("SVN resource definition missing required '" + REVISION_ATTRIB_NAME
                    + "' attribute");
        }

        try {
            return Long.parseLong(DatatypeHelper.safeTrimOrNullString(configElement.getAttributeNS(null,
                    WORKING_COPY_DIR_ATTRIB_NAME)));
        } catch (NumberFormatException e) {
            log.error("SVN resource definition attribute '" + REVISION_ATTRIB_NAME + "' contains an invalid number");
            throw new BeanCreationException("SVN resource definition attribute '" + REVISION_ATTRIB_NAME
                    + "' contains an invalid number");
        }
    }

    /**
     * Gets the value of the {@value #RESOURCE_FILE_ATTRIB_NAME} attribute.
     * 
     * @param configElement resource configuration element
     * 
     * @return value of the attribute
     * 
     * @throws BeanCreationException thrown if the attribute is missing or contains an empty string
     */
    protected String getResourceFile(Element configElement) throws BeanCreationException {
        if (!configElement.hasAttributeNS(null, RESOURCE_FILE_ATTRIB_NAME)) {
            log.error("SVN resource definition missing required '" + RESOURCE_FILE_ATTRIB_NAME + "' attribute");
            throw new BeanCreationException("SVN resource definition missing required '" + RESOURCE_FILE_ATTRIB_NAME
                    + "' attribute");
        }

        String filename = DatatypeHelper.safeTrimOrNullString(configElement.getAttributeNS(null,
                RESOURCE_FILE_ATTRIB_NAME));
        if (filename == null) {
            log.error("SVN resource definition attribute '" + RESOURCE_FILE_ATTRIB_NAME
                    + "' may not be an empty string");
            throw new BeanCreationException("SVN resource definition attribute '" + RESOURCE_FILE_ATTRIB_NAME
                    + "' may not be an empty string");
        }

        return filename;
    }

    /**
     * Gets the value of the {@value #USERNAME_ATTRIB_NAME} attribute.
     * 
     * @param configElement resource configuration element
     * 
     * @return value of the attribute
     * 
     * @throws BeanCreationException thrown if the attribute is present but contains an empty string
     */
    protected String getUsername(Element configElement) throws BeanCreationException {
        String username = DatatypeHelper.safeTrimOrNullString(configElement.getAttributeNS(null, USERNAME_ATTRIB_NAME));
        if (username == null) {
            log.error("SVN resource definition attribute '" + USERNAME_ATTRIB_NAME + "' may not be an empty string");
            throw new BeanCreationException("SVN resource definition attribute '" + USERNAME_ATTRIB_NAME
                    + "' may not be an empty string");
        }
        return username;
    }

    /**
     * Gets the value of the {@value #PASSWORD_ATTRIB_NAME} attribute.
     * 
     * @param configElement resource configuration element
     * 
     * @return value of the attribute
     * 
     * @throws BeanCreationException thrown if the attribute is present but contains an empty string
     */
    protected String getPassword(Element configElement) throws BeanCreationException {
        String password = DatatypeHelper.safeTrimOrNullString(configElement.getAttributeNS(null, PASSWORD_ATTRIB_NAME));
        if (password == null) {
            log.error("SVN resource definition attribute '" + PASSWORD_ATTRIB_NAME + "' may not be an empty string");
            throw new BeanCreationException("SVN resource definition attribute '" + PASSWORD_ATTRIB_NAME
                    + "' may not be an empty string");
        }
        return password;
    }

    /**
     * Gets the value of the {@value #PROXY_HOST_ATTRIB_NAME} attribute.
     * 
     * @param configElement resource configuration element
     * 
     * @return value of the attribute
     * 
     * @throws BeanCreationException thrown if the attribute is present but contains an empty string
     */
    protected String getProxyHost(Element configElement) throws BeanCreationException {
        String host = DatatypeHelper.safeTrimOrNullString(configElement.getAttributeNS(null, PROXY_HOST_ATTRIB_NAME));
        if (host == null) {
            log.error("SVN resource definition attribute '" + PROXY_HOST_ATTRIB_NAME + "' may not be an empty string");
            throw new BeanCreationException("SVN resource definition attribute '" + PROXY_HOST_ATTRIB_NAME
                    + "' may not be an empty string");
        }
        return host;
    }

    /**
     * Gets the value of the {@value #PROXY_PORT_ATTRIB_NAME} attribute.
     * 
     * @param configElement resource configuration element
     * 
     * @return value of the attribute, or {@value #DEFAULT_PROXY_PORT} if the attribute is not defined
     * 
     * @throws BeanCreationException thrown if the attribute is present but contains an empty string
     */
    protected int getProxyPort(Element configElement) throws BeanCreationException {
        if (!configElement.hasAttributeNS(null, PROXY_PORT_ATTRIB_NAME)) {
            return DEFAULT_PROXY_PORT;
        }

        String port = DatatypeHelper.safeTrimOrNullString(configElement.getAttributeNS(null, PROXY_PORT_ATTRIB_NAME));
        if (port == null) {
            log.error("SVN resource definition attribute '" + PROXY_PORT_ATTRIB_NAME + "' may not be an empty string");
            throw new BeanCreationException("SVN resource definition attribute '" + PROXY_PORT_ATTRIB_NAME
                    + "' may not be an empty string");
        }

        try {
            return Integer.parseInt(port);
        } catch (NumberFormatException e) {
            log.error("SVN resource definition attribute '" + PROXY_PORT_ATTRIB_NAME + "' contains an invalid number");
            throw new BeanCreationException("SVN resource definition attribute '" + PROXY_PORT_ATTRIB_NAME
                    + "' contains an invalid number");
        }
    }

    /**
     * Gets the value of the {@value #PROXY_USERNAME_ATTRIB_NAME} attribute.
     * 
     * @param configElement resource configuration element
     * 
     * @return value of the attribute
     * 
     * @throws BeanCreationException thrown if the attribute is present but contains an empty string
     */
    protected String getProxyUsername(Element configElement) throws BeanCreationException {
        String username = DatatypeHelper.safeTrimOrNullString(configElement.getAttributeNS(null,
                PROXY_USERNAME_ATTRIB_NAME));
        if (username == null) {
            log.error("SVN resource definition attribute '" + PROXY_USERNAME_ATTRIB_NAME
                    + "' may not be an empty string");
            throw new BeanCreationException("SVN resource definition attribute '" + PROXY_USERNAME_ATTRIB_NAME
                    + "' may not be an empty string");
        }
        return username;
    }

    /**
     * Gets the value of the {@value #PROXY_PASSWORD_ATTRIB_NAME} attribute.
     * 
     * @param configElement resource configuration element
     * 
     * @return value of the attribute
     * 
     * @throws BeanCreationException thrown if the attribute is present but contains an empty string
     */
    protected String getProxyPassword(Element configElement) throws BeanCreationException {
        String password = DatatypeHelper.safeTrimOrNullString(configElement.getAttributeNS(null,
                PROXY_PASSWORD_ATTRIB_NAME));
        if (password == null) {
            log.error("SVN resource definition attribute '" + PROXY_PASSWORD_ATTRIB_NAME
                    + "' may not be an empty string");
            throw new BeanCreationException("SVN resource definition attribute '" + PROXY_PASSWORD_ATTRIB_NAME
                    + "' may not be an empty string");
        }
        return password;
    }

}