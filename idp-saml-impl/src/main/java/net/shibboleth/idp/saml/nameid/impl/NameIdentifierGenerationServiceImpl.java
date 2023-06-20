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

package net.shibboleth.idp.saml.nameid.impl;

import javax.annotation.Nonnull;

import org.opensaml.saml.saml1.profile.SAML1NameIdentifierGenerator;
import org.opensaml.saml.saml2.profile.SAML2NameIDGenerator;

import net.shibboleth.idp.saml.nameid.NameIdentifierGenerationService;
import net.shibboleth.shared.annotation.constraint.NonnullAfterInit;
import net.shibboleth.shared.component.AbstractIdentifiableInitializableComponent;
import net.shibboleth.shared.component.ComponentInitializationException;
import net.shibboleth.shared.logic.Constraint;

/** Implementation of {@link NameIdentifierGenerationService}. */
public class NameIdentifierGenerationServiceImpl extends AbstractIdentifiableInitializableComponent
        implements NameIdentifierGenerationService {

    /** SAML 1 generator. */
    @NonnullAfterInit private SAML1NameIdentifierGenerator saml1Generator;

    /** SAML 2 generator. */
    @NonnullAfterInit private SAML2NameIDGenerator saml2Generator;
    
    /**
     * Set the {@link SAML1NameIdentifierGenerator} to use.
     * 
     * @param generator generator to use
     */
    public void setSAML1NameIdentifierGenerator(@Nonnull final SAML1NameIdentifierGenerator generator) {
        saml1Generator = Constraint.isNotNull(generator, "SAML1NameIdentifierGenerator cannot be null");
    }

    /**
     * Set the {@link SAML2NameIDGenerator} to use.
     * 
     * @param generator generator to use
     */
    public void setSAML2NameIDGenerator(@Nonnull final SAML2NameIDGenerator generator) {
        saml2Generator = Constraint.isNotNull(generator, "SAML2NameIDGenerator cannot be null");
    }
    
    /** {@inheritDoc} */
    @Override
    protected void doInitialize() throws ComponentInitializationException {
        super.doInitialize();
        
        if (saml1Generator == null || saml2Generator == null) {
            throw new ComponentInitializationException("Generators cannot be null");
        }
    }

    /** {@inheritDoc} */
   @Nonnull public SAML1NameIdentifierGenerator getSAML1NameIdentifierGenerator() {
        checkComponentActive();
        assert saml1Generator!=null;
        return saml1Generator;
    }

    /** {@inheritDoc} */
    @Nonnull public SAML2NameIDGenerator getSAML2NameIDGenerator() {
        checkComponentActive();
        assert saml2Generator!=null;
        return saml2Generator;
    }
}