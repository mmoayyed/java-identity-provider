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

package net.shibboleth.idp.consent.logic;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import net.shibboleth.idp.consent.flow.ar.AttributeConsentFlowDescriptor;
import net.shibboleth.idp.profile.context.ProfileInterceptorContext;
import net.shibboleth.idp.profile.interceptor.ProfileInterceptorFlowDescriptor;

import org.opensaml.messaging.context.navigate.ChildContextLookup;
import org.opensaml.profile.context.ProfileRequestContext;

import com.google.common.base.Function;

/**
 * Function to return an attribute consent flow descriptor from a profile request context.
 */
public class AttributeConsentFlowDescriptorLookupStrategy implements
        Function<ProfileRequestContext, AttributeConsentFlowDescriptor> {

    /** Strategy used to find the {@link ProfileInterceptorContext} from the {@link ProfileRequestContext}. */
    @Nonnull private Function<ProfileRequestContext, ProfileInterceptorContext> interceptorContextlookupStrategy;

    /** Constructor. */
    public AttributeConsentFlowDescriptorLookupStrategy() {
        interceptorContextlookupStrategy = new ChildContextLookup<>(ProfileInterceptorContext.class);
    }

    /** {@inheritDoc} */
    @Nullable public AttributeConsentFlowDescriptor apply(@Nullable final ProfileRequestContext input) {
        if (input == null) {
            return null;
        }

        final ProfileInterceptorContext interceptorContext = interceptorContextlookupStrategy.apply(input);
        if (interceptorContext == null) {
            return null;
        }

        final ProfileInterceptorFlowDescriptor interceptorFlowDescriptor = interceptorContext.getAttemptedFlow();
        if (interceptorFlowDescriptor == null) {
            return null;
        }

        if (!(interceptorFlowDescriptor instanceof AttributeConsentFlowDescriptor)) {
            return null;
        }

        return (AttributeConsentFlowDescriptor) interceptorFlowDescriptor;
    }

}
