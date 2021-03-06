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
import org.apache.http.impl.client.HttpClients;
import org.opensaml.security.httpclient.HttpClientSecurityParameters;

import javax.annotation.Nonnull;

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
        super(HttpClients.createDefault(), new HttpClientSecurityParameters());
    }

    public void setResponseCode(final int code) {
        this.responseCode = code;
    }

    @Override
    protected int connect(@Nonnull final URI uri, @Nonnull final Service service) {
        return responseCode;
    }
}
