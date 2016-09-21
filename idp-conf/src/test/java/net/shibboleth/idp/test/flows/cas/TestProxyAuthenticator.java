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

import net.shibboleth.idp.cas.proxy.ProxyAuthenticator;
import org.opensaml.security.trust.TrustEngine;
import org.opensaml.security.x509.X509Credential;

import javax.annotation.Nonnull;
import java.net.URI;
import java.security.GeneralSecurityException;

/**
 * @author Marvin S. Addison
 */
public class TestProxyAuthenticator implements ProxyAuthenticator<TrustEngine<X509Credential>> {

    /** Whether to fail or not. */
    private boolean failureFlag;

    public void setFailureFlag(final boolean isFail) {
        this.failureFlag = isFail;
    }

    @Override
    public void authenticate(@Nonnull URI uri, TrustEngine<X509Credential> criteria) throws GeneralSecurityException {
        if (failureFlag) {
            throw new GeneralSecurityException("Proxy callback authentication failed (failureFlag==true)");
        }
    }
}
