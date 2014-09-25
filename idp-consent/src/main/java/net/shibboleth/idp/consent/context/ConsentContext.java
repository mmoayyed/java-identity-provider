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

package net.shibboleth.idp.consent.context;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import net.shibboleth.idp.consent.flow.ConsentFlowDescriptor;
import net.shibboleth.utilities.java.support.logic.Constraint;

import org.opensaml.messaging.context.BaseContext;

import com.google.common.base.MoreObjects;

/**
 * Context representing the state of a consent flow.
 */
// TODO Just a stub.
public class ConsentContext extends BaseContext {

    /** Consent flow descriptor. */
    @Nullable private ConsentFlowDescriptor consentFlowDescriptor;

    /**
     * Get the consent flow descriptor.
     * 
     * @return the consent flow descriptor
     */
    public ConsentFlowDescriptor getConsentFlowDescriptor() {
        return consentFlowDescriptor;
    }

    /**
     * Set the consent flow descriptor.
     * 
     * @param descriptor consent flow descriptor
     */
    public void setConsentFlowDescriptor(@Nonnull final ConsentFlowDescriptor descriptor) {
        consentFlowDescriptor = Constraint.isNotNull(descriptor, "Consent flow descriptor cannot be null");
    }

    /** {@inheritDoc} */
    @Override public String toString() {
        return MoreObjects.toStringHelper(this).add("consentFlowDescriptor", consentFlowDescriptor).toString();
    }

}
