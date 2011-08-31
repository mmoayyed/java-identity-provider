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

package net.shibboleth.idp.relyingparty;

import org.opensaml.messaging.context.AbstractSubcontext;
import org.opensaml.messaging.context.SubcontextContainer;
import org.opensaml.util.Assert;

/**
 * {@link org.opensaml.messaging.context.Subcontext} containing relying party specific profile configuration. This is
 * usually a subcontext of a {@link net.shibboleth.idp.profile.ProfileRequestContext}.
 */
public final class RelyingPartyConfigurationSubcontext extends AbstractSubcontext {

    /** The relying party configuration. */
    private final RelyingPartyConfiguration config;

    /**
     * Constructor.
     * 
     * @param owner owner of this context, may be null
     * @param relyingPartyConfiguration the relying party configuration, never null
     */
    public RelyingPartyConfigurationSubcontext(SubcontextContainer owner,
            RelyingPartyConfiguration relyingPartyConfiguration) {
        super(owner);

        Assert.isNull(relyingPartyConfiguration, "Relying party configuration can not be null");
        config = relyingPartyConfiguration;
    }

    /**
     * Gets the relying party configuration.
     * 
     * @return the relying party configuration, never null
     */
    public RelyingPartyConfiguration getConfiguration() {
        return config;
    }
}