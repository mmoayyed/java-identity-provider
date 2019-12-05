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

package net.shibboleth.idp.attribute.resolver.dc.ldap.impl;

import javax.annotation.Nonnull;

import org.apache.velocity.VelocityContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import edu.internet2.middleware.shibboleth.common.attribute.provider.V2SAMLProfileRequestContext;
import net.shibboleth.idp.attribute.resolver.context.AttributeResolutionContext;
import net.shibboleth.idp.attribute.resolver.dc.ldap.TemplatedExecutableSearchFilterBuilder;

/**
 * A {@link TemplatedExecutableSearchFilterBuilder} which also injects an
 * {@link V2SAMLProfileRequestContext} into the spring context.
 */
public class V2CompatibleTemplatedExecutableSearchFilterBuilder extends TemplatedExecutableSearchFilterBuilder {

    /** Class logger. */
    private final Logger log = LoggerFactory.getLogger(V2CompatibleTemplatedExecutableSearchFilterBuilder.class);


    /** {@inheritDoc} */
    protected void addExtraVelocityContext(@Nonnull final VelocityContext velocityContext,
            @Nonnull final AttributeResolutionContext resolutionContext) {
        if (isV2Compatibility()) {
            final V2SAMLProfileRequestContext requestContext = new V2SAMLProfileRequestContext(resolutionContext, null);
            log.trace("Adding v2 request context {}", requestContext);
            velocityContext.put("requestContext", requestContext);
        }
        super.addExtraVelocityContext(velocityContext, resolutionContext);
    }

}
