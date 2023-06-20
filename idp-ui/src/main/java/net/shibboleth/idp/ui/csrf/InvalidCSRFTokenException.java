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

package net.shibboleth.idp.ui.csrf;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.springframework.webflow.execution.FlowExecutionException;

/** Exception indicating a problem validating a CSRF token at runtime. */
public class InvalidCSRFTokenException extends FlowExecutionException{
    
    /** Serial version UID. */
    private static final long serialVersionUID = 9166119183460973854L;

    /**
     * Creates a new invalid CSRF token execution exception.
     * 
     * @param flowId the flow where the exception occurred
     * @param stateId the state where the exception occurred
     * @param message a descriptive message
     */
    public InvalidCSRFTokenException(@Nonnull final String flowId, @Nullable final String stateId,
            @Nonnull final String message) {
        super(flowId, stateId, message);
        
    }
    
}