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

package net.shibboleth.idp.profile.interceptor;

import java.util.LinkedHashMap;
import java.util.Map;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import net.shibboleth.utilities.java.support.annotation.constraint.Live;
import net.shibboleth.utilities.java.support.annotation.constraint.NonnullElements;

import org.opensaml.messaging.context.BaseContext;

/**
 * A {@link BaseContext} which holds flows that are available to be executed, flows which were executed but did not
 * complete, and the last flow attempted.
 */
public class ProfileInterceptorContext extends BaseContext {

    /** The last flow attempted. */
    @Nullable private ProfileInterceptorFlowDescriptor attemptedFlow;

    /** Flows that have been executed, successfully or otherwise, without producing a completed result. */
    @Nonnull @NonnullElements private final Map<String, ProfileInterceptorFlowDescriptor> incompleteFlows;

    /** Flows that are available to be executed. */
    @Nonnull @NonnullElements private final Map<String, ProfileInterceptorFlowDescriptor> availableFlows;

    /** Constructor. */
    public ProfileInterceptorContext() {
        incompleteFlows = new LinkedHashMap<>();
        availableFlows = new LinkedHashMap<>();
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
    @Nonnull public void setAttemptedFlow(@Nullable final ProfileInterceptorFlowDescriptor flow) {
        attemptedFlow = flow;
    }

    /**
     * Get the flows that are available to be executed.
     * 
     * @return the available flows
     */
    @Nonnull @NonnullElements @Live public Map<String, ProfileInterceptorFlowDescriptor> getAvailableFlows() {
        return availableFlows;
    }

    /**
     * Get the flows that have been executed, successfully or otherwise, without producing a completed result.
     * 
     * @return the incompletely executed flows
     */
    @Nonnull @NonnullElements @Live public Map<String, ProfileInterceptorFlowDescriptor> getIncompleteFlows() {
        return incompleteFlows;
    }
}
