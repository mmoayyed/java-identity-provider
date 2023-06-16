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

package net.shibboleth.idp.cas.config;

import javax.annotation.Nonnull;

import net.shibboleth.shared.annotation.constraint.NotEmpty;

/**
 * CAS protocol configuration that applies to the <code>/proxy</code> URI.
 *
 * @author Marvin S. Addison
 */
public class ProxyConfiguration extends AbstractProtocolConfiguration {

    /** Proxy ticket profile URI. */
    @Nonnull @NotEmpty public static final String PROFILE_ID = PROTOCOL_URI + "/proxy";

    /** Proxy ticket profile counter name. */
    @Nonnull @NotEmpty public static final String PROFILE_COUNTER = PROTOCOL_COUNTER + ".proxy";

    /** Default ticket prefix. */
    @Nonnull @NotEmpty public static final String DEFAULT_TICKET_PREFIX = "PT";

    /** Default ticket length (random part). */
    public static final int DEFAULT_TICKET_LENGTH = 25;


    /** Creates a new instance. */
    public ProxyConfiguration() {
        super(PROFILE_ID);
    }

    /** {@inheritDoc} */
    @Override
    @Nonnull @NotEmpty protected String getDefaultTicketPrefix() {
        return DEFAULT_TICKET_PREFIX;
    }

    /** {@inheritDoc} */
    @Override
    protected int getDefaultTicketLength() {
        return DEFAULT_TICKET_LENGTH;
    }

}