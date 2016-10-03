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

package net.shibboleth.idp.saml.attribute.principalconnector.impl;

import java.util.Collection;
import java.util.Collections;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import net.shibboleth.idp.authn.context.SubjectCanonicalizationContext;
import net.shibboleth.idp.saml.nameid.NameDecoderException;
import net.shibboleth.idp.saml.nameid.NameIDDecoder;
import net.shibboleth.idp.saml.nameid.NameIdentifierDecoder;
import net.shibboleth.utilities.java.support.annotation.constraint.NonnullAfterInit;
import net.shibboleth.utilities.java.support.annotation.constraint.NonnullElements;
import net.shibboleth.utilities.java.support.annotation.constraint.NotEmpty;
import net.shibboleth.utilities.java.support.annotation.constraint.NotLive;
import net.shibboleth.utilities.java.support.annotation.constraint.NullableElements;
import net.shibboleth.utilities.java.support.annotation.constraint.Unmodifiable;
import net.shibboleth.utilities.java.support.component.AbstractIdentifiableInitializableComponent;
import net.shibboleth.utilities.java.support.component.ComponentInitializationException;
import net.shibboleth.utilities.java.support.component.ComponentSupport;
import net.shibboleth.utilities.java.support.logic.Constraint;
import net.shibboleth.utilities.java.support.primitive.StringSupport;

import org.opensaml.saml.saml1.core.NameIdentifier;
import org.opensaml.saml.saml2.core.NameID;

import com.google.common.base.Predicates;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;

/**
 * The concrete representation of a &lt;PrincipalConnector&gt;, delegates actual work to decoders.
 */
public class PrincipalConnector extends AbstractIdentifiableInitializableComponent implements NameIdentifierDecoder,
        NameIDDecoder {

    /** The {@link NameID} decoder. */
    @NonnullAfterInit private NameIDDecoder nameIDDecoder;

    /** The {@link NameIdentifier} decoder. */
    @NonnullAfterInit private NameIdentifierDecoder nameIdentifierDecoder;

    /** The format we match against. */
    @NonnullAfterInit @NotEmpty private String format;

    /** The relying parties we support. */
    @Nonnull private Collection<String> relyingParties = Collections.emptySet();

    /** {@inheritDoc} */
    @Override protected void doInitialize() throws ComponentInitializationException {
        if (null == nameIDDecoder) {
            throw new ComponentInitializationException("NameIDDecoder cannot be null");
        }
        if (null == nameIdentifierDecoder) {
            throw new ComponentInitializationException("NameIdentifierDecoder cannot be null");
        }
        if (null == format) {
            throw new ComponentInitializationException("Name identifier format cannot be empty or null");
        }
        super.doInitialize();
    }
    
    /**
     * Set the {@link NameIDDecoder}.
     * 
     * @param saml2Decoder the decoder
     */
    @Nonnull public void setNameIDDecoder(final NameIDDecoder saml2Decoder) {
        ComponentSupport.ifInitializedThrowUnmodifiabledComponentException(this);
        nameIDDecoder = Constraint.isNotNull(saml2Decoder, "NameIDDecoder cannot be null");
    }

    /**
     * Get the {@link NameIDDecoder}.
     * 
     * @return the decoder
     */
    @Nonnull public NameIDDecoder getNameIDDecoder() {
        return nameIDDecoder;
    }

    /**
     * Get the {@link NameIdentifierDecoder}.
     * 
     * @param saml1Decoder the decoder for a {@link NameIdentifier}
     */
    @Nonnull public void setNameIdentifierDecoder(@Nonnull final NameIdentifierDecoder saml1Decoder) {
        ComponentSupport.ifInitializedThrowUnmodifiabledComponentException(this);
        nameIdentifierDecoder = Constraint.isNotNull(saml1Decoder, "NameIdentifierDecoder cannot be null");
    }

    /**
     * Set the {@link NameIdentifierDecoder}.
     * 
     * @return the decoder
     */
    @Nonnull public NameIdentifierDecoder getNameIdentifierDecoder() {
        return nameIdentifierDecoder;
    }

    /**
     * Set the format we support.
     * 
     * @param theFormat the format to match on
     */
    @Nonnull public void setFormat(@Nonnull @NotEmpty final String theFormat) {
        ComponentSupport.ifInitializedThrowUnmodifiabledComponentException(this);
        format = Constraint.isNotNull(StringSupport.trimOrNull(theFormat),
                "Name identifier format cannot be empty or null");
    }

    /**
     * Get the format we support.
     * 
     * @return the format we support
     */
    @Nonnull public String getFormat() {
        return format;
    }

    /**
     * Get the supported relying parties.
     * 
     * @return the supporred relying parties
     */
    @Nonnull @NonnullElements @NotLive @Unmodifiable public Collection<String> getRelyingParties() {
        return relyingParties;
    }

    /**
     * Set the supported relying parties.
     * 
     * @param rps the supported relying parties
     */
    public void setRelyingParties(@Nullable @NullableElements final Collection<String> rps) {
        ComponentSupport.ifInitializedThrowUnmodifiabledComponentException(this);
        if (null != rps) {
            relyingParties = ImmutableSet.copyOf(Iterables.filter(rps, Predicates.notNull()));
        }
    }

    /**
     * Does the supplier requester (relying party) match our configuration?
     * 
     * @param requester the requester
     * @return true iff no relyingParties were configured or the requester matches
     */
    public boolean requesterMatches(@Nullable final String requester) {
        return null == requester || relyingParties.isEmpty() || relyingParties.contains(requester);
    }

    /** {@inheritDoc} */
    @Override
    @Nullable public String decode(@Nonnull final SubjectCanonicalizationContext c14nContext,
            @Nonnull final NameID nameID) throws NameDecoderException {
        return nameIDDecoder.decode(c14nContext, nameID);
    }

    /** {@inheritDoc} */
    @Override
    @Nullable public String decode(@Nonnull final SubjectCanonicalizationContext c14nContext,
            @Nonnull final NameIdentifier nameIdentifier) throws NameDecoderException {
        return nameIdentifierDecoder.decode(c14nContext, nameIdentifier);
    }
    
}