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

package net.shibboleth.idp.saml.session;

import java.time.Instant;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.opensaml.saml.common.xml.SAMLConstants;

import net.shibboleth.idp.session.BasicSPSession;
import net.shibboleth.shared.annotation.constraint.NotEmpty;

/**
 * Marker subtype for a SAML 1 session, adds no actual information other than its identity as
 * a SAML 1 session. 
 */
public class SAML1SPSession extends BasicSPSession {
    
    /**
     * Constructor.
     *
     * @param id the identifier of the service associated with this session
     * @param creation creation time of session
     * @param expiration expiration time of session
     */
    public SAML1SPSession(@Nonnull @NotEmpty final String id, @Nonnull final Instant creation,
            @Nonnull final Instant expiration) {
        super(id, creation, expiration);
    }

    /** {@inheritDoc} */
    @Override
    @Nullable @NotEmpty public String getProtocol() {
        return SAMLConstants.SAML11P_NS;
    }

}