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

package net.shibboleth.idp.admin;

import javax.annotation.Nonnull;

import net.shibboleth.utilities.java.support.annotation.constraint.NotEmpty;

/**
 * Descriptor for an administrative flow that tracks whether it's been run or not to
 * limit use.
 * 
 * <p>This is primarily for flows that are used to initialize the system in some fashion.</p>
 *
 * @since 3.4.0
 */
public class OneTimeAdministrativeFlowDescriptor extends BasicAdministrativeFlowDescriptor {

    /** Execution flag. */
    private boolean flowExecuted;
    
    /**
     * Constructor.
     *
     * @param id profile identifier
     */
    public OneTimeAdministrativeFlowDescriptor(@Nonnull @NotEmpty final String id) {
        super(id);
    }

    /**
     * Get whether the flow has been executed.
     * 
     * @return execution flag
     */
    public boolean isFlowExecuted() {
        return flowExecuted;
    }

    /**
     * Set whether the flow has been executed.
     * 
     * @param flag flag to set
     */
    public void setFlowExecuted(final boolean flag) {
        flowExecuted = flag;
    }
    
}