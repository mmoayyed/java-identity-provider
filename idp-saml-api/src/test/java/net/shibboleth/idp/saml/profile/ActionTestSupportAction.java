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

package net.shibboleth.idp.saml.profile;

import net.shibboleth.idp.profile.AbstractProfileAction;
import net.shibboleth.utilities.java.support.component.ComponentInitializationException;

import org.opensaml.profile.ProfileException;
import org.opensaml.profile.context.ProfileRequestContext;
import org.opensaml.saml.saml1.core.Response;
import org.springframework.webflow.execution.Event;
import org.springframework.webflow.execution.RequestContext;

public class ActionTestSupportAction extends AbstractProfileAction<Object, Response> {

    public ActionTestSupportAction() throws ComponentInitializationException {
        setId("Test Component");
        initialize();
    }

    /** {@inheritDoc} */
    protected Event doExecute(RequestContext springRequestContext,
            ProfileRequestContext<Object, Response> profileRequestContext) throws ProfileException {
        return null;
    }
}