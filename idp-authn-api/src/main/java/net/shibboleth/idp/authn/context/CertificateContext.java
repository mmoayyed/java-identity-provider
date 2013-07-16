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

package net.shibboleth.idp.authn.context;

import java.security.cert.Certificate;

import javax.annotation.Nullable;

import org.opensaml.messaging.context.BaseContext;

/**
 * Context, usually attached to {@link AuthenticationContext}, that carries a {@link Certificate} to be
 * validated.
 */
public class CertificateContext extends BaseContext {

    /** The certificate to be validated. */
    @Nullable private Certificate certificate;

    /**
     * Gets the certificate to be validated.
     * 
     * @return the certificate to be validated
     */
    @Nullable public Certificate getCertificate() {
        return certificate;
    }

    /**
     * Sets the certificate to be validated.
     * 
     * @param cert certificate to be validated
     * 
     * @return this context
     */
    public CertificateContext setCertificate(@Nullable final Certificate cert) {
        certificate = cert;
        return this;
    }
    
}