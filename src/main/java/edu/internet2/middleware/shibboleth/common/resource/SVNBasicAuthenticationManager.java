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

package edu.internet2.middleware.shibboleth.common.resource;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.net.ssl.TrustManager;

import net.jcip.annotations.ThreadSafe;

import org.opensaml.xml.util.DatatypeHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tmatesoft.svn.core.SVNErrorMessage;
import org.tmatesoft.svn.core.SVNException;
import org.tmatesoft.svn.core.SVNURL;
import org.tmatesoft.svn.core.auth.ISVNAuthenticationManager;
import org.tmatesoft.svn.core.auth.ISVNAuthenticationProvider;
import org.tmatesoft.svn.core.auth.ISVNProxyManager;
import org.tmatesoft.svn.core.auth.SVNAuthentication;
import org.tmatesoft.svn.core.io.SVNRepository;

/** Authentication manager for SVN resources. */
@ThreadSafe
public class SVNBasicAuthenticationManager implements ISVNAuthenticationManager {

    /** Class logger. */
    private final Logger log = LoggerFactory.getLogger(SVNBasicAuthenticationManager.class);

    /** Network connection timeout in milliseconds. */
    private int connectionTimeout;

    /** Read operation timeout in milliseconds. */
    private int readTimeout;

    private TrustManager trustManager;

    /** User authentication mechanisms. */
    private Map<String, SVNAuthentication> authenticationMethods;

    /** HTTP proxy configuration. */
    private final BasicProxyManager proxyManager;

    /**
     * Constructor.
     * 
     * @param authnMethods user authentication methods
     */
    public SVNBasicAuthenticationManager(List<SVNAuthentication> authnMethods) {
        connectionTimeout = 5000;
        readTimeout = 10000;
        setAuthenticationMethods(authnMethods);
        proxyManager = null;
    }

    /**
     * Constructor.
     * 
     * @param authnMethods user authentication methods
     * @param proxyHost host name or IP address of the proxy server
     * @param proxyPort port of the proxy server
     * @param proxyUser username used to connect to the proxy server
     * @param proxyPassword password used to connect to the proxy server
     */
    public SVNBasicAuthenticationManager(List<SVNAuthentication> authnMethods, String proxyHost, int proxyPort,
            String proxyUser, String proxyPassword) {
        connectionTimeout = 5000;
        readTimeout = 10000;
        setAuthenticationMethods(authnMethods);
        proxyManager = new BasicProxyManager(proxyHost, proxyPort, proxyUser, proxyPassword);
    }

    /** {@inheritDoc} */
    public void acknowledgeAuthentication(boolean authnAccepted, String authnKind, String authnRealm,
            SVNErrorMessage error, SVNAuthentication authnMethods) throws SVNException {
        if (authnAccepted) {
            log.trace("Successful authentication to SVN repository with {} credentials", authnKind);
        } else {
            log.trace("Unable to authenticate to SVN repository with {} credentials", authnKind);
        }
    }

    /** {@inheritDoc} */
    public void acknowledgeTrustManager(TrustManager manager) {
        log.debug("HTTPS connectiont trusted by trust manager");
    }

    /** {@inheritDoc} */
    public int getConnectTimeout(SVNRepository repository) {
        return connectionTimeout;
    }

    /**
     * Sets the network connection timeout in milliseconds. If a value of zero or less is given than the value
     * {@value Integer#MAX_VALUE} will be used.
     * 
     * @param timeout network connection timeout in milliseconds
     */
    public void setConnectionTimeout(int timeout) {
        if (timeout <= 0) {
            connectionTimeout = Integer.MAX_VALUE;
        } else {
            connectionTimeout = timeout;
        }
    }

    /** {@inheritDoc} */
    public SVNAuthentication getFirstAuthentication(String authnKind, String authnRealm, SVNURL repository)
            throws SVNException {
        return authenticationMethods.get(authnKind);
    }

    /** {@inheritDoc} */
    public SVNAuthentication getNextAuthentication(String authnKind, String authnRealm, SVNURL respository)
            throws SVNException {
        return null;
    }

    /** {@inheritDoc} */
    public ISVNProxyManager getProxyManager(SVNURL repository) throws SVNException {
        return proxyManager;
    }

    /** {@inheritDoc} */
    public int getReadTimeout(SVNRepository repository) {
        return readTimeout;
    }

    /**
     * Sets the read operation timeout in milliseconds. If a value of zero or less is given than the value
     * {@value Integer#MAX_VALUE} will be used.
     * 
     * @param timeout network connection timeout in milliseconds
     */
    public void setReadTimeout(int timeout) {
        if (timeout <= 0) {
            readTimeout = Integer.MAX_VALUE;
        } else {
            readTimeout = timeout;
        }
    }

    /** {@inheritDoc} */
    public TrustManager getTrustManager(SVNURL respository) throws SVNException {
        return trustManager;
    }

    /**
     * Sets the trust manager used when negotiating SSL/TLS connections.
     * 
     * @param manager trust manager used when negotiating SSL/TLS connections
     */
    public void setTrustManager(TrustManager manager) {
        trustManager = manager;
    }

    /** {@inheritDoc} */
    public boolean isAuthenticationForced() {
        return false;
    }

    /** {@inheritDoc} */
    public void setAuthenticationProvider(ISVNAuthenticationProvider arg0) {
    }

    /**
     * Sets the user authentication methods.
     * 
     * @param authnMethods user authentication methods
     */
    private void setAuthenticationMethods(List<SVNAuthentication> authnMethods) {
        if (authnMethods == null || authnMethods.size() == 0) {
            authenticationMethods = Collections.emptyMap();
        } else {
            HashMap<String, SVNAuthentication> methods = new HashMap<String, SVNAuthentication>();
            for (SVNAuthentication method : authnMethods) {
                if (methods.containsKey(method.getKind())) {
                    log.warn("An authentication method of type " + method.getKind()
                            + " has already been set, only the first will be used");
                } else {
                    methods.put(method.getKind(), method);
                }
            }
            authenticationMethods = Collections.unmodifiableMap(methods);
        }
    }

    /** Basic implementation of {@link ISVNProxyManager}. */
    private class BasicProxyManager implements ISVNProxyManager {

        /** Host name or IP address of the proxy. */
        private final String host;

        /** Port of the proxy. */
        private final int port;

        /** Username used to connect to the proxy. */
        private final String user;

        /** Password used to connect to the proxy. */
        private final String password;

        /**
         * Constructor.
         * 
         * @param host host name or IP address of the proxy server
         * @param port port of the proxy server
         * @param user username used to connect to the proxy server
         * @param password password used to connect to the proxy server
         */
        public BasicProxyManager(String host, int port, String user, String password) {
            this.host = DatatypeHelper.safeTrimOrNullString(host);
            if (this.host == null) {
                throw new IllegalArgumentException("Proxy host may not be null or empty");
            }

            this.port = port;

            this.user = DatatypeHelper.safeTrimOrNullString(user);
            this.password = DatatypeHelper.safeTrimOrNullString(password);
        }

        /** {@inheritDoc} */
        public void acknowledgeProxyContext(boolean accepted, SVNErrorMessage error) {
            if (accepted) {
                log.trace("Connected to HTTP proxy " + host + ":" + port);
            }
            log.error("Unable to connect to HTTP proxy " + host + ":" + port + " recieved error:\n"
                    + error.getFullMessage());
        }

        /** {@inheritDoc} */
        public String getProxyHost() {
            return host;
        }

        /** {@inheritDoc} */
        public String getProxyPassword() {
            return password;
        }

        /** {@inheritDoc} */
        public int getProxyPort() {
            return port;
        }

        /** {@inheritDoc} */
        public String getProxyUserName() {
            return user;
        }
    }
}
