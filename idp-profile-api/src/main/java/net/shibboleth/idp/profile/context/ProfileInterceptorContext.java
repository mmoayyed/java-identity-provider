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

package net.shibboleth.idp.profile.context;

import java.util.List;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import net.shibboleth.idp.profile.interceptor.ProfileInterceptorFlowDescriptor;
import net.shibboleth.idp.profile.interceptor.ProfileInterceptorResult;
import net.shibboleth.utilities.java.support.annotation.constraint.Live;
import net.shibboleth.utilities.java.support.annotation.constraint.NonnullElements;
import net.shibboleth.utilities.java.support.logic.Constraint;

import org.opensaml.messaging.context.BaseContext;

import com.google.common.collect.Lists;

/**
 * A {@link BaseContext} which holds flows that are available to be executed, the last flow attempted,
 * and any flow result.
 */
public class ProfileInterceptorContext extends BaseContext {

    /** The last flow attempted. */
    @Nullable private ProfileInterceptorFlowDescriptor attemptedFlow;

    /** Flows that need to be executed. */
    @Nonnull @NonnullElements private final List<ProfileInterceptorFlowDescriptor> availableFlows;

    /** The result of the flow. */
    @Nullable private ProfileInterceptorResult result;

    /** Constructor. */
    public ProfileInterceptorContext() {
        availableFlows = Lists.newArrayList();
    }

    /**
     * Get the last flow that was attempted.
     * 
     * @return last flow that was attempted
     */
    @Nullable public ProfileInterceptorFlowDescriptor getAttemptedFlow() {
        return attemptedFlow;
    }

    /**
     * Set the last flow that was attempted.
     * 
     * @param flow last flow that was attempted
     */
    public void setAttemptedFlow(@Nullable final ProfileInterceptorFlowDescriptor flow) {
        attemptedFlow = flow;
    }

    /**
     * Get the flows that are available to be executed.
     * 
     * @return the available flows
     */
    @Nonnull @NonnullElements @Live public List<ProfileInterceptorFlowDescriptor> getAvailableFlows() {
        return availableFlows;
    }

    /**
     * Get the result.
     * 
     * @return the result
     */
    @Nullable public ProfileInterceptorResult getResult() {
        return result;
    }

    /**
     * Set the result.
     * 
     * @param interceptorResult the result
     */
    public void setResult(@Nonnull final ProfileInterceptorResult interceptorResult) {
        result = Constraint.isNotNull(interceptorResult, "Interceptor result cannot be null");
    }
    
}