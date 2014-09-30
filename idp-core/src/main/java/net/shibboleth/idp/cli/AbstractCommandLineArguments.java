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

import java.net.MalformedURLException;
import java.net.URL;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import com.beust.jcommander.Parameter;
import com.beust.jcommander.Parameters;

/**
 * Base class for JCommander command line argument handling for an HTTP-based remote service call,
 * with an abstract method that adds to a URL based on a derived class' arguments. 
 */
@Parameters(separators = " =")
public abstract class AbstractCommandLineArguments implements CommandLineArguments {

    /** Display command usage. */
    @Parameter(names = {"-h", "--help"}, description = "Display program usage", help = true)
    private boolean help;

    /** URL to invoke. */
    @Parameter(names = {"-u", "--url"}, description = "Base URL to invoke (no path, no query string)")
    @Nonnull private String url = "http://localhost";

    /** Path to invoke. */
    @Parameter(names = {"-p", "--path"}, description = "Path to append to URL to invoke (may include query string)")
    @Nullable private String path;
    
    /** Trust store for SSL connectivity. */
    @Parameter(names = {"-ts", "--trustStore"}, description = "Path to a trust store for SSL connections")
    @Nullable private String trustStore;

    /** Trust store type for SSL connectivity. */
    @Parameter(names = {"-tt", "--trustStoreType"}, description = "Type of trust store for SSL connections")
    @Nullable private String trustStoreType;

    /** Trust store password for SSL connectivity. */
    @Parameter(names = {"-tp", "--trustStorePassword"}, description = "Password to a trust store for SSL connections")
    @Nullable private String trustStorePassword;

    /**
     * Value of "help" parameter.
     * 
     * @return parameter value
     */
    public boolean getHelp() {
        return help;
    }
    
    /**
     * Value of "url" parameter.
     * 
     * <p>Defaults to http://localhost</p>
     * 
     * @return parameter value
     */
    @Nonnull public String getURL() {
        return url;
    }

    /**
     * Value of "path" parameter.
     * 
     * @return parameter value
     */
    @Nullable public String getPath() {
        return path;
    }

    /**
     * Value of "trustStore" parameter.
     * 
     * @return parameter value
     */
    @Nullable public String getTrustStore() {
        return trustStore;
    }

    /**
     * Value of "trustStoreType" parameter.
     * 
     * @return parameter value
     */
    @Nullable public String getTrustStoreType() {
        return trustStoreType;
    }

    /**
     * Value of "trustStorePassword" parameter.
     * 
     * @return parameter value
     */
    @Nullable public String getTrustStorePassword() {
        return trustStorePassword;
    }

    /** {@inheritDoc} */
    @Override
    public boolean isUsage() {
        return help;
    }
        
    /** {@inheritDoc} */
    @Override
    public void validate() {
        
    }
    
    /**
     * Compute the full URL to connect to.
     * 
     * @return the URL to connect to
     * 
     * @throws MalformedURLException if the URL constructed is invalid 
     */
    @Nonnull public URL buildURL() throws MalformedURLException {
        installTrustStore();
        
        final StringBuilder builder = new StringBuilder(getURL());
        if (getPath() != null) {
            builder.append(getPath());
        }

        return new URL(doBuildURL(builder).toString());
    }
    
    /**
     * Override this method to modify the eventual URL and attach any parameters.
     * 
     * @param builder   contains the URL in a partial state of construction, possibly including query string
     * 
     * @return a builder containing the modified URL string
     */
    @Nonnull protected StringBuilder doBuildURL(@Nonnull final StringBuilder builder) {
        return builder;
    }
    
    /** Use the configured parameters to set global JVM trust store parameters for SSL connectivity. */
    private void installTrustStore() {
        if (trustStoreType != null) {
            System.setProperty("javax.net.ssl.trustStore", trustStore);
            if (trustStoreType != null) {
                System.setProperty("javax.net.ssl.trustStoreType", trustStoreType);
            }
            if (trustStorePassword != null) {
                System.setProperty("javax.net.ssl.trustStorePassword", trustStorePassword);
            }
        }
    }
    
}