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

/**
 * Credential extractors are responsible for extracting an {@link PrincipalCredential} from a given HTTP request. Some
 * examples of this might be pulling a username/password from submitted form fields or extracting a client certificate
 * from header of a SOAP message.
 * 
 * Note, an extractor is <strong>not</strong> responsible for prompting for credentials and thus does not have access to
 * the HTTP response.
 */
public interface PrincipalCredentalExtractor {

    /**
     * Extracts a principal's credential from the current HTTP request.
     * 
     * @param requestContext current authentication request context, never null
     * 
     * @return the extract credential
     * 
     * @throws Exception thrown if credential data was present but it was mangeled or unusable. This exception must not
     *             be thrown simply because of an absence of a credential.
     */
    public PrincipalCredential extractCredential(final AuthenticationRequestContext requestContext) throws Exception;
}