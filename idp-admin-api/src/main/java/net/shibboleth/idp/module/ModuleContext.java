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

package net.shibboleth.idp.module;

import java.io.PrintStream;
import java.nio.file.Path;
import java.util.Collections;
import java.util.List;
import java.util.Locale.LanguageRange;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.apache.http.client.HttpClient;
import org.opensaml.security.httpclient.HttpClientSecurityParameters;

import net.shibboleth.utilities.java.support.annotation.constraint.NonnullElements;
import net.shibboleth.utilities.java.support.annotation.constraint.NotEmpty;
import net.shibboleth.utilities.java.support.annotation.constraint.NotLive;
import net.shibboleth.utilities.java.support.annotation.constraint.Unmodifiable;
import net.shibboleth.utilities.java.support.logic.Constraint;

/**
 * Information required to perform some module operations.
 * 
 * @since 4.1.0
 */
public final class ModuleContext {

    /** IdP installation root. */
    @Nonnull private String idpHome;
    
    /** HttpClient if needed. */
    @Nullable private HttpClient httpClient;
    
    /** HTTP security parameters. */
    @Nullable private HttpClientSecurityParameters httpClientSecurityParams;
    
    /** Language expressions to use for i18n. */
    @Nonnull @NonnullElements private List<LanguageRange> languageRanges;
    
    /** Output stream for sending output to the module consumer. */
    @Nullable private PrintStream messageStream;

    /**
     * Constructor.
     *
     * @param home location of IdP install
     */
    public ModuleContext(@Nonnull @NotEmpty final String home) {
        idpHome = Constraint.isNotEmpty(home, "Home location cannot be null or empty");
        languageRanges = Collections.emptyList();
    }

    /**
     * Constructor.
     *
     * @param home location of IdP install
     */
    public ModuleContext(@Nonnull final Path home) {
        this(home.toString());
    }
    
    /**
     * Gets software installation location.
     * 
     * <p>Use the String variant to avoid Windows borkage.</p>
     * 
     * @return install path
     * 
     * @deprecated
     */
    @Deprecated(since="4.2", forRemoval=true)
    @Nonnull Path getIdPHome() {
        return Path.of(idpHome);
    }
    
    /**
     * Gets software installation location.
     * 
     * @return install path
     * 
     * @since 4.2.0
     */
    @Nonnull @NotEmpty String getInstallLocation() {
        return idpHome;
    }
    
    /**
     * Gets an {@link HttpClient} instance to use if available.
     * 
     * @return HTTP client instance
     */
    @Nullable HttpClient getHttpClient() {
        return httpClient;
    }
    
    /**
     * Sets an {@link HttpClient} instance to use.
     * 
     * @param client client to use
     */
    public void setHttpClient(@Nullable final HttpClient client) {
        httpClient = client;
    }

    /**
     * Gets {@link HttpClient} security parameters, if any.
     * 
     * @return HTTP client security parameters to use
     */
    @Nullable HttpClientSecurityParameters getHttpClientSecurityParameters() {
        return httpClientSecurityParams;
    }

    /**
     * Sets {@link HttpClient} security parameters to use.
     * 
     * @param params security parameters
     */
    public void setHttpClientSecurityParameters(@Nullable final HttpClientSecurityParameters params) {
        httpClientSecurityParams = params;
    }
    
    /**
     * Gets the language ranges to use for i18n.
     * 
     * @return language ranges
     */
    @Nonnull @NonnullElements @NotLive @Unmodifiable public List<LanguageRange> getLanguageRanges() {
        return languageRanges;
    }
    
    /**
     * Sets the language ranges to use for i18n.
     * 
     * @param ranges language ranges
     */
    public void setLanguageRanges(@Nullable @NonnullElements final List<LanguageRange> ranges) {
        if (ranges != null) {
            languageRanges = List.copyOf(ranges);
        } else {
            languageRanges = Collections.emptyList();
        }
    }

    /**
     * Gets the output stream to receive any instructions or additional information after
     * performing operations.
     * 
     * @return output stream, or null
     */
    @Nullable public PrintStream getMessageStream() {
        return messageStream;
    }
    
    /**
     * Sets the output stream to receive any instructions or additional information after
     * performing operations.
     * 
     * @param stream output stream
     */
    public void setMessageStream(@Nullable final PrintStream stream) {
        messageStream = stream;
    }
    
}