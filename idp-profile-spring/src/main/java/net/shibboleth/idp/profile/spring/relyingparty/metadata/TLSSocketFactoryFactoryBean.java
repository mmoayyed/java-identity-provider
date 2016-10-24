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

import org.apache.http.client.HttpClient;
import org.apache.http.conn.socket.LayeredConnectionSocketFactory;
import org.apache.http.conn.ssl.StrictHostnameVerifier;
import org.opensaml.security.httpclient.HttpClientSecurityParameters;
import org.opensaml.security.httpclient.impl.SecurityEnhancedTLSSocketFactory;
import org.opensaml.security.trust.TrustEngine;
import org.opensaml.security.x509.X509Credential;
import org.springframework.beans.factory.config.AbstractFactoryBean;

import net.shibboleth.utilities.java.support.httpclient.HttpClientSupport;

/**
 * A factory bean for producing instances of {@link LayeredConnectionSocketFactory} for use in {@link HttpClient}.
 */
public class TLSSocketFactoryFactoryBean extends AbstractFactoryBean {
    
    /** The optional trust engine used in evaluating server TLS credentials. */
    private TrustEngine<?> tlsTrustEngine;
    
    /** The optional HttpClient security parameters instance that will be used with the HttpClient instance. */
    private HttpClientSecurityParameters httpClientSecurityParameters;
    
    /** Whether the responder's SSL/TLS certificate should be ignored. */
    private boolean connectionDisregardTLSCertificate;
    
    /**
     * Sets the optional trust engine used in evaluating server TLS credentials.
     * 
     * @param engine the trust engine instance to use, or null
     */
    public void setTLSTrustEngine(@Nullable final TrustEngine<? super X509Credential> engine) {
        tlsTrustEngine = engine;
    }
    
    /**
     * Sets the optional HttpClient security parameters instance that will be used with the HttpClient instance.
     * 
     * @param params the parameters, or null
     */
    public void setHttpClientSecurityParameters(@Nullable final HttpClientSecurityParameters params) {
        httpClientSecurityParameters = params;
    }

    /**
     * Sets whether the responder's SSL/TLS certificate should be ignored.
     * 
     * @param disregard whether the responder's SSL/TLS certificate should be ignored
     */
    public void setConnectionDisregardTLSCertificate(final boolean disregard) {
        connectionDisregardTLSCertificate = disregard;
    }

    /** {@inheritDoc} */
    public Class getObjectType() {
        return LayeredConnectionSocketFactory.class;
    }

    /** {@inheritDoc} */
    protected Object createInstance() throws Exception {
        if (tlsTrustEngine != null 
                || (httpClientSecurityParameters != null && httpClientSecurityParameters.getTLSTrustEngine() != null)) {
            return  new SecurityEnhancedTLSSocketFactory(HttpClientSupport.buildNoTrustTLSSocketFactory(), 
                    new StrictHostnameVerifier());
        } else if (connectionDisregardTLSCertificate) {
            return HttpClientSupport.buildNoTrustTLSSocketFactory();
        } else {
            return HttpClientSupport.buildStrictTLSSocketFactory();
        }
    }

}
