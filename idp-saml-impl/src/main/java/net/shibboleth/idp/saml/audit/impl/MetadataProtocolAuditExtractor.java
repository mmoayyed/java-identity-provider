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

import java.util.function.Function;

import javax.annotation.Nullable;

import org.opensaml.profile.context.ProfileRequestContext;
import org.springframework.webflow.execution.RequestContext;

import net.shibboleth.idp.profile.context.SpringRequestContext;

/** {@link Function} that returns the Metadata protocol (as defined by the bean
 * called <code>shibboleth.MetadataLookup.Protocol</code>). */
public class MetadataProtocolAuditExtractor implements Function<ProfileRequestContext,Object> {

    /** {@inheritDoc} */
    @Nullable public Object apply(@Nullable final ProfileRequestContext input) {
        final SpringRequestContext springCtx = input.getSubcontext(SpringRequestContext.class);
        if (springCtx == null) {
            return null;
        }
        final RequestContext reqCtx = springCtx.getRequestContext();
        if (reqCtx == null) {
            return null;
        }
        return reqCtx.getActiveFlow().getApplicationContext().getBean("shibboleth.MetadataLookup.Protocol");
    }
}