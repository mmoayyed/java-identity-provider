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

package net.shibboleth.idp.profile.spring.relyingparty.metadata;

import javax.annotation.Nullable;

import net.shibboleth.idp.Version;
import net.shibboleth.utilities.java.support.annotation.Duration;
import net.shibboleth.utilities.java.support.httpclient.HttpClientBuilder;

import org.apache.http.client.HttpClient;
import org.springframework.beans.factory.config.AbstractFactoryBean;

/**
 * Factory bean to accumulate the parameters into a {@link HttpClientBuilder} and to then emit a {@link HttpClient}.
 */
public class HttpClientFactoryBean extends AbstractFactoryBean<HttpClient> {

    /** Our captive builder. */
    private final HttpClientBuilder builder;
    
    /**
     * Connection Timeout.<br/>
     * We need this field to ensure that Spring does the conversion.
     */
    @Duration private long connectionTimeout;

    /**
     * Constructor.
     *
     */
    public HttpClientFactoryBean() {
        builder = createHttpClientBuilder();
        final StringBuilder stringBuilder = new StringBuilder("ShibbolethIdp/");
        stringBuilder .append(Version.getVersion()).append(" OpenSAML/").append(org.opensaml.core.Version.getVersion());
        builder.setUserAgent(stringBuilder.toString());
    }

    /** {@inheritDoc} */
    @Override public Class<HttpClient> getObjectType() {

        return HttpClient.class;
    }

    /**
     * Sets the maximum length of time in milliseconds to wait for the connection to be established. A value of less
     * than 1 indicates no timeout.
     * 
     * @param timeout maximum length of time in milliseconds to wait for the connection to be established
     */
    public void setConnectionTimeout(@Duration long timeout) {
        connectionTimeout = timeout;
        if (timeout > Integer.MAX_VALUE) {
            throw new IllegalArgumentException("Timeout was too large");
        }
        builder.setConnectionTimeout((int) timeout);
    }

    /**
     * Sets whether the responder's SSL certificate should be ignored.
     * 
     * @param disregard whether the responder's SSL certificate should be ignored
     */
    public void setConnectionDisregardSslCertificate(final boolean disregard) {
        builder.setConnectionDisregardSslCertificate(disregard);
    }

    /**
     * Sets the hostname of the default proxy used when making connection. A null indicates no default proxy.
     * 
     * @param host hostname of the default proxy used when making connection
     */
    public void setConnectionProxyHost(final String host) {
        builder.setConnectionProxyHost(host);
    }

    /**
     * Sets the port of the default proxy used when making connection.
     * 
     * @param port port of the default proxy used when making connection; must be greater than 0 and less than 65536
     */
    public void setConnectionProxyPort(final int port) {
        builder.setConnectionProxyPort(port);
    }

    /**
     * Sets the username to use when authenticating to the proxy.
     * 
     * @param usename username to use when authenticating to the proxy; may be null
     */
    public void setConnectionProxyUsername(final String usename) {
        builder.setConnectionProxyUsername(usename);
    }

    /**
     * Sets the password used when authenticating to the proxy.
     * 
     * @param password password used when authenticating to the proxy; may be null
     */
    public void setConnectionProxyPassword(final String password) {
        builder.setConnectionProxyPassword(password);
    }

    /**
     * Sets the user agent to be used when talking to the server. may not be null in which case the default will be
     * used.
     * 
     * @param agent what to set
     */
    public void setUserAgent(@Nullable final String agent) {
        builder.setUserAgent(agent);
    }
    
    /**
     * Create and return the instance of {@link HttpClientBuilder} to use.  
     * Subclasses may override to build a specialized subclass.
     * 
     * @return a new builder instance
     */
    protected HttpClientBuilder createHttpClientBuilder() {
        return new HttpClientBuilder();
    }
    
    /**
     * Get the instance of {@link HttpClientBuilder} to use.
     * 
     * @return the existing builder instance in use
     */
    protected HttpClientBuilder getHttpClientBuilder() {
        return builder;
    }

    /** {@inheritDoc} */
    @Override protected HttpClient createInstance() throws Exception {
        return builder.buildClient();
    }
    
}
