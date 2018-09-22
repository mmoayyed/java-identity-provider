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

package net.shibboleth.idp.saml.profile.config;

import javax.annotation.Nonnull;

import org.opensaml.messaging.context.MessageContext;

import com.google.common.base.Predicate;

/**
 * Common interface for SAML profile configurations involving artifact consumption, for example artifact
 * resolution requests. 
 */
public interface SAMLArtifactConsumerProfileConfiguration extends SAMLProfileConfiguration {
    
    /**
     * Get the predicate used to determine if artifact resolution requests should be signed.
     * 
     * @return predicate used to determine if artifact resolution requests should be signed
     */
    @Nonnull Predicate<MessageContext> getSignArtifactRequests(); 

    /**
     * Get the predicate used to determine if artifact resolution requests should use client TLS.
     * 
     * @return predicate used to determine if artifact resolution requests should use client TLS
     */
    @Nonnull Predicate<MessageContext> getClientTLSArtifactRequests(); 

    
}