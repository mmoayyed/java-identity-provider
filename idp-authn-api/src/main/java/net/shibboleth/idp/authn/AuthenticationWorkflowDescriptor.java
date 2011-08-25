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

package net.shibboleth.idp.authn;

import org.opensaml.util.Assert;
import org.opensaml.util.StringSupport;

/** A descriptor of an authentication workflow. */
public class AuthenticationWorkflowDescriptor {

    /** The unique identifier of the authentication workflow. */
    private String workflowId;

    /** Whether this workflow supports passive authentication. */
    private boolean supportsPassive;

    /** Whether this workflow supports forced authentication. */
    private boolean supportsForced;

    /**
     * Constructor.
     * 
     * @param id unique ID of this workflow, can not be null or empty
     */
    public AuthenticationWorkflowDescriptor(String id) {
        workflowId = StringSupport.trimOrNull(id);
        Assert.isNotNull(workflowId, "Workflow ID can not be null or empty");
    }

    /**
     * Gets the unique identifier of the authentication workflow.
     * 
     * @return unique identifier of the authentication workflow
     */
    public String getWorkflowId() {
        return workflowId;
    }

    /**
     * Gets whether this workflow supports passive authentication.
     * 
     * @return whether this workflow supports passive authentication
     */
    public boolean isSupportsPassive() {
        return supportsPassive;
    }

    /**
     * Sets whether this workflow supports passive authentication.
     * 
     * @param isSupported whether this workflow supports passive authentication
     */
    public void setSupportsPassive(boolean isSupported) {
        supportsPassive = isSupported;
    }

    /**
     * Gets whether this workflow supports forced authentication.
     * 
     * @return whether this workflow supports forced authentication
     */
    public boolean isSupportsForced() {
        return supportsForced;
    }

    /**
     * Sets whether this workflow supports forced authentication.
     * 
     * @param isSupported whether this workflow supports forced authentication.
     */
    public void setSupportsForced(boolean isSupported) {
        supportsForced = isSupported;
    }

    /** {@inheritDoc} */
    public int hashCode() {
        return workflowId.hashCode();
    }

    /** {@inheritDoc} */
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }

        if (obj == this) {
            return true;
        }

        if (obj instanceof AuthenticationWorkflowDescriptor) {
            return workflowId.equals(((AuthenticationWorkflowDescriptor) obj).getWorkflowId());
        }

        return false;
    }
}