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

package net.shibboleth.idp.profile.spring.relyingparty.impl;

import net.shibboleth.ext.spring.util.BaseSpringNamespaceHandler;
import net.shibboleth.idp.profile.spring.relyingparty.metadata.AbstractMetadataProviderParser;

/** Namespace handler for the relying party files. Only the elements need to be handled.
    All the types belong in the SAML namespace. */
public class RelyingPartyNamespaceHandler extends BaseSpringNamespaceHandler {

    /** {@inheritDoc} */
    @Override public void init() {
        // Relying party Configuration
        // Firstly the types that go inside the Profile
        registerBeanDefinitionParser(RelyingPartyParser.ELEMENT_NAME, new RelyingPartyParser());
        registerBeanDefinitionParser(DefaultRelyingPartyParser.ELEMENT_NAME, new DefaultRelyingPartyParser());
        registerBeanDefinitionParser(AnonymousRelyingPartyParser.ELEMENT_NAME, new AnonymousRelyingPartyParser());

        registerBeanDefinitionParser(AbstractMetadataProviderParser.RELYING_PARTY_GROUP_ELEMENT_NAME,
                new RelyingPartyGroupParser());
    }
}