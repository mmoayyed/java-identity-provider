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
import java.util.Collections;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.joda.time.DateTime;
import org.opensaml.profile.context.ProfileRequestContext;
import org.opensaml.saml.common.messaging.context.SAMLMessageInfoContext;

import com.google.common.base.Function;

import net.shibboleth.idp.profile.AuditExtractorFunction;
import net.shibboleth.utilities.java.support.logic.Constraint;

/** {@link AuditExtractorFunction} that returns the message IssueInstant from a {@link MessageContext}. */
public class SAMLMessageInstantAuditExtractor implements AuditExtractorFunction {

    /** Lookup strategy for SAMLMessageInfoContext to read from. */
    @Nonnull private final Function<ProfileRequestContext,SAMLMessageInfoContext> msgInfoContextLookupStrategy;
    
    /**
     * Constructor.
     *
     * @param strategy lookup strategy for {@link SAMLMessageInfoContext}
     */
    public SAMLMessageInstantAuditExtractor(
            @Nonnull final Function<ProfileRequestContext,SAMLMessageInfoContext> strategy) {
        msgInfoContextLookupStrategy = Constraint.isNotNull(strategy,
                "SAMLMessageInfoContext lookup strategy cannot be null");
    }

    /** {@inheritDoc} */
    @Override
    @Nullable public Collection<String> apply(@Nullable final ProfileRequestContext input) {
        final SAMLMessageInfoContext infoCtx = msgInfoContextLookupStrategy.apply(input);
        if (infoCtx != null) {
            final DateTime ts = infoCtx.getMessageIssueInstant();
            return Collections.singletonList(ts.toString());
        } else {
            return Collections.emptyList();
        }
    }

}