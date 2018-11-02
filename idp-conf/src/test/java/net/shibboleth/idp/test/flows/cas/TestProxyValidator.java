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

package net.shibboleth.idp.test.flows.cas;

import net.shibboleth.idp.cas.proxy.impl.HttpClientProxyValidator;
import net.shibboleth.idp.cas.service.Service;
import net.shibboleth.utilities.java.support.resolver.CriteriaSet;
import org.opensaml.security.trust.TrustEngine;
import org.opensaml.security.x509.X509Credential;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import java.net.URI;

/**
 * Test proxy validator component.
 *
 * @author Marvin S. Addison
 */
public class TestProxyValidator extends HttpClientProxyValidator {

    /** Validation repsonse HTTP status code to return. */
    private int responseCode;

    /** Creates a new instance. */
    public TestProxyValidator() {
        super(new TrustEngine<X509Credential>() {
            @Override
            public boolean validate(
                    @Nonnull final X509Credential x509Credential, @Nullable final CriteriaSet criteriaSet) {
                return true;
            }
        });
    }

    public void setResponseCode(final int code) {
        this.responseCode = code;
    }

    @Override
    protected int connect(@Nonnull final URI uri, @Nonnull final Service service) {
        return responseCode;
    }
}
