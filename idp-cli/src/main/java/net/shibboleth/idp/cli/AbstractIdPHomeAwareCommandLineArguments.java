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

import org.opensaml.security.httpclient.HttpClientSecurityParameters;

import com.beust.jcommander.Parameter;

import net.shibboleth.shared.annotation.constraint.NotEmpty;
import net.shibboleth.shared.primitive.StringSupport;

/**
 * An extension to {@link net.shibboleth.shared.cli.AbstractCommandLineArguments}
 * that allows idp.home override and includes HTTP client support.
 * 
 * @since 4.1.0
 */
public abstract class AbstractIdPHomeAwareCommandLineArguments
        extends net.shibboleth.shared.cli.AbstractCommandLineArguments {

    /** IdP location. */
    @Parameter(names = "--home")
    @Nullable private String idpHome;
    
    /** Name for the {@link org.apache.hc.client5.http.classic.HttpClient} . */
    @Parameter(names= {"-hc", "--http-client"})
    @Nullable @NotEmpty private String httpClientName;

    /** Name for the {@link HttpClientSecurityParameters} . */
    @Parameter(names= {"-hs", "--http-security"})
    @Nullable @NotEmpty private String httpClientSecurityParametersName;
    
    /** 
     * Gets the configured home location.
     * 
     * @return home location
     */
    @Nullable public String getIdPHome() {
        return idpHome;
    }

    /**
     * Get bean name for the {@link org.apache.hc.client5.http.classic.HttpClient} (if specified).
     * 
     * @return the name or null
     */
    @Nullable @NotEmpty public String getHttpClientName() {
        return httpClientName;
    }
    
    /**
     * Set bean name for the {@link org.apache.hc.client5.http.classic.HttpClient}.
     * 
     * @param name bean name
     */
    public void setHttpClientName(@Nullable @NotEmpty final String name) {
        httpClientName = StringSupport.trimOrNull(name);
    }

    /**
     * Get bean name for the {@link HttpClientSecurityParameters} (if specified).
     * 
     * @return the name or null
     */
    @Nullable @NotEmpty public String getHttpClientSecurityParameterstName() {
        return httpClientSecurityParametersName;
    }
    
    /** {@inheritDoc} */
    @Override
    public void printHelp(final PrintStream out) {
        super.printHelp(out);
        out.println(String.format("  --%-20s %s", "home",
                "Sets idp.home if not installed to default location."));
        out.println(String.format("  %-22s %s", "-hc, --http-client",
                "Use the named bean for HTTP operations"));
        out.println(String.format("  %-22s %s", "-hs, --http-security",
                "Use the named bean for HTTP security"));
        out.println();
    }
}