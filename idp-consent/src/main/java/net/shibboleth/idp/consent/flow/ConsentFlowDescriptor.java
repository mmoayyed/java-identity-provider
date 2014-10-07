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

package net.shibboleth.idp.consent.flow;

import java.util.Map;

import javax.annotation.Nonnull;

import net.shibboleth.idp.consent.Consent;
import net.shibboleth.idp.consent.logic.FlowIdLookupFunction;
import net.shibboleth.idp.consent.storage.ConsentSerializer;
import net.shibboleth.idp.profile.interceptor.ProfileInterceptorFlowDescriptor;
import net.shibboleth.utilities.java.support.component.ComponentSupport;
import net.shibboleth.utilities.java.support.logic.Constraint;

import org.opensaml.storage.StorageSerializer;

/**
 * Abstract descriptor for a consent flow.
 * 
 * Holds a {@link StorageSerializer} for {@link Consent}. Defaults the storage context lookup strategy to a
 * {@link FlowIdLookupFunction}.
 * 
 * TODO details
 */
public abstract class ConsentFlowDescriptor extends ProfileInterceptorFlowDescriptor {

    /** Whether consent equality includes comparing consent values. */
    private boolean compareValues;

    /** Consent serializer. */
    @Nonnull private StorageSerializer<Map<String, Consent>> consentSerializer;

    /** Constructor. */
    public ConsentFlowDescriptor() {
        setStorageContextLookupStrategy(new FlowIdLookupFunction());
        setConsentSerializer(new ConsentSerializer());
    }

    /**
     * Whether consent equality includes comparing consent values.
     * 
     * @return true if consent equality includes comparing consent values
     */
    public boolean compareValues() {
        return compareValues;
    }

    /**
     * Get the consent serializer.
     * 
     * @return consent serializer
     */
    @Nonnull public StorageSerializer<Map<String, Consent>> getConsentSerializer() {
        return consentSerializer;
    }

    /**
     * Set whether consent equality includes comparing consent values.
     * 
     * @param flag true if consent equality includes comparing consent values
     */
    public void setCompareValues(final boolean flag) {
        ComponentSupport.ifInitializedThrowUnmodifiabledComponentException(this);

        compareValues = flag;
    }

    /**
     * Set the consent serializer.
     * 
     * @param serializer consent serializer
     */
    public void setConsentSerializer(@Nonnull final StorageSerializer<Map<String, Consent>> serializer) {
        consentSerializer = Constraint.isNotNull(serializer, "Consent serializer cannot be null");
    }
}
