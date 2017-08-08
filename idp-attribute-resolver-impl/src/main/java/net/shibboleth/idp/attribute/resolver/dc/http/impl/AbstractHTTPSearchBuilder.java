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

package net.shibboleth.idp.attribute.resolver.dc.http.impl;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.apache.http.HttpRequest;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.client.protocol.HttpClientContext;
import org.opensaml.security.httpclient.HttpClientSecurityParameters;
import org.opensaml.security.httpclient.HttpClientSecuritySupport;

import net.shibboleth.idp.attribute.IdPAttribute;
import net.shibboleth.idp.attribute.IdPAttributeValue;
import net.shibboleth.idp.attribute.resolver.ResolutionException;
import net.shibboleth.idp.attribute.resolver.context.AttributeResolutionContext;
import net.shibboleth.idp.attribute.resolver.dc.impl.ExecutableSearchBuilder;
import net.shibboleth.utilities.java.support.annotation.constraint.NotEmpty;
import net.shibboleth.utilities.java.support.component.AbstractInitializableComponent;
import net.shibboleth.utilities.java.support.component.ComponentSupport;
import net.shibboleth.utilities.java.support.logic.Constraint;

/**
 * Basis of request builder. Derived classes just have to provide the per request URI but may override
 * the complete request build if desired, for example to construct a SOAP message or something more
 * exotic.
 * 
 * <p>This is all a bit byzantine to maintain a consistent design with the LDAP/etc. connectors,
 * which split the work of producing "search objects that execute and return a result" and
 * "mapping strategies that process a result". The HTTP client supports response handlers
 * that offload all the connection cleanup and avoid any extra data buffering, so our
 * facade passes the mapping strategy in as a response handler and just returns the result.</p>
 */
public abstract class AbstractHTTPSearchBuilder extends AbstractInitializableComponent implements
        ExecutableSearchBuilder<HTTPSearch> {
    
    /** HTTP client security parameters. */
    @Nullable private HttpClientSecurityParameters httpClientSecurityParameters;
    
    
    /**
     * Get the optional client security parameters.
     * 
     * <p>This is informational to accommodate a scenario in which the parameters should influence
     * the construction of the request, but the actual parameters to use will be supplied to the
     * {@link HTTPSearch#execute(HttpClient, HttpClientSecurityParameters, HTTPResponseMappingStrategy)} method.</p>
     * 
     * @return client security parameters
     */
    @Nullable public HttpClientSecurityParameters getHttpClientSecurityParameters() {
        return httpClientSecurityParameters;
    }
    
    /**
     * Set the optional client security parameters.
     * 
     * <p>This is informational to accommodate a scenario in which the parameters should influence
     * the construction of the request, but the actual parameters to use will be supplied to the
     * {@link HTTPSearch#execute(HttpClient, HttpClientSecurityParameters, HTTPResponseMappingStrategy)} method.</p>
     * 
     * @param params client security parameters
     */
    public void setHttpClientSecurityParameters(@Nullable final HttpClientSecurityParameters params) {
        ComponentSupport.ifInitializedThrowUnmodifiabledComponentException(this);
        ComponentSupport.ifDestroyedThrowDestroyedComponentException(this);

        httpClientSecurityParameters = params;
    }
    
    /** {@inheritDoc} */
    @Override public HTTPSearch build(@Nonnull final AttributeResolutionContext resolutionContext,
            @Nonnull final Map<String, List<IdPAttributeValue<?>>> dependencyAttributes) throws ResolutionException {
        
        final HttpUriRequest request = getHttpRequest(resolutionContext, dependencyAttributes);

// Checkstyle: AnonInnerLength OFF
        return new HTTPSearch() {
            
            /** {@inheritDoc} */
            @Nonnull public String getResultCacheKey() {
                Constraint.isTrue(request instanceof HttpGet, "Only GET requests are cacheable");
                return ((HttpGet) request).getURI().toString();
            }

            /** {@inheritDoc} */
            public String toString() {
                return request.getRequestLine().getUri();
            }
            
            /** {@inheritDoc} */
            @Nonnull public Map<String,IdPAttribute> execute(@Nonnull final HttpClient client,
                    @Nullable final HttpClientSecurityParameters securityParameters,
                    @Nonnull final HTTPResponseMappingStrategy mappingStrategy) throws IOException {
                
                final HttpClientContext clientContext = HttpClientContext.create();
                HttpClientSecuritySupport.marshalSecurityParameters(clientContext, httpClientSecurityParameters, true);
                HttpClientSecuritySupport.addDefaultTLSTrustEngineCriteria(clientContext, request);
                final Map<String,IdPAttribute> results = client.execute(request, mappingStrategy, clientContext);
                HttpClientSecuritySupport.checkTLSCredentialEvaluated(clientContext, request.getURI().getScheme());
                return results;
            }
            
        };
// Checkstyle: AnonInnerLength ON
    }

    /**
     * Method to return the URL to access via GET.
     * 
     * @param resolutionContext the context of the resolution
     * @param dependencyAttributes made available to the request
     * 
     * @return the URL to GET
     * @throws ResolutionException if an error occurs
     */
    @Nonnull @NotEmpty protected abstract String getURL(@Nonnull final AttributeResolutionContext resolutionContext,
            @Nonnull final Map<String,List<IdPAttributeValue<?>>> dependencyAttributes) throws ResolutionException;
    
    /**
     * Default implementation just supports GET and builds a request around a URL.
     * 
     * @param resolutionContext the context of the resolution
     * @param dependencyAttributes made available to the request
     * 
     * @return the {@link HttpRequest} to use
     * @throws ResolutionException if an error occurs
     */
    @Nonnull protected HttpUriRequest getHttpRequest(@Nonnull final AttributeResolutionContext resolutionContext,
            @Nonnull final Map<String,List<IdPAttributeValue<?>>> dependencyAttributes) throws ResolutionException {
        
        // Default just wraps a computed URL into a GET.
        try {
            return new HttpGet(getURL(resolutionContext, dependencyAttributes));
        } catch (final IllegalArgumentException e) {
            throw new ResolutionException(e);
        }
    }
        
}