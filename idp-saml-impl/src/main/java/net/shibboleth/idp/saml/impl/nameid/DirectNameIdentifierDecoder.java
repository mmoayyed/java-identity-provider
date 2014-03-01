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

package net.shibboleth.idp.saml.impl.nameid;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import net.shibboleth.idp.saml.nameid.NameDecoderException;
import net.shibboleth.idp.saml.nameid.NameIdentifierDecoder;
import net.shibboleth.utilities.java.support.annotation.constraint.NotEmpty;
import net.shibboleth.utilities.java.support.component.AbstractIdentifiableInitializableComponent;
import net.shibboleth.utilities.java.support.component.ComponentInitializationException;
import net.shibboleth.utilities.java.support.component.ComponentSupport;

import org.opensaml.saml.saml1.core.NameIdentifier;
import org.slf4j.LoggerFactory;

/**
 * Class to implement the direct transform from a {@link NameIdentifier}. The decode operation returns the input.
 */
public class DirectNameIdentifierDecoder extends AbstractIdentifiableInitializableComponent implements
        NameIdentifierDecoder {

    /**
     * {@inheritDoc}
     * 
     * The decoded value is just the input. We do not police any values.
     */
    @Override @Nonnull @NotEmpty public String decode(@Nonnull final NameIdentifier nameIdentifier,
            @Nullable final String responderId, @Nullable final String requesterId) throws NameDecoderException {
        ComponentSupport.ifNotInitializedThrowUninitializedComponentException(this);

        if (nameIdentifier.getNameIdentifier() == null) {
            throw new NameDecoderException("NameIdentifier value was null");
        }
        return nameIdentifier.getNameIdentifier();
    }

    /** {@inheritDoc} */
    @Override protected void doInitialize() throws ComponentInitializationException {
        super.doInitialize();
        LoggerFactory.getLogger(DirectNameIdentifierDecoder.class).debug("Direct Transform {}", getId());
    }
    
}