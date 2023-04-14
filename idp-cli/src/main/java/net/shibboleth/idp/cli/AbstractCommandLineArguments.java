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
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLSession;

import net.shibboleth.shared.annotation.constraint.NonnullElements;
import net.shibboleth.shared.annotation.constraint.NotEmpty;
import net.shibboleth.shared.annotation.constraint.NotLive;
import net.shibboleth.shared.annotation.constraint.Unmodifiable;
import net.shibboleth.shared.codec.Base64Support;
import net.shibboleth.shared.codec.EncodingException;
import net.shibboleth.shared.primitive.StringSupport;

import com.beust.jcommander.Parameter;
import com.beust.jcommander.Parameters;

/**
 * Base class for JCommander command line argument handling for an HTTP-based remote service call,
 * with an abstract method that adds to a URL based on a derived class' arguments. 
 */
@Parameters(separators = " =")
public abstract class AbstractCommandLineArguments implements CommandLineArguments {

    /** Name of system property for overriding default URL. */
    @Nonnull @NotEmpty public static final String BASEURL_PROPERTY = "net.shibboleth.idp.cli.baseURL";
    
    /** Display command usage. */
    @Parameter(names = {"-h", "--help"}, description = "Display program usage", help = true)
    private boolean help;

    /** Base of URL to invoke. */
    @Parameter(names = {"-u", "--url"}, description = "Base URL to invoke (path and query string will be appended)")
    @Nonnull private String url;

    /** Path to add to base URL. */
    @Parameter(names = {"-p", "--path"}, description = "Path to append to base URL to invoke")
    @Nullable private String path;

    /** Disable TLS certificate name checking. */
    @Parameter(names = {"-k", "--disableNameChecking"}, description = "Disable TLS certificate name checking")
    private boolean disableNameChecking;

    /** Trust store for SSL connectivity. */
    @Parameter(names = {"-ts", "--trustStore"}, description = "Path to a trust store for SSL connections")
    @Nullable private String trustStore;

    /** Trust store type for SSL connectivity. */
    @Parameter(names = {"-tt", "--trustStoreType"}, description = "Type of trust store for SSL connections")
    @Nullable private String trustStoreType;

    /** Trust store password for SSL connectivity. */
    @Parameter(names = {"-tp", "--trustStorePassword"}, description = "Password to a trust store for SSL connections")
    @Nullable private String trustStorePassword;

    /** Username to be used in the HTTP-Basic authentication. */
    @Parameter(names = {"--username"}, required = false, description = "Username to be used in HTTP-Basic Auth")
    @Nullable private String username;

    /** Password to be used in the HTTP-Basic authentication. */
    @Parameter(names = {"--password"}, required = false, password = true,
            description = "Password to be used in HTTP-Basic Auth")
    @Nullable private String password;

    /** HTTP method to use, GET by default. */
    @Parameter(names = {"--method"}, required = false, description = "")
    @Nullable @NotEmpty private String method;
    
    /** HTTP header name/value pair(s). */
    @Parameter(names = {"--header"}, required = false, arity = 2, description = "HTTP header name and value")
    @Nullable private List<String> headers;
    
    /** Constructor. */
    public AbstractCommandLineArguments() {
        String u = System.getProperty(BASEURL_PROPERTY);  
        if (u == null) {
            u = "http://localhost/idp";
        }
        url = u;
    }
    
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
    
    /**
     * Value of "disableNameChecking" parameter.
     * 
     * @return parameter value
     */
    public boolean isDisableNameChecking() {
        return disableNameChecking;
    }
    
    /**
     * Value of "username" parameter.
     * 
     * @return username parameter
     * 
     * @since 4.2.0
     */
    @Nullable public String getUsername() {
        return username;
    }

    /**
     * Value of "password" parameter.
     * 
     * @return password parameter
     * 
     * @since 4.2.0
     */
    @Nullable public String getPassword() {
        return password;
    }
    
    /**
     * Value of "method" parameter.
     * 
     * @return method
     * 
     * @since 4.2.0
     */
    @Nullable @NotEmpty public String getMethod() {
        return StringSupport.trimOrNull(method);
    }

    /** {@inheritDoc} */
    @Nullable @NonnullElements @NotLive @Unmodifiable public Map<String,String> getHeaders() {
        final List<String> hdrs = headers;
        if (hdrs != null) {
            final Map<String,String> map = new HashMap<>();
            final Iterator<String> iter = hdrs.iterator();
            while (iter.hasNext()) {
                final String h = iter.next();
                if (iter.hasNext()) {
                    final String v = iter.next();
                    if (h != null && v != null) {
                        if (map.containsKey(h)) {
                            map.put(h, map.get(h).concat(",").concat(v));
                        } else {
                            map.put(h, v);
                        }
                    }
                }
            }
            return map;
        }
        return null;
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
        
        if (disableNameChecking) {
            final HostnameVerifier allHostsValid = new HostnameVerifier() {
                public boolean verify(final String hostname, final SSLSession session) {
                    return true;
                }
            };      
            HttpsURLConnection.setDefaultHostnameVerifier(allHostsValid);                       
        }
        
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
    
    /** {@inheritDoc} */
    @Nullable @NotEmpty public String getBasicAuthHeader() {
        if (StringSupport.trimOrNull(username) == null || StringSupport.trimOrNull(password) == null) {
            return null;
        }
        final String rawHeader = username + ":" + password;
        try {
            final byte[] bytes = rawHeader.getBytes(StandardCharsets.UTF_8);
            assert bytes != null;
            return "Basic " + Base64Support.encode(bytes, false);
        } catch (final EncodingException e) {
            System.err.println(e.getMessage());
            return null;
        }
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