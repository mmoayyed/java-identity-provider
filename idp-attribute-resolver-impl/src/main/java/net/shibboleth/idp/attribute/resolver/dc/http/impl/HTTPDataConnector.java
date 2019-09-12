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

/*
 * Derived from work (c) 2015 CSC, see included license.
 */

package net.shibboleth.idp.attribute.resolver.dc.http.impl;

import java.io.IOException;
import java.util.Map;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.apache.http.client.HttpClient;
import org.opensaml.security.httpclient.HttpClientSecurityParameters;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import net.shibboleth.idp.attribute.IdPAttribute;
import net.shibboleth.idp.attribute.resolver.ResolutionException;
import net.shibboleth.idp.attribute.resolver.dc.ValidationException;
import net.shibboleth.idp.attribute.resolver.dc.Validator;
import net.shibboleth.idp.attribute.resolver.dc.impl.AbstractSearchDataConnector;
import net.shibboleth.utilities.java.support.annotation.constraint.NonnullAfterInit;
import net.shibboleth.utilities.java.support.component.ComponentInitializationException;
import net.shibboleth.utilities.java.support.component.ComponentSupport;
import net.shibboleth.utilities.java.support.logic.Constraint;

/**
 * This class implements a {@link net.shibboleth.idp.attribute.resolver.DataConnector}
 * that obtains data from an HTTP service.
 */
public class HTTPDataConnector extends AbstractSearchDataConnector<HTTPSearch,HTTPResponseMappingStrategy> {
    
    /** Class logging. */
    @Nonnull private final Logger log = LoggerFactory.getLogger(HTTPDataConnector.class);

    /** The {@link HttpClient} to use. */
    @NonnullAfterInit private HttpClient httpClient;
    
    /** HTTP client security parameters. */
    @Nullable private HttpClientSecurityParameters httpClientSecurityParameters;
    
    /** Constructor. */
    public HTTPDataConnector() {
        setValidator(new Validator() {
            public void validate() throws ValidationException {
            }

            public void setThrowValidateError(final boolean what) {
            }

            public boolean isThrowValidateError() {
                return false;
            }
        });
    }

    /**
     * Set the {@link HttpClient} to use.
     * 
     * @param client client to use
     */
    public void setHttpClient(@Nonnull final HttpClient client) {
        ComponentSupport.ifInitializedThrowUnmodifiabledComponentException(this);
        ComponentSupport.ifDestroyedThrowDestroyedComponentException(this);

        httpClient = Constraint.isNotNull(client, "HttpClient cannot be null");
    }

    /**
     * Set the optional client security parameters.
     * 
     * @param params the new client security parameters
     */
    public void setHttpClientSecurityParameters(@Nullable final HttpClientSecurityParameters params) {
        ComponentSupport.ifInitializedThrowUnmodifiabledComponentException(this);
        ComponentSupport.ifDestroyedThrowDestroyedComponentException(this);

        httpClientSecurityParameters = params;
    }
    
    /** {@inheritDoc} */
    public void doInitialize() throws ComponentInitializationException {
        super.doInitialize();
        
        if (httpClient == null) {
            throw new ComponentInitializationException(getLogPrefix() + " HttpClient cannot be null");
        }
    }

    /** {@inheritDoc} */
    @Override
    @Nullable protected Map<String,IdPAttribute> retrieveAttributes(@Nonnull final HTTPSearch executable)
            throws ResolutionException {

        try {
            return getMappingStrategy().map(
                    executable.execute(httpClient, httpClientSecurityParameters, getMappingStrategy()));
        } catch (final IOException e) {
            throw new ResolutionException(getLogPrefix() + " HTTP request failed", e);
        }
    }
        
}