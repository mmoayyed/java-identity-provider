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

package net.shibboleth.idp.ui.context;

import net.shibboleth.idp.ui.saml.ServiceDescriptionStrategy;
import net.shibboleth.idp.ui.saml.ServiceNameStrategy;
import net.shibboleth.utilities.java.support.component.ComponentInitializationException;

import org.opensaml.saml.saml2.metadata.EntityDescriptor;

/**
 * UI context to deal with SAML {@link EntityDescriptor}.
 */
public class SAMLRelyingPartyUIInformation extends RelyingPartyUIInformation<EntityDescriptor> {

    /** {@inheritDoc} 
     * @throws ComponentInitializationException */
    @Override protected void doInitialize() throws ComponentInitializationException {
        super.doInitialize();
        if (null == getServiceDescription().getDataStrategy()) {
            getServiceDescription().setDataStrategy(new ServiceDescriptionStrategy());
        }
        if (null == getServiceName().getDataStrategy()) {
            getServiceName().setDataStrategy(new ServiceNameStrategy());
        }
    }
}
