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

package net.shibboleth.idp.saml.impl.attribute.principalconnector;

import java.util.Collection;
import java.util.Collections;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import net.shibboleth.idp.authn.SubjectCanonicalizationException;
import net.shibboleth.idp.saml.nameid.NameDecoderException;
import net.shibboleth.idp.saml.nameid.NameIDDecoder;
import net.shibboleth.idp.saml.nameid.NameIdentifierDecoder;
import net.shibboleth.utilities.java.support.annotation.constraint.NotEmpty;
import net.shibboleth.utilities.java.support.annotation.constraint.NullableElements;
import net.shibboleth.utilities.java.support.component.AbstractIdentifiableInitializableComponent;
import net.shibboleth.utilities.java.support.component.ComponentSupport;
import net.shibboleth.utilities.java.support.logic.Constraint;
import net.shibboleth.utilities.java.support.primitive.StringSupport;

import org.opensaml.saml.saml1.core.NameIdentifier;
import org.opensaml.saml.saml2.core.NameID;
import org.springframework.beans.factory.InitializingBean;

import com.google.common.base.Predicates;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;

/**
 * The concrete representation of a &lt;PrincipalConnector&gt;.
 */
public class PrincipalConnector extends AbstractIdentifiableInitializableComponent implements NameIdentifierDecoder,
        NameIDDecoder, InitializingBean {

    /** The {@link NameID} decoder. */
    @Nonnull private final NameIDDecoder nameIDDecoder;

    /** The {@link NameIdentifier} decoder. */
    @Nonnull private final NameIdentifierDecoder nameIdentifierDecoder;

    /** The format we match against. */
    @Nonnull @NotEmpty private final String format;

    /** The sp we match against. */
    @Nonnull private Collection<String> relyingParties = Collections.EMPTY_SET;

    /**
     * Constructor.
     * 
     * @param decoderNameID the decoder for {@link NameID}
     * @param decodernameIdentifier for {@link NameIdentifier}
     * @param theFormat the format to match on.
     */
    public PrincipalConnector(@Nonnull NameIDDecoder decoderNameID,
            @Nonnull NameIdentifierDecoder decodernameIdentifier, @Nonnull String theFormat) {
        nameIDDecoder = Constraint.isNotNull(decoderNameID, "provided NameIDDecoder must not be null");
        nameIdentifierDecoder =
                Constraint.isNotNull(decodernameIdentifier, "provided NameIdentifierDecoder must not be null");
        format = Constraint.isNotNull(StringSupport.trimOrNull(theFormat), "provided format must not be empty or null");
    }

    /**
     * Get the {@link NameID} decoder.
     * 
     * @return the decoder
     */
    @Nonnull public NameIDDecoder getNameIDDecoder() {
        return nameIDDecoder;
    }

    /**
     * Get the {@link NameIdentifierDecoder} decoder.
     * 
     * @return the decoder
     */
    @Nonnull public NameIdentifierDecoder getNameIdentifierDecoder() {
        return nameIdentifierDecoder;
    }

    /** {@inheritDoc} */
    @Override public synchronized void setId(@Nonnull String componentId) {
        super.setId(componentId);
    }

    /**
     * Get the format we support.
     * 
     * @return the format we support.
     */
    @Nonnull public String getFormat() {
        return format;
    }

    /**
     * The supported relying Parties.
     * 
     * @return the RPs
     */
    @Nonnull public Collection<String> getRelyingParties() {
        return relyingParties;
    }

    /**
     * Sets the relying parties we are interested in.
     * 
     * @param rps the relying parties.
     */
    public void setRelyingParties(@Nullable @NullableElements Collection<String> rps) {
        ComponentSupport.ifInitializedThrowUnmodifiabledComponentException(this);
        if (null != rps) {
            relyingParties = ImmutableSet.copyOf(Iterables.filter(rps, Predicates.notNull()));
        }
    }

    /**
     * Does the supplier requester (relying party match out configuration).
     * 
     * @param requester the requester.
     * @return true if no relyingParties were configured or if the requester matches.
     */
    public boolean requesterMatches(@Nullable String requester) {

        return null == requester || relyingParties.isEmpty() || relyingParties.contains(requester);
    }

    /** {@inheritDoc} */
    @Override @Nonnull public String decode(@Nonnull NameID nameID, @Nullable String responderId,
            @Nullable String requesterId) throws SubjectCanonicalizationException, NameDecoderException {
        return nameIDDecoder.decode(nameID, responderId, requesterId);
    }

    /** {@inheritDoc} */
    @Override @Nonnull public String decode(@Nonnull NameIdentifier nameIdentifier, @Nullable String responderId,
            @Nullable String requesterId) throws SubjectCanonicalizationException, NameDecoderException {
        return nameIdentifierDecoder.decode(nameIdentifier, responderId, requesterId);
    }

    /** {@inheritDoc} */
    @Override public void afterPropertiesSet() throws Exception {
        initialize();
    }
}
