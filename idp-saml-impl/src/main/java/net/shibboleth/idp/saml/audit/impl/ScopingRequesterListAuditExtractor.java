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

package net.shibboleth.idp.saml.audit.impl;

import java.util.Collection;
import java.util.function.Function;
import java.util.stream.Collectors;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.opensaml.profile.context.ProfileRequestContext;
import org.opensaml.saml.saml2.core.AuthnRequest;
import org.opensaml.saml.saml2.core.RequesterID;
import org.opensaml.saml.saml2.core.Scoping;

/**
 * {@link Function} that returns {@link Scoping#getRequesterIDs()} from an {@link AuthnRequest}.
 * 
 * @since 4.2.0
 */
public class ScopingRequesterListAuditExtractor extends AbstractScopingAuditExtractor<Collection<String>> {
    
    /**
     * Constructor.
     *
     * @param strategy lookup strategy for message
     */
    public ScopingRequesterListAuditExtractor(@Nonnull final Function<ProfileRequestContext,AuthnRequest> strategy) {
        super(strategy);
    }

    /**
     * Override point to do the extraction.
     * 
     * @param scoping the input object
     * 
     * @return the extracted value
     */
    @Nullable protected Collection<String> doApply(@Nullable final Scoping scoping) {
        if (scoping != null) {
            return scoping.getRequesterIDs().stream()
                    .map(RequesterID::getURI)
                    .filter(s -> s != null)
                    .collect(Collectors.toUnmodifiableList());
        }
        
        return null;
    }
    
}