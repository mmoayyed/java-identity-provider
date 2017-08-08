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
import java.util.Map;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.apache.http.client.HttpClient;
import org.opensaml.security.httpclient.HttpClientSecurityParameters;

import net.shibboleth.idp.attribute.IdPAttribute;
import net.shibboleth.idp.attribute.resolver.dc.impl.ExecutableSearch;

/** An HTTP request that returns attribute data. */
public interface HTTPSearch extends ExecutableSearch {

    /**
     * The abstraction that will contact the service and obtain results.
     * 
     * @param client the HTTP client
     * @param securityParameters client security settings
     * @param mappingStrategy response mapping strategy
     * 
     * @return attribute results
     * @throws IOException if an error occurs
     */
    @Nonnull Map<String,IdPAttribute> execute(@Nonnull final HttpClient client,
            @Nullable final HttpClientSecurityParameters securityParameters,
            @Nonnull final HTTPResponseMappingStrategy mappingStrategy) throws IOException;
    
}