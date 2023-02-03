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

package net.shibboleth.idp.authn.duo.impl;

import java.io.IOException;
import java.io.InputStream;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.concurrent.ThreadSafe;

import org.apache.hc.client5.http.classic.HttpClient;
import org.apache.hc.client5.http.protocol.HttpClientContext;
import org.apache.hc.core5.http.ClassicHttpRequest;
import org.apache.hc.core5.http.ClassicHttpResponse;
import org.apache.hc.core5.http.HttpStatus;
import org.opensaml.security.httpclient.HttpClientSecurityParameters;
import org.opensaml.security.httpclient.HttpClientSecuritySupport;

import com.duosecurity.duoweb.DuoWebException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import net.shibboleth.shared.annotation.constraint.NonnullAfterInit;
import net.shibboleth.shared.component.AbstractInitializableComponent;
import net.shibboleth.shared.component.ComponentInitializationException;
import net.shibboleth.shared.logic.Constraint;

/**
 * A base class for authentication actions which call a Duo AuthAPI endpont.
 */
@ThreadSafe
public abstract class AbstractDuoAuthenticator extends AbstractInitializableComponent {

    /** HttpClient for contacting Duo. */
    @NonnullAfterInit private HttpClient httpClient;

    /** HTTP client security parameters. */
    @Nullable private HttpClientSecurityParameters httpClientSecurityParameters;
    
    /** JSON object mapper. */
    @NonnullAfterInit private ObjectMapper objectMapper;
    
    /**
     * Set the {@link HttpClient} to use for contacting Duo.
     * 
     * @param client HttpClient
     */
    public void setHttpClient(@Nonnull final HttpClient client) {
        checkSetterPreconditions();
        httpClient = Constraint.isNotNull(client, "HTTP client cannot be null");
    }

    /**
     * Set the optional client security parameters.
     * 
     * @param params the new client security parameters
     */
    public void setHttpClientSecurityParameters(@Nullable final HttpClientSecurityParameters params) {
        checkSetterPreconditions();
        httpClientSecurityParameters = params;
    }
    
    /**
     * Set the JSON {@link ObjectMapper}.
     * 
     * @param mapper object mapper
     */
    public void setObjectMapper(@Nonnull final ObjectMapper mapper) {
        checkSetterPreconditions();
        objectMapper = Constraint.isNotNull(mapper, "Object mapper cannot be null");
    }

    /** {@inheritDoc} */
    @Override protected void doInitialize() throws ComponentInitializationException {
        super.doInitialize();
        
        
        if (httpClient == null) {
            throw new ComponentInitializationException("HttpClient cannot be null");
        }

        if (objectMapper == null) {
            throw new ComponentInitializationException("ObjectMapper cannot be null");
        }
    }

    /**
     * Performs a call to the Duo AuthAPI. Upon a successful call, the JSON response is mapped into the appropriate type
     * of {@link DuoResponseWrapper}.
     * 
     * @param request the prepared HTTP request
     * @param wrapperTypeRef the type of {@link DuoResponseWrapper} to use
     * @param <T> the DuoResponse type being wrapped
     * 
     * @return a {@link DuoResponseWrapper}
     * 
     * @throws IOException on an I/O error
     * @throws ClientProtocolException on an HTTP error 
     * @throws DuoWebException on a Duo-related error
     */
    protected <T extends DuoResponseWrapper<?>> T doAPIRequest(@Nonnull final ClassicHttpRequest request,
            @Nonnull final TypeReference<T> wrapperTypeRef)
                    throws DuoWebException, IOException {

        // Make the request.
        final HttpClientContext clientContext = HttpClientContext.create();
        HttpClientSecuritySupport.marshalSecurityParameters(clientContext, httpClientSecurityParameters, true);
        HttpClientSecuritySupport.addDefaultTLSTrustEngineCriteria(clientContext, request);
        final ClassicHttpResponse httpResponse = httpClient.executeOpen(null, request, clientContext);
        HttpClientSecuritySupport.checkTLSCredentialEvaluated(clientContext, request.getScheme());

        // Check the HTTP response code.
        final int httpStatusCode = httpResponse.getCode();
        if (httpStatusCode == HttpStatus.SC_BAD_REQUEST) {
            final InputStream httpContent = httpResponse.getEntity().getContent();
            final DuoFailureResponse msg = objectMapper.readValue(httpContent, DuoFailureResponse.class);
            final StringBuilder builder = new StringBuilder();
            builder.append(msg.getMessage() != null ? msg.getMessage() : "no message")
                .append(" (")
                .append(msg.getMessageDetail() != null ? msg.getMessageDetail() : "no detail")
                .append(")");
            throw new DuoWebException(builder.toString());
        }
        if (httpStatusCode != HttpStatus.SC_OK) {
            throw new IOException("Non-ok status code (" + httpStatusCode + ") returned from Duo: "
                    + httpResponse.getReasonPhrase());
        } else if (httpResponse.getEntity() == null) {
            throw new IOException("No response body returned from Duo");
        }

        // Parse the JSON response.
        final T duoResponse = objectMapper.readValue(httpResponse.getEntity().getContent(), wrapperTypeRef);

        if (duoResponse == null) {
            throw new DuoWebException("Unable to parse JSON response");
        } else if (!"OK".equals(duoResponse.getStat())) {
            throw new DuoWebException("Unexpected status value in JSON response: " + duoResponse.getStat());
        }

        return duoResponse;
    }

}