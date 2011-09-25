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

package net.shibboleth.idp.authn;

import java.security.cert.X509Certificate;

import org.opensaml.messaging.context.AbstractSubcontext;
import org.opensaml.messaging.context.SubcontextContainer;
import org.opensaml.util.Assert;

/**
 * Context, usually attached to {@link AuthenticationRequestContext}, that carries an {@link X509Certificate} to be
 * validated.
 */
public class X509CertificateSubcontext extends AbstractSubcontext {

    /** The certificate to be validated. */
    private final X509Certificate certificate;

    /**
     * Constructor.
     * 
     * @param owner the context which owns this one, may be null
     * @param cert the certificate to be validated, not null
     */
    public X509CertificateSubcontext(final SubcontextContainer owner, final X509Certificate cert) {
        super(owner);
        certificate = Assert.isNotNull(cert, "X.509 certificate can not be null");
    }

    /**
     * Gets the certificate to be validated.
     * 
     * @return the certificate to be validated, never null
     */
    public X509Certificate getCertificate() {
        return certificate;
    }
}