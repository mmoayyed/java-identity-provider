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

package net.shibboleth.idp.consent.flow.tou;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import net.shibboleth.idp.consent.flow.ConsentFlowDescriptor;
import net.shibboleth.idp.consent.logic.TermsOfUseHashFunction;
import net.shibboleth.utilities.java.support.annotation.constraint.NonnullAfterInit;
import net.shibboleth.utilities.java.support.annotation.constraint.NotEmpty;
import net.shibboleth.utilities.java.support.component.ComponentInitializationException;
import net.shibboleth.utilities.java.support.component.ComponentSupport;
import net.shibboleth.utilities.java.support.logic.Constraint;
import net.shibboleth.utilities.java.support.primitive.StringSupport;

import com.google.common.base.Function;

/**
 * Descriptor for a terms of use consent flow.
 * 
 * TODO details
 */
public class TermsOfUseFlowDescriptor extends ConsentFlowDescriptor {

    /** Message code used to resolve the terms of use id. */
    @Nullable @NonnullAfterInit @NotEmpty private String idMessageCode;

    /** Message code used to resolve the terms of use text. */
    @Nullable @NonnullAfterInit @NotEmpty private String textMessageCode;

    /** Function to create hash of terms of use text. */
    @Nullable @NonnullAfterInit private Function<TermsOfUse, String> termsOfUseHashFunction;

    /** Constructor. */
    public TermsOfUseFlowDescriptor() {
        setTermsOfUseHashFunction(new TermsOfUseHashFunction());
    }

    /** {@inheritDoc} */
    @Override protected void doInitialize() throws ComponentInitializationException {

        if (idMessageCode == null) {
            throw new ComponentInitializationException(
                    "Message code used to resolve the terms of use id cannot be null");
        }

        if (textMessageCode == null) {
            throw new ComponentInitializationException(
                    "Message code used to resolve the terms of use text cannot be null");
        }

        super.doInitialize();
    }

    /**
     * Get the message code used to resolve the terms of use id.
     * 
     * @return message code used to resolve the terms of use id
     */
    @Nullable @NonnullAfterInit @NotEmpty public String getIdMessageCode() {
        return idMessageCode;
    }

    /**
     * Get the message code used to resolve the terms of use text.
     * 
     * @return message code used to resolve the terms of use text
     */
    @Nullable @NonnullAfterInit @NotEmpty public String getTextMessageCode() {
        return textMessageCode;
    }

    /**
     * Get function to create hash of terms of use text.
     * 
     * @return function to create hash of terms of use text
     */
    @Nullable @NonnullAfterInit public Function<TermsOfUse, String> getTermsOfUseHashFunction() {
        return termsOfUseHashFunction;
    }

    /**
     * Set the message code used to resolve the terms of use id.
     * 
     * @param code message code used to resolve the terms of use id
     */
    public void setIdMessageCode(@Nonnull @NotEmpty final String code) {
        ComponentSupport.ifInitializedThrowUnmodifiabledComponentException(this);

        idMessageCode =
                Constraint.isNotNull(StringSupport.trimOrNull(code),
                        "Message code used to resolve the terms of use id cannot be null");
    }

    /**
     * Set the message code used to resolve the terms of use id.
     * 
     * @param code message code used to resolve the terms of use id
     */
    public void setTextMessageCode(@Nonnull @NotEmpty final String code) {
        ComponentSupport.ifInitializedThrowUnmodifiabledComponentException(this);

        textMessageCode =
                Constraint.isNotNull(StringSupport.trimOrNull(code),
                        "Message code used to resolve the terms of use text cannot be null");
    }

    /**
     * Set function to create hash of terms of use text.
     * 
     * @param function function to create hash of terms of use text
     */
    public void setTermsOfUseHashFunction(@Nonnull final Function<TermsOfUse, String> function) {
        ComponentSupport.ifInitializedThrowUnmodifiabledComponentException(this);

        termsOfUseHashFunction = Constraint.isNotNull(function, "Terms of use hash function cannot be null");
    }
}
