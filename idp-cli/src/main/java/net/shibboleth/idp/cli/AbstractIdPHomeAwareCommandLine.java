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

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.apache.hc.client5.http.classic.HttpClient;
import org.opensaml.security.httpclient.HttpClientSecurityParameters;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;

import net.shibboleth.idp.spring.IdPPropertiesApplicationContextInitializer;
import net.shibboleth.shared.cli.AbstractCommandLine;

/**
 * An extension to {@link AbstractCommandLine} that auto-adds our context initializer for idp.home
 * and property support.
 *
 * @param <T> argument object type
 * 
 * @since 4.1.0
 */
public abstract class AbstractIdPHomeAwareCommandLine<T extends AbstractIdPHomeAwareCommandLineArguments>
        extends AbstractCommandLine<T> {
    
    /** The injected HttpClient. */
    @Nullable private HttpClient httpClient;
    
    /** Injected security parameters. */
    @Nullable private HttpClientSecurityParameters httpClientSecurityParameters;
    
    /**
     * Gets the {@link HttpClient} to use.
     * 
     * @return the HTTP client to use
     */
    @Nullable public HttpClient getHttpClient() {
        return httpClient;
    }

    /**
     * Gets the {@link HttpClientSecurityParameters} to use.
     * 
     * @return the HTTP client security parameters to use
     */
    @Nullable public HttpClientSecurityParameters getHttpClientSecurityParameters() {
        return httpClientSecurityParameters;
    }

    /** {@inheritDoc} */
    @Override
    protected int doRun(@Nonnull final T args) {
        if (args.getIdPHome() != null) {
            System.setProperty("idp.home", args.getIdPHome());
        }
        setContextInitializer(new IdPPropertiesApplicationContextInitializer());

        final int rc = super.doRun(args);
        if (rc != RC_OK) {
            return rc;
        }
        
        if (args.getHttpClientName() != null) {
            try {
                httpClient = getApplicationContext().getBean(args.getHttpClientName(), HttpClient.class);
            } catch (final NoSuchBeanDefinitionException e) {
                getLogger().error("Could not locate HttpClient '{}'", args.getHttpClientName());
                return RC_IO;
            }
        }
        
        if (args.getHttpClientSecurityParameterstName() != null) {
            try {
                httpClientSecurityParameters =
                        getApplicationContext().getBean(args.getHttpClientSecurityParameterstName(),
                                HttpClientSecurityParameters.class);
            } catch (final NoSuchBeanDefinitionException e) {
                getLogger().error("Could not locate HttpClientSecurityParameters '{}'",
                        args.getHttpClientSecurityParameterstName());
                return RC_IO;
            }
        }
        
        return RC_OK;
    }
    
}